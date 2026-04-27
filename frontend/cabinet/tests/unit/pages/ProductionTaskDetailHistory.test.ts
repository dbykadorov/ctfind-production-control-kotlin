/**
 * Production task detail history (Feature 005 US5, T077).
 *
 * Mounts ProductionTaskTimeline (the component embedded in the detail page)
 * with realistic API events and asserts chronological order, actor and
 * timestamp display, and detail lines for status, assignment, planning, and
 * block reason transitions.
 */
import type { ProductionTaskHistoryEventResponse } from '@/api/types/production-tasks'
import { mount } from '@vue/test-utils'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import ProductionTaskTimeline from '@/components/domain/ProductionTaskTimeline.vue'

const DETAIL_PAGE_SOURCE = readFileSync(
  join(process.cwd(), 'src/pages/production/ProductionTaskDetailPage.vue'),
  'utf8',
)

const HISTORY: ProductionTaskHistoryEventResponse[] = [
  {
    type: 'CREATED',
    actorDisplayName: 'Менеджер Анна',
    eventAt: '2026-04-27T10:00:00Z',
    fromStatus: null,
    toStatus: 'NOT_STARTED',
  },
  {
    type: 'ASSIGNED',
    actorDisplayName: 'Менеджер Анна',
    eventAt: '2026-04-27T11:00:00Z',
    previousExecutorDisplayName: null,
    newExecutorDisplayName: 'Иван',
  },
  {
    type: 'PLANNING_UPDATED',
    actorDisplayName: 'Менеджер Анна',
    eventAt: '2026-04-27T11:30:00Z',
    plannedStartDateBefore: null,
    plannedStartDateAfter: '2026-05-01',
    plannedFinishDateBefore: null,
    plannedFinishDateAfter: '2026-05-03',
  },
  {
    type: 'STATUS_CHANGED',
    actorDisplayName: 'Иван',
    eventAt: '2026-04-27T12:00:00Z',
    fromStatus: 'NOT_STARTED',
    toStatus: 'IN_PROGRESS',
  },
  {
    type: 'BLOCKED',
    actorDisplayName: 'Иван',
    eventAt: '2026-04-27T13:00:00Z',
    fromStatus: 'IN_PROGRESS',
    toStatus: 'BLOCKED',
    reason: 'Нет материала',
    note: 'Ожидаем поставку',
  },
]

describe('ProductionTaskDetailHistory (US5)', () => {
  it('detail page wires ProductionTaskTimeline through data.history', () => {
    expect(DETAIL_PAGE_SOURCE).toContain('import ProductionTaskTimeline')
    expect(DETAIL_PAGE_SOURCE).toContain(':history="data.history"')
  })

  it('renders one entry per history event in chronological API order', () => {
    const wrapper = mount(ProductionTaskTimeline, { props: { history: HISTORY } })
    const entries = wrapper.findAll('[data-testid="production-task-timeline-entry"]')
    expect(entries.length).toBe(HISTORY.length)
    const titles = entries.map(li => li.text())
    expect(titles[0]).toContain('Задача создана')
    expect(titles[1]).toContain('Назначен исполнитель')
    expect(titles[2]).toContain('План скорректирован')
    expect(titles[3]).toContain('Изменён статус')
    expect(titles[4]).toContain('Заблокировано')
  })

  it('shows actor display name on each entry', () => {
    const wrapper = mount(ProductionTaskTimeline, { props: { history: HISTORY } })
    expect(wrapper.text()).toContain('Менеджер Анна')
    expect(wrapper.text()).toContain('Иван')
  })

  it('renders status arrow for STATUS_CHANGED events', () => {
    const wrapper = mount(ProductionTaskTimeline, { props: { history: HISTORY } })
    expect(wrapper.text()).toContain('Статус: не начато → в работе')
  })

  it('renders assignment line on ASSIGNED events', () => {
    const wrapper = mount(ProductionTaskTimeline, { props: { history: HISTORY } })
    expect(wrapper.text()).toContain('Исполнитель: Иван')
  })

  it('renders planning before/after on PLANNING_UPDATED events', () => {
    const wrapper = mount(ProductionTaskTimeline, { props: { history: HISTORY } })
    expect(wrapper.text()).toContain('План: начало')
    expect(wrapper.text()).toContain('План: окончание')
    expect(wrapper.text()).toContain('1 мая 2026')
    expect(wrapper.text()).toContain('3 мая 2026')
  })

  it('renders block reason and note details', () => {
    const wrapper = mount(ProductionTaskTimeline, { props: { history: HISTORY } })
    expect(wrapper.text()).toContain('Причина: Нет материала')
    expect(wrapper.text()).toContain('Комментарий: Ожидаем поставку')
  })

  it('shows empty fallback when there are no events', () => {
    const wrapper = mount(ProductionTaskTimeline, { props: { history: [] } })
    expect(wrapper.text()).toContain('История пуста')
  })
})
