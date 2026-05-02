/**
 * Unit-tests for use-production-task-history (Feature 005 US5, T076).
 *
 * Verifies formatProductionTaskHistoryEvent maps API events to localized
 * timeline entries with actor, timestamp, status arrow, executor change,
 * planning before/after, reason and note details.
 */
import type { ProductionTaskHistoryEventResponse } from '@/api/types/production-tasks'
import { describe, expect, it } from 'vitest'
import {
  formatProductionTaskHistoryEvent,
  mapProductionTaskHistory,
} from '@/api/composables/use-production-task-history'

function ev(overrides: Partial<ProductionTaskHistoryEventResponse> = {}): ProductionTaskHistoryEventResponse {
  return {
    type: 'CREATED',
    actorDisplayName: 'Менеджер Анна',
    eventAt: '2026-04-27T11:00:00Z',
    fromStatus: null,
    toStatus: 'NOT_STARTED',
    ...overrides,
  }
}

describe('formatProductionTaskHistoryEvent', () => {
  it('translates known event types to russian titles', () => {
    expect(formatProductionTaskHistoryEvent(ev({ type: 'CREATED' })).title).toBe('Задача создана')
    expect(formatProductionTaskHistoryEvent(ev({ type: 'ASSIGNED' })).title).toBe('Назначен исполнитель')
    expect(formatProductionTaskHistoryEvent(ev({ type: 'PLANNING_UPDATED' })).title).toBe('План скорректирован')
    expect(formatProductionTaskHistoryEvent(ev({ type: 'STATUS_CHANGED' })).title).toBe('Изменён статус')
    expect(formatProductionTaskHistoryEvent(ev({ type: 'BLOCKED' })).title).toBe('Заблокировано')
    expect(formatProductionTaskHistoryEvent(ev({ type: 'UNBLOCKED' })).title).toBe('Разблокировано')
    expect(formatProductionTaskHistoryEvent(ev({ type: 'COMPLETED' })).title).toBe('Завершено')
  })

  it('passes unknown event type through unchanged', () => {
    const entry = formatProductionTaskHistoryEvent(ev({ type: 'CUSTOM_EVENT' }))
    expect(entry.title).toBe('CUSTOM_EVENT')
  })

  it('exposes actor and a localized timestamp label', () => {
    const entry = formatProductionTaskHistoryEvent(ev({
      actorDisplayName: 'Иван',
      eventAt: '2026-04-27T11:00:00Z',
    }))
    expect(entry.actorDisplayName).toBe('Иван')
    expect(entry.eventAt).toBe('2026-04-27T11:00:00Z')
    expect(entry.eventAtLabel).toMatch(/2026/)
  })

  it('renders STATUS_CHANGED with from → to arrow', () => {
    const entry = formatProductionTaskHistoryEvent(ev({
      type: 'STATUS_CHANGED',
      fromStatus: 'NOT_STARTED',
      toStatus: 'IN_PROGRESS',
    }))
    expect(entry.details).toContain('Статус: не начато → в работе')
  })

  it('renders STATUS_CHANGED with only target status when fromStatus is null', () => {
    const entry = formatProductionTaskHistoryEvent(ev({
      type: 'STATUS_CHANGED',
      fromStatus: null,
      toStatus: 'IN_PROGRESS',
    }))
    expect(entry.details).toContain('Статус: в работе')
  })

  it('omits status line for BLOCKED/UNBLOCKED/COMPLETED events to avoid duplicate signal', () => {
    const blocked = formatProductionTaskHistoryEvent(ev({
      type: 'BLOCKED',
      fromStatus: 'IN_PROGRESS',
      toStatus: 'BLOCKED',
      reason: 'Нет материала',
    }))
    expect(blocked.details).not.toContain('Статус: в работе → заблокировано')
    expect(blocked.details).toContain('Причина: Нет материала')
  })

  it('renders ASSIGNED with previous → new executor', () => {
    const entry = formatProductionTaskHistoryEvent(ev({
      type: 'ASSIGNED',
      previousExecutorDisplayName: 'Иван',
      newExecutorDisplayName: 'Сергей',
    }))
    expect(entry.details).toContain('Исполнитель: Иван → Сергей')
  })

  it('renders ASSIGNED with only new executor on initial assignment', () => {
    const entry = formatProductionTaskHistoryEvent(ev({
      type: 'ASSIGNED',
      previousExecutorDisplayName: null,
      newExecutorDisplayName: 'Сергей',
    }))
    expect(entry.details).toContain('Исполнитель: Сергей')
  })

  it('renders PLANNING_UPDATED with start and finish before/after', () => {
    const entry = formatProductionTaskHistoryEvent(ev({
      type: 'PLANNING_UPDATED',
      plannedStartDateBefore: '2026-05-01',
      plannedStartDateAfter: '2026-05-02',
      plannedFinishDateBefore: '2026-05-03',
      plannedFinishDateAfter: '2026-05-05',
    }))
    expect(entry.details).toContain('План: начало: 1 мая 2026 → 2 мая 2026')
    expect(entry.details).toContain('План: окончание: 3 мая 2026 → 5 мая 2026')
  })

  it('omits planning line when before/after are equal', () => {
    const entry = formatProductionTaskHistoryEvent(ev({
      type: 'PLANNING_UPDATED',
      plannedStartDateBefore: '2026-05-01',
      plannedStartDateAfter: '2026-05-01',
      plannedFinishDateBefore: null,
      plannedFinishDateAfter: '2026-05-05',
    }))
    expect(entry.details.some(l => l.startsWith('План: начало'))).toBe(false)
    expect(entry.details).toContain('План: окончание: 5 мая 2026')
  })

  it('renders BLOCKED reason and note', () => {
    const entry = formatProductionTaskHistoryEvent(ev({
      type: 'BLOCKED',
      reason: 'Нет материала',
      note: 'Ожидаем поставку',
    }))
    expect(entry.details).toContain('Причина: Нет материала')
    expect(entry.details).toContain('Комментарий: Ожидаем поставку')
  })
})

describe('mapProductionTaskHistory', () => {
  it('preserves API order (chronological asc)', () => {
    const events: ProductionTaskHistoryEventResponse[] = [
      ev({ type: 'CREATED', eventAt: '2026-04-27T10:00:00Z' }),
      ev({ type: 'ASSIGNED', eventAt: '2026-04-27T11:00:00Z', newExecutorDisplayName: 'И' }),
      ev({ type: 'STATUS_CHANGED', eventAt: '2026-04-27T12:00:00Z', toStatus: 'IN_PROGRESS' }),
    ]
    const mapped = mapProductionTaskHistory(events)
    expect(mapped.map(e => e.type)).toEqual(['CREATED', 'ASSIGNED', 'STATUS_CHANGED'])
    expect(mapped.map(e => e.eventAt)).toEqual([
      '2026-04-27T10:00:00Z',
      '2026-04-27T11:00:00Z',
      '2026-04-27T12:00:00Z',
    ])
  })

  it('returns empty array for null/undefined input', () => {
    expect(mapProductionTaskHistory(undefined)).toEqual([])
  })
})
