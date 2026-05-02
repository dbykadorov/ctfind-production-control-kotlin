/**
 * Composable filter wiring + overdueOnly client-side filter
 * (Feature 006 US2, T013).
 *
 * Verifies that filter params are forwarded to the server (size always =
 * 200, plus search/executor/date range) and that overdueOnly is applied
 * client-side after grouping but before COMPLETED cap, leaving the
 * COMPLETED column empty by definition.
 */
import type { ProductionTaskListRowResponse } from '@/api/types/production-tasks'
import { effectScope, nextTick } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useProductionTasksBoard } from '@/api/composables/use-production-tasks-board'

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

function row(overrides: Partial<ProductionTaskListRowResponse> = {}): ProductionTaskListRowResponse {
  return {
    id: 'task-x',
    taskNumber: 'PT-0',
    purpose: 'X',
    order: { id: 'o', orderNumber: 'ORD-1', customerDisplayName: 'C' },
    quantity: 1,
    uom: 'шт',
    status: 'NOT_STARTED',
    statusLabel: 'не начато',
    updatedAt: '2026-04-28T10:00:00Z',
    version: 0,
    ...overrides,
  }
}

beforeEach(() => {
  vi.useFakeTimers()
  vi.setSystemTime(new Date('2026-04-28T10:00:00Z'))
})

afterEach(() => {
  vi.useRealTimers()
})

describe('board filters wiring (US2 T013)', () => {
  it('forwards executor + date range to the API; size stays 200', async () => {
    mocks.get.mockResolvedValueOnce({
      data: { items: [], page: 0, size: 200, totalItems: 0, totalPages: 0 },
    })

    const scope = effectScope()
    scope.run(() => {
      const board = useProductionTasksBoard()
      void board.refetch({
        executorUserId: 'exec-7',
        dueDateFrom: '2026-05-01',
        dueDateTo: '2026-05-31',
      })
    })
    await flushPromises()
    scope.stop()

    const params = mocks.get.mock.calls.at(-1)![1].params as Record<string, unknown>
    expect(params.size).toBe(200)
    expect(params.executorUserId).toBe('exec-7')
    expect(params.dueDateFrom).toBe('2026-05-01')
    expect(params.dueDateTo).toBe('2026-05-31')
    expect(params.overdueOnly).toBeUndefined()
  })
})

describe('overdueOnly client-side filter (US2 T013)', () => {
  it('drops non-overdue rows from all columns when overdueOnly=true', async () => {
    mocks.get.mockResolvedValueOnce({
      data: {
        items: [
          row({ id: 'a', plannedFinishDate: '2026-04-10', status: 'NOT_STARTED' }),
          row({ id: 'b', plannedFinishDate: '2026-05-10', status: 'NOT_STARTED' }),
          row({ id: 'c', plannedFinishDate: '2026-04-10', status: 'IN_PROGRESS' }),
          row({ id: 'd', plannedFinishDate: '2026-05-10', status: 'IN_PROGRESS' }),
        ],
        page: 0,
        size: 200,
        totalItems: 4,
        totalPages: 1,
      },
    })

    const scope = effectScope()
    const board = scope.run(() => {
      const b = useProductionTasksBoard()
      void b.refetch({ overdueOnly: true })
      return b
    })!
    await flushPromises()
    scope.stop()

    const data = board.data.value!
    expect(data.byStatus.NOT_STARTED.map(r => r.id)).toEqual(['a'])
    expect(data.byStatus.IN_PROGRESS.map(r => r.id)).toEqual(['c'])
    expect(data.totalVisible).toBe(2)
  })

  it('overdueOnly leaves COMPLETED column empty by definition (completed tasks are never overdue)', async () => {
    mocks.get.mockResolvedValueOnce({
      data: {
        items: [
          row({ id: 'a', plannedFinishDate: '2026-04-10', status: 'IN_PROGRESS' }),
          row({
            id: 'c',
            plannedFinishDate: '2026-04-10',
            status: 'COMPLETED',
            updatedAt: '2026-04-27T08:00:00Z',
          }),
        ],
        page: 0,
        size: 200,
        totalItems: 2,
        totalPages: 1,
      },
    })

    const scope = effectScope()
    const board = scope.run(() => {
      const b = useProductionTasksBoard()
      void b.refetch({ overdueOnly: true })
      return b
    })!
    await flushPromises()
    scope.stop()

    const data = board.data.value!
    expect(data.byStatus.IN_PROGRESS.map(r => r.id)).toEqual(['a'])
    expect(data.byStatus.COMPLETED).toEqual([])
  })

  it('does NOT send overdueOnly to the API (it is client-only)', async () => {
    mocks.get.mockResolvedValueOnce({
      data: { items: [], page: 0, size: 200, totalItems: 0, totalPages: 0 },
    })

    const scope = effectScope()
    scope.run(() => {
      const board = useProductionTasksBoard()
      void board.refetch({ overdueOnly: true, search: 'q' })
    })
    await flushPromises()
    scope.stop()

    const params = mocks.get.mock.calls.at(-1)![1].params as Record<string, unknown>
    expect(params.search).toBe('q')
    expect(params.overdueOnly).toBeUndefined()
  })
})
