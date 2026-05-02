/**
 * Unit-tests for useProductionTasksBoard (Feature 006 US1, T004).
 *
 * Verifies fetch with size=200, grouping into all 4 status keys (including
 * empty arrays), COMPLETED window cap (last 7d × max 30), `truncated` flag,
 * abort handling, and 403 → forbidden mapping.
 */
import type { ProductionTaskListRowResponse } from '@/api/types/production-tasks'
import { effectScope, nextTick } from 'vue'
import { describe, expect, it, vi } from 'vitest'
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
    id: 'task-1',
    taskNumber: 'PT-000001',
    purpose: 'Раскрой',
    order: { id: 'o-1', orderNumber: 'ORD-1', customerDisplayName: 'Acme' },
    quantity: 1,
    uom: 'шт',
    status: 'NOT_STARTED',
    statusLabel: 'не начато',
    updatedAt: '2026-04-28T10:00:00Z',
    version: 0,
    ...overrides,
  }
}

function pageResponse(rows: ProductionTaskListRowResponse[], totalItems = rows.length) {
  return {
    data: { items: rows, page: 0, size: 200, totalItems, totalPages: 1 },
  }
}

describe('useProductionTasksBoard (US1 T004)', () => {
  it('fetches /api/production-tasks with size=200 and merges filter params', async () => {
    mocks.get.mockResolvedValueOnce(pageResponse([]))

    const scope = effectScope()
    scope.run(() => {
      const board = useProductionTasksBoard()
      void board.refetch({ search: 'PT-000', executorUserId: 'exec-1', dueDateFrom: '2026-05-01' })
    })
    await flushPromises()
    scope.stop()

    const [url, options] = mocks.get.mock.calls.at(-1)!
    expect(url).toBe('/api/production-tasks')
    expect(options).toMatchObject({
      params: {
        size: 200,
        search: 'PT-000',
        executorUserId: 'exec-1',
        dueDateFrom: '2026-05-01',
      },
    })
    expect(options.signal).toBeInstanceOf(AbortSignal)
  })

  it('groups rows into all four status keys, leaving empty columns as []', async () => {
    mocks.get.mockResolvedValueOnce(pageResponse([
      row({ id: 'a', status: 'NOT_STARTED' }),
      row({ id: 'b', status: 'IN_PROGRESS' }),
    ]))

    const scope = effectScope()
    const board = scope.run(() => {
      const b = useProductionTasksBoard()
      void b.refetch()
      return b
    })!
    await flushPromises()
    scope.stop()

    const data = board.data.value!
    expect(Object.keys(data.byStatus).sort()).toEqual(
      ['BLOCKED', 'COMPLETED', 'IN_PROGRESS', 'NOT_STARTED'],
    )
    expect(data.byStatus.NOT_STARTED.map(r => r.id)).toEqual(['a'])
    expect(data.byStatus.IN_PROGRESS.map(r => r.id)).toEqual(['b'])
    expect(data.byStatus.BLOCKED).toEqual([])
    expect(data.byStatus.COMPLETED).toEqual([])
    expect(data.totalVisible).toBe(2)
  })

  it('caps COMPLETED to 30 most recent within 7 days, drops older completed', async () => {
    const now = new Date('2026-04-28T10:00:00Z')
    vi.setSystemTime(now)

    const completed: ProductionTaskListRowResponse[] = []
    // 35 completed within window: 4 hours apart starting 1h ago
    for (let i = 0; i < 35; i++) {
      const t = new Date(now.getTime() - 60 * 60 * 1000 - i * 4 * 60 * 60 * 1000)
      completed.push(row({
        id: `recent-${i}`,
        status: 'COMPLETED',
        updatedAt: t.toISOString(),
      }))
    }
    // 3 completed older than 7 days
    completed.push(row({
      id: 'old-1',
      status: 'COMPLETED',
      updatedAt: new Date(now.getTime() - 8 * 24 * 60 * 60 * 1000).toISOString(),
    }))
    completed.push(row({
      id: 'old-2',
      status: 'COMPLETED',
      updatedAt: new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString(),
    }))

    mocks.get.mockResolvedValueOnce(pageResponse(completed))

    const scope = effectScope()
    const board = scope.run(() => {
      const b = useProductionTasksBoard()
      void b.refetch()
      return b
    })!
    await flushPromises()
    scope.stop()
    vi.useRealTimers()

    const data = board.data.value!
    expect(data.byStatus.COMPLETED).toHaveLength(30)
    // sorted descending by updatedAt
    expect(data.byStatus.COMPLETED[0]!.id).toBe('recent-0')
    expect(data.byStatus.COMPLETED[29]!.id).toBe('recent-29')
    // older-than-window are dropped
    expect(data.byStatus.COMPLETED.every(r => !r.id.startsWith('old-'))).toBe(true)
  })

  it('sets truncated=true when totalItems exceeds 200', async () => {
    mocks.get.mockResolvedValueOnce(pageResponse([row()], 250))

    const scope = effectScope()
    const board = scope.run(() => {
      const b = useProductionTasksBoard()
      void b.refetch()
      return b
    })!
    await flushPromises()
    scope.stop()

    expect(board.data.value!.truncated).toBe(true)
  })

  it('keeps truncated=false when totalItems <= 200', async () => {
    mocks.get.mockResolvedValueOnce(pageResponse([row()], 1))

    const scope = effectScope()
    const board = scope.run(() => {
      const b = useProductionTasksBoard()
      void b.refetch()
      return b
    })!
    await flushPromises()
    scope.stop()

    expect(board.data.value!.truncated).toBe(false)
  })

  it('maps 403 response to forbidden=true and clears data', async () => {
    mocks.get.mockRejectedValueOnce({
      response: { status: 403, data: { code: 'forbidden' } },
    })

    const scope = effectScope()
    const board = scope.run(() => {
      const b = useProductionTasksBoard()
      void b.refetch()
      return b
    })!
    await flushPromises()
    scope.stop()

    expect(board.forbidden.value).toBe(true)
    expect(board.error.value).toBeNull()
    expect(board.data.value).toBeNull()
  })

  it('swallows CanceledError without setting error or forbidden', async () => {
    mocks.get.mockRejectedValueOnce({ name: 'CanceledError' })

    const scope = effectScope()
    const board = scope.run(() => {
      const b = useProductionTasksBoard()
      void b.refetch()
      return b
    })!
    await flushPromises()
    scope.stop()

    expect(board.forbidden.value).toBe(false)
    expect(board.error.value).toBeNull()
  })
})
