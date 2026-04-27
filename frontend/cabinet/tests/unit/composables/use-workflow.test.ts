import { describe, expect, it, vi } from 'vitest'
import { effectScope, nextTick, ref } from 'vue'
import { applyTransition, useOrderTransitions } from '@/api/composables/use-workflow'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
}))

vi.mock('@/api/api-client', () => ({
  httpClient: {
    get: mocks.get,
    post: mocks.post,
  },
}))

async function flushPromises(): Promise<void> {
  await nextTick()
  await Promise.resolve()
  await nextTick()
}

describe('useOrderTransitions', () => {
  it('loads one direct forward transition from Spring order detail', async () => {
    mocks.get.mockResolvedValueOnce({
      data: orderDetailResponse({ status: 'NEW', statusLabel: 'новый', version: 4 }),
    })

    const scope = effectScope()
    const result = scope.run(() => useOrderTransitions(ref('order-1')))!
    await flushPromises()
    scope.stop()

    expect(mocks.get).toHaveBeenCalledWith('/api/orders/order-1', {
      signal: expect.any(AbortSignal),
    })
    expect(result.data.value).toEqual([
      {
        action: 'В работу',
        state: 'новый',
        next_state: 'в работе',
        allowed: 'ORDER_MANAGER',
      },
    ])
  })

  it('does not offer transitions for shipped orders', async () => {
    mocks.get.mockResolvedValueOnce({
      data: orderDetailResponse({ status: 'SHIPPED', statusLabel: 'отгружен', version: 7 }),
    })

    const scope = effectScope()
    const result = scope.run(() => useOrderTransitions(ref('order-1')))!
    await flushPromises()
    scope.stop()

    expect(result.data.value).toEqual([])
  })
})

describe('applyTransition', () => {
  it('posts target status with cached expected version', async () => {
    mocks.get.mockResolvedValueOnce({
      data: orderDetailResponse({ status: 'NEW', statusLabel: 'новый', version: 4 }),
    })
    const scope = effectScope()
    const result = scope.run(() => useOrderTransitions(ref('order-1')))!
    await flushPromises()
    expect(result.data.value).toHaveLength(1)
    mocks.post.mockResolvedValueOnce({
      data: orderDetailResponse({ status: 'IN_WORK', statusLabel: 'в работе', version: 5 }),
    })

    await applyTransition('order-1', 'В работу')
    scope.stop()

    expect(mocks.post).toHaveBeenCalledWith('/api/orders/order-1/status', {
      expectedVersion: 4,
      toStatus: 'IN_WORK',
    })
  })
})

function orderDetailResponse(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    id: 'order-1',
    orderNumber: 'ORD-000001',
    customer: {
      id: 'customer-1',
      displayName: 'ООО Ромашка',
      status: 'ACTIVE',
    },
    deliveryDate: '2026-05-15',
    status: 'NEW',
    statusLabel: 'новый',
    notes: 'Test order',
    items: [],
    history: [],
    changeDiffs: [],
    createdAt: '2026-04-26T18:00:00Z',
    updatedAt: '2026-04-26T18:30:00Z',
    version: 2,
    overdue: false,
    ...overrides,
  }
}
