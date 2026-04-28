/**
 * Composable test for createProductionTasksFromOrder (Feature 005 US2, T040).
 *
 * Verifies payload mapping (POST body, content layout) and that backend
 * validation errors propagate to the caller for UI handling.
 */
import { describe, expect, it, vi } from 'vitest'
import { createProductionTasksFromOrder } from '@/api/composables/use-production-tasks'

const mocks = vi.hoisted(() => ({
  post: vi.fn(),
}))

vi.mock('@/api/api-client', () => ({
  httpClient: {
    post: mocks.post,
  },
}))

describe('createProductionTasksFromOrder (US2 T040)', () => {
  it('posts to /api/production-tasks/from-order with orderId and tasks payload', async () => {
    mocks.post.mockResolvedValueOnce({
      data: {
        items: [
          { id: 'task-1', taskNumber: 'PT-000001', status: 'NOT_STARTED', version: 0 },
        ],
      },
    })

    const result = await createProductionTasksFromOrder('order-1', [
      {
        orderItemId: 'item-1',
        purpose: 'Раскрой',
        quantity: 2,
        uom: 'шт',
        executorUserId: 'exec-1',
        plannedStartDate: '2026-05-01',
        plannedFinishDate: '2026-05-03',
      },
    ])

    expect(mocks.post).toHaveBeenCalledWith('/api/production-tasks/from-order', {
      orderId: 'order-1',
      tasks: [
        {
          orderItemId: 'item-1',
          purpose: 'Раскрой',
          quantity: 2,
          uom: 'шт',
          executorUserId: 'exec-1',
          plannedStartDate: '2026-05-01',
          plannedFinishDate: '2026-05-03',
        },
      ],
    })
    expect(result.items).toHaveLength(1)
    expect(result.items[0]?.taskNumber).toBe('PT-000001')
  })

  it('propagates backend validation errors to the caller', async () => {
    mocks.post.mockRejectedValueOnce({
      response: { status: 400, data: { code: 'validation_failed', message: 'Order item not found.' } },
    })

    await expect(createProductionTasksFromOrder('order-1', [
      { orderItemId: 'missing', purpose: 'X', quantity: 1, uom: 'шт' },
    ])).rejects.toMatchObject({
      response: { status: 400, data: { code: 'validation_failed' } },
    })
  })

  it('omits optional fields when not provided', async () => {
    mocks.post.mockResolvedValueOnce({ data: { items: [] } })

    await createProductionTasksFromOrder('order-1', [
      { orderItemId: 'item-1', purpose: 'Раскрой', quantity: 1, uom: 'шт' },
    ])

    const call = mocks.post.mock.calls.at(-1)
    expect(call?.[0]).toBe('/api/production-tasks/from-order')
    const task = (call?.[1] as { tasks: Array<Record<string, unknown>> }).tasks[0]
    expect(task?.executorUserId).toBeUndefined()
    expect(task?.plannedStartDate).toBeUndefined()
    expect(task?.plannedFinishDate).toBeUndefined()
  })
})
