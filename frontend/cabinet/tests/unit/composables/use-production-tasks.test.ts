import { afterEach, describe, expect, it, vi } from 'vitest'
import { effectScope, nextTick } from 'vue'
import { useProductionTasksList } from '@/api/composables/use-production-tasks'

const { getMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
}))

vi.mock('@/api/api-client', () => ({
  httpClient: { get: getMock },
}))

async function flush(): Promise<void> {
  await nextTick()
  await Promise.resolve()
  await nextTick()
}

describe('useProductionTasksList', () => {
  afterEach(() => {
    getMock.mockReset()
  })

  it('loads paged tasks from Spring API', async () => {
    getMock.mockResolvedValueOnce({
      data: {
        items: [
          {
            id: '2f7e1d0f-4b1f-4276-8e99-0e9d3f07e917',
            taskNumber: 'PT-000001',
            purpose: 'Раскрой',
            order: {
              id: '95849543-a34d-4d8b-828a-981f46bfb63f',
              orderNumber: 'ORD-000001',
              customerDisplayName: 'Acme',
            },
            orderItem: null,
            quantity: 2,
            uom: 'шт',
            status: 'NOT_STARTED',
            statusLabel: 'не начато',
            executor: null,
            plannedStartDate: null,
            plannedFinishDate: null,
            blockedReason: null,
            updatedAt: '2026-04-27T11:00:00Z',
            version: 0,
          },
        ],
        page: 0,
        size: 20,
        totalItems: 1,
        totalPages: 1,
      },
    })

    const scope = effectScope()
    const { data, loading, error, refetch } = scope.run(() => useProductionTasksList())!
    await refetch({ search: 'ORD', page: 0, size: 20 })
    await flush()

    expect(getMock).toHaveBeenCalledWith('/api/production-tasks', {
      params: { search: 'ORD', page: 0, size: 20 },
      signal: expect.any(AbortSignal),
    })
    expect(loading.value).toBe(false)
    expect(error.value).toBeNull()
    expect(data.value?.items).toHaveLength(1)
    expect(data.value?.items[0]?.taskNumber).toBe('PT-000001')
    scope.stop()
  })
})