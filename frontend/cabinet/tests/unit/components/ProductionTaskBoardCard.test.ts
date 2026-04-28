/**
 * Component tests for ProductionTaskBoardCard (Feature 006 US1, T005).
 *
 * Verifies card content rendering: task number, purpose, order context,
 * executor display vs «не назначен», planned-finish + overdue badge,
 * BLOCKED reason rendering with line-clamp class for long text, and
 * absence of blocked-reason for non-BLOCKED rows.
 */
import type { ProductionTaskListRowResponse } from '@/api/types/production-tasks'
import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ProductionTaskBoardCard from '@/components/domain/ProductionTaskBoardCard.vue'

function row(overrides: Partial<ProductionTaskListRowResponse> = {}): ProductionTaskListRowResponse {
  return {
    id: 'task-1',
    taskNumber: 'PT-000001',
    purpose: 'Раскрой',
    order: { id: 'o-1', orderNumber: 'ORD-1', customerDisplayName: 'ООО Ромашка' },
    quantity: 1,
    uom: 'шт',
    status: 'NOT_STARTED',
    statusLabel: 'не начато',
    updatedAt: '2026-04-28T10:00:00Z',
    version: 0,
    ...overrides,
  }
}

beforeEach(() => {
  vi.useFakeTimers()
  vi.setSystemTime(new Date('2026-04-28T10:00:00Z'))
})

afterEach(() => {
  vi.useRealTimers()
})

describe('ProductionTaskBoardCard (US1 T005)', () => {
  it('renders task number, purpose, and order context', () => {
    const wrapper = mount(ProductionTaskBoardCard, {
      props: { row: row({ purpose: 'Фрезеровка кромки' }) },
    })
    expect(wrapper.text()).toContain('PT-000001')
    expect(wrapper.text()).toContain('Фрезеровка кромки')
    expect(wrapper.text()).toContain('ORD-1 · ООО Ромашка')
  })

  it('renders executor display name when present', () => {
    const wrapper = mount(ProductionTaskBoardCard, {
      props: { row: row({ executor: { id: 'e-1', displayName: 'Иван Исполнитель' } }) },
    })
    expect(wrapper.text()).toContain('Иван Исполнитель')
  })

  it('renders «не назначен» when executor is missing', () => {
    const wrapper = mount(ProductionTaskBoardCard, {
      props: { row: row({ executor: undefined }) },
    })
    expect(wrapper.text()).toContain('не назначен')
  })

  it('renders «срок не указан» when plannedFinishDate is missing', () => {
    const wrapper = mount(ProductionTaskBoardCard, {
      props: { row: row({ plannedFinishDate: undefined }) },
    })
    expect(wrapper.text()).toContain('срок не указан')
  })

  it('shows overdue badge when planned finish is in the past and status is not COMPLETED', () => {
    const wrapper = mount(ProductionTaskBoardCard, {
      props: { row: row({ plannedFinishDate: '2026-04-01', status: 'IN_PROGRESS' }) },
    })
    expect(wrapper.find('[data-testid="production-task-board-card-overdue"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('просрочено')
  })

  it('does NOT show overdue badge when status is COMPLETED even if planned finish is in the past', () => {
    const wrapper = mount(ProductionTaskBoardCard, {
      props: { row: row({ plannedFinishDate: '2026-04-01', status: 'COMPLETED' }) },
    })
    expect(wrapper.find('[data-testid="production-task-board-card-overdue"]').exists()).toBe(false)
  })

  it('does NOT show overdue badge for future planned finish', () => {
    const wrapper = mount(ProductionTaskBoardCard, {
      props: { row: row({ plannedFinishDate: '2026-05-30', status: 'NOT_STARTED' }) },
    })
    expect(wrapper.find('[data-testid="production-task-board-card-overdue"]').exists()).toBe(false)
  })

  it('renders blockedReason on BLOCKED cards with line-clamp-2 class', () => {
    const longReason = 'Закончились заготовки 18мм; ждём поставку до конца недели; параллельно ищем альтернативу'
    const wrapper = mount(ProductionTaskBoardCard, {
      props: { row: row({ status: 'BLOCKED', blockedReason: longReason }) },
    })
    const reasonEl = wrapper.find('[data-testid="production-task-board-card-blocked-reason"]')
    expect(reasonEl.exists()).toBe(true)
    expect(reasonEl.text()).toContain(longReason)
    expect(reasonEl.classes()).toContain('line-clamp-2')
  })

  it('omits blockedReason rendering when status is NOT_STARTED', () => {
    const wrapper = mount(ProductionTaskBoardCard, {
      props: { row: row({ status: 'NOT_STARTED', blockedReason: 'should not appear' }) },
    })
    expect(wrapper.find('[data-testid="production-task-board-card-blocked-reason"]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('should not appear')
  })

  it('omits blockedReason rendering when status is BLOCKED but reason is empty', () => {
    const wrapper = mount(ProductionTaskBoardCard, {
      props: { row: row({ status: 'BLOCKED', blockedReason: '' }) },
    })
    expect(wrapper.find('[data-testid="production-task-board-card-blocked-reason"]').exists()).toBe(false)
  })
})
