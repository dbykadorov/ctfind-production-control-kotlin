import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope, nextTick } from 'vue'
import { recentActivityInternals, useRecentActivity } from '@/api/composables/use-recent-activity'

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
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('returns an empty list without network calls', async () => {
    const scope = effectScope()
    const result = scope.run(() => useRecentActivity())!
    await flushPromises()

    expect(result.data.value).toEqual([])
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
