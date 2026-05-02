import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope, nextTick } from 'vue'
import { recentActivityInternals, useRecentActivity } from '@/api/composables/use-recent-activity'

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

describe('use-recent-activity / constants', () => {
  it('последние 10 записей, debounce 1500мс, fallback poll 30с', () => {
    expect(recentActivityInternals.PAGE_SIZE).toBe(10)
    expect(recentActivityInternals.REALTIME_DEBOUNCE_MS).toBe(1500)
    expect(recentActivityInternals.FALLBACK_POLL_MS).toBe(30_000)
  })
})

describe('use-recent-activity / Spring placeholder', () => {
  beforeEach(() => {
    subscribeListUpdateMock.mockClear()
    subscribeListUpdateMock.mockImplementation(() => () => {})
    getMock.mockReset()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('loads recent status changes from Spring dashboard endpoint', async () => {
    getMock.mockResolvedValueOnce({
      data: {
        totalOrders: 0,
        activeOrders: 0,
        overdueOrders: 0,
        statusCounts: {},
        recentChanges: [
          {
            orderId: 'order-1',
            orderNumber: 'ORD-000001',
            customerDisplayName: 'ООО Ромашка',
            fromStatus: 'NEW',
            toStatus: 'IN_WORK',
            changedAt: '2026-04-26T19:00:00Z',
            actorDisplayName: 'Manager',
          },
        ],
        trend: [],
      },
    })

    const scope = effectScope()
    const result = scope.run(() => useRecentActivity())!
    await flushPromises()

    expect(getMock).toHaveBeenCalledWith('/api/orders/dashboard', {
      signal: expect.any(AbortSignal),
    })
    expect(result.data.value).toEqual([
      {
        name: 'status:order-1:2026-04-26T19:00:00Z',
        order: 'order-1',
        fromStatus: 'новый',
        toStatus: 'в работе',
        actorUser: 'Manager',
        eventAt: '2026-04-26T19:00:00Z',
      },
    ])
    expect(result.error.value).toBeNull()
    expect(result.loading.value).toBe(false)
    scope.stop()
  })

  it('keeps list subscription contract for future realtime invalidation', async () => {
    const scope = effectScope()
    scope.run(() => useRecentActivity())
    await flushPromises()

    expect(subscribeListUpdateMock).toHaveBeenCalledWith(
      'Customer Order Status Change',
      expect.any(Function),
    )
    scope.stop()
  })

  it('unsubscribes on dispose', async () => {
    const unsubscribe = vi.fn()
    subscribeListUpdateMock.mockReturnValueOnce(unsubscribe)

    const scope = effectScope()
    scope.run(() => useRecentActivity())
    await flushPromises()
    scope.stop()

    expect(unsubscribe).toHaveBeenCalled()
  })
})
