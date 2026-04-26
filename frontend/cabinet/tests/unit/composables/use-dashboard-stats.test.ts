import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope, nextTick } from 'vue'
import { dashboardStatsInternals, useDashboardStats } from '@/api/composables/use-dashboard-stats'

const { subscribeListUpdateMock } = vi.hoisted(() => ({
  subscribeListUpdateMock: vi.fn(() => () => {}),
}))

vi.mock('@/api/socket', () => ({
  subscribeListUpdate: (...args: unknown[]) => (subscribeListUpdateMock as (...a: unknown[]) => unknown)(...args),
}))

async function flushPromises(): Promise<void> {
  await nextTick()
  await Promise.resolve()
  await nextTick()
}

describe('use-dashboard-stats / constants', () => {
  it('debounce 1500мс, polling 30 секунд, статусы в порядке lifecycle', () => {
    expect(dashboardStatsInternals.REALTIME_DEBOUNCE_MS).toBe(1500)
    expect(dashboardStatsInternals.FALLBACK_POLL_MS).toBe(30_000)
    expect(dashboardStatsInternals.STATUS_ORDER).toEqual(['новый', 'в работе', 'готов', 'отгружен'])
  })
})

describe('use-dashboard-stats / Spring placeholder', () => {
  beforeEach(() => {
    subscribeListUpdateMock.mockClear()
    subscribeListUpdateMock.mockImplementation(() => () => {})
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('returns zero KPI and distribution without network calls', async () => {
    const scope = effectScope()
    const result = scope.run(() => useDashboardStats())!
    await flushPromises()

    expect(result.kpis.value).toEqual({ totalActive: 0, inProgress: 0, ready: 0, overdue: 0 })
    expect(result.distribution.value).toEqual([
      { status: 'новый', count: 0, percent: 0 },
      { status: 'в работе', count: 0, percent: 0 },
      { status: 'готов', count: 0, percent: 0 },
      { status: 'отгружен', count: 0, percent: 0 },
    ])
    expect(result.error.value).toBeNull()
    expect(result.loading.value).toBe(false)
    scope.stop()
  })

  it('keeps list subscription contract for future realtime invalidation', async () => {
    const scope = effectScope()
    scope.run(() => useDashboardStats())
    await flushPromises()

    expect(subscribeListUpdateMock).toHaveBeenCalledWith('Customer Order', expect.any(Function))
    scope.stop()
  })

  it('unsubscribes on dispose', async () => {
    const unsubscribe = vi.fn()
    subscribeListUpdateMock.mockReturnValueOnce(unsubscribe)

    const scope = effectScope()
    scope.run(() => useDashboardStats())
    await flushPromises()
    scope.stop()

    expect(unsubscribe).toHaveBeenCalled()
  })
})
