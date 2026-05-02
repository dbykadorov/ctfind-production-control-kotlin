import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope, nextTick } from 'vue'
import { dashboardStatsInternals, useDashboardStats } from '@/api/composables/use-dashboard-stats'

const { subscribeListUpdateMock, getMock } = vi.hoisted(() => ({
  subscribeListUpdateMock: vi.fn(() => () => {}),
  getMock: vi.fn(),
}))

vi.mock('@/api/socket', () => ({
  subscribeListUpdate: (...args: unknown[]) => (subscribeListUpdateMock as (...a: unknown[]) => unknown)(...args),
}))

vi.mock('@/api/api-client', () => ({
  httpClient: {
    get: getMock,
  },
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
    getMock.mockReset()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('loads KPI and distribution from Spring dashboard endpoint', async () => {
    getMock.mockResolvedValueOnce({
      data: {
        totalOrders: 7,
        activeOrders: 5,
        overdueOrders: 2,
        statusCounts: {
          NEW: 2,
          IN_WORK: 1,
          READY: 2,
          SHIPPED: 2,
        },
        recentChanges: [],
        trend: [],
      },
    })

    const scope = effectScope()
    const result = scope.run(() => useDashboardStats())!
    await flushPromises()

    expect(getMock).toHaveBeenCalledWith('/api/orders/dashboard', {
      signal: expect.any(AbortSignal),
    })
    expect(result.kpis.value).toEqual({ totalActive: 5, inProgress: 1, ready: 2, overdue: 2 })
    expect(result.distribution.value).toEqual([
      { status: 'новый', count: 2, percent: 29 },
      { status: 'в работе', count: 1, percent: 14 },
      { status: 'готов', count: 2, percent: 29 },
      { status: 'отгружен', count: 2, percent: 29 },
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
