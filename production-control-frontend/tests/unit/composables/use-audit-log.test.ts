/**
 * Unit-tests for useAuditLog composable.
 *
 * Verifies fetch with default 7-day range, filter forwarding, empty search
 * trimming, error mapping (403 -> forbidden, generic error, CanceledError),
 * and abort-on-refetch behaviour.
 */
import { afterEach, describe, expect, it, vi } from 'vitest'
import { effectScope, nextTick } from 'vue'
import { useAuditLog } from '@/api/composables/use-audit-log'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
}))

vi.mock('@/api/api-client', () => ({
  httpClient: {
    get: mocks.get,
  },
}))

async function flushPromises(): Promise<void> {
  await nextTick()
  await Promise.resolve()
  await nextTick()
}

function pageResponse(items: unknown[] = [], page = 0, size = 50, totalItems = items.length) {
  return {
    data: { items, page, size, totalItems, totalPages: Math.ceil(totalItems / size) || 1 },
  }
}

describe('useAuditLog', () => {
  afterEach(() => {
    mocks.get.mockReset()
    vi.useRealTimers()
  })

  it('fetches /api/audit with default 7-day range and page=0 size=50', async () => {
    const now = new Date('2026-04-28T12:00:00Z')
    vi.setSystemTime(now)

    mocks.get.mockResolvedValueOnce(pageResponse())

    const scope = effectScope()
    scope.run(() => {
      const c = useAuditLog()
      void c.refetch()
      return c
    })!
    await flushPromises()
    scope.stop()

    expect(mocks.get).toHaveBeenCalledTimes(1)
    const [url, options] = mocks.get.mock.calls[0]!
    expect(url).toBe('/api/audit')

    const params = options.params
    expect(params.page).toBe(0)
    expect(params.size).toBe(50)

    // from should be ~7 days ago
    const fromDate = new Date(params.from)
    const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
    expect(Math.abs(fromDate.getTime() - sevenDaysAgo.getTime())).toBeLessThan(60_000)

    // to should be ~now
    const toDate = new Date(params.to)
    expect(Math.abs(toDate.getTime() - now.getTime())).toBeLessThan(60_000)
  })

  it('forwards filter params correctly', async () => {
    mocks.get.mockResolvedValueOnce(pageResponse())

    const scope = effectScope()
    scope.run(() => {
      const c = useAuditLog()
      void c.refetch({
        category: ['AUTH', 'ORDER'],
        actorUserId: 'user-1',
        search: 'test query',
        page: 2,
      })
      return c
    })!
    await flushPromises()
    scope.stop()

    const [, options] = mocks.get.mock.calls[0]!
    const params = options.params

    // category may be passed as an array or repeated params
    expect(params.category).toEqual(['AUTH', 'ORDER'])
    expect(params.actorUserId).toBe('user-1')
    expect(params.search).toBe('test query')
    expect(params.page).toBe(2)
  })

  it('trims empty search', async () => {
    mocks.get.mockResolvedValueOnce(pageResponse())

    const scope = effectScope()
    scope.run(() => {
      const c = useAuditLog()
      void c.refetch({ search: '  ' })
    })
    await flushPromises()
    scope.stop()

    const [, options] = mocks.get.mock.calls[0]!
    const params = options.params
    expect(params.search).toBeUndefined()
  })

  it('maps 403 response to forbidden error', async () => {
    mocks.get.mockRejectedValueOnce({
      response: { status: 403, data: { code: 'forbidden' } },
    })

    const scope = effectScope()
    const composable = scope.run(() => {
      const c = useAuditLog()
      void c.refetch()
      return c
    })!
    await flushPromises()
    scope.stop()

    expect(composable.error.value).toEqual({ kind: 'forbidden' })
    expect(composable.data.value).toBeNull()
  })

  it('maps network error to generic error', async () => {
    mocks.get.mockRejectedValueOnce(new Error('Network Error'))

    const scope = effectScope()
    const composable = scope.run(() => {
      const c = useAuditLog()
      void c.refetch()
      return c
    })!
    await flushPromises()
    scope.stop()

    expect(composable.error.value).toEqual(expect.objectContaining({ kind: 'error' }))
  })

  it('swallows CanceledError on abort', async () => {
    const canceledError = new Error('canceled')
    canceledError.name = 'CanceledError'
    mocks.get.mockRejectedValueOnce(canceledError)

    const scope = effectScope()
    const composable = scope.run(() => {
      const c = useAuditLog()
      void c.refetch()
      return c
    })!
    await flushPromises()
    scope.stop()

    expect(composable.error.value).toBeNull()
  })

  it('aborts prior request on re-fetch', async () => {
    let firstSignal: AbortSignal | undefined

    mocks.get.mockImplementation((_url: string, options: { signal?: AbortSignal }) => {
      if (!firstSignal) {
        firstSignal = options.signal
      }
      return new Promise(() => {}) // never resolves
    })

    const scope = effectScope()
    const composable = scope.run(() => useAuditLog())!

    // first call
    void composable.refetch()
    await flushPromises()

    expect(firstSignal).toBeInstanceOf(AbortSignal)
    expect(firstSignal!.aborted).toBe(false)

    // second call should abort the first
    void composable.refetch()
    await flushPromises()

    expect(firstSignal!.aborted).toBe(true)
    expect(mocks.get).toHaveBeenCalledTimes(2)

    scope.stop()
  })
})
