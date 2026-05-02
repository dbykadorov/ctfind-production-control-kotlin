import { describe, expect, it, vi } from 'vitest'
import { effectScope, nextTick, ref } from 'vue'
import { useOrderHistory } from '@/api/composables/use-history'

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

describe('useOrderHistory', () => {
  it('loads status changes and business-field diffs from Spring order detail', async () => {
    mocks.get.mockResolvedValueOnce({
      data: {
        history: [
          {
            type: 'STATUS_CHANGED',
            fromStatus: 'NEW',
            toStatus: 'IN_WORK',
            actorDisplayName: 'Manager',
            changedAt: '2026-04-26T19:00:00Z',
            note: 'Started',
          },
        ],
        changeDiffs: [
          {
            type: 'UPDATED',
            actorDisplayName: 'Manager',
            changedAt: '2026-04-26T18:30:00Z',
            fieldDiffs: [
              {
                fieldname: 'delivery_date',
                fieldLabel: 'Срок исполнения',
                fromValue: '2026-05-15',
                toValue: '2026-05-20',
              },
            ],
          },
        ],
      },
    })

    const scope = effectScope()
    const result = scope.run(() => useOrderHistory(ref('order-1')))!
    await flushPromises()
    scope.stop()

    expect(mocks.get).toHaveBeenCalledWith('/api/orders/order-1', {
      signal: expect.any(AbortSignal),
    })
    expect(result.entries.value).toEqual([
      {
        id: 'status:2026-04-26T19:00:00Z:IN_WORK',
        kind: 'status',
        at: '2026-04-26T19:00:00Z',
        actor: 'Manager',
        actor_label: 'Manager',
        details: [
          { fieldname: 'status', from_value: 'NEW', to_value: 'IN_WORK' },
          { fieldname: 'note', to_value: 'Started' },
        ],
      },
      {
        id: 'diff:2026-04-26T18:30:00Z:0',
        kind: 'edit',
        at: '2026-04-26T18:30:00Z',
        actor: 'Manager',
        actor_label: 'Manager',
        details: [
          {
            fieldname: 'delivery_date',
            field_label: 'Срок исполнения',
            from_value: '2026-05-15',
            to_value: '2026-05-20',
          },
        ],
      },
    ])
  })
})
