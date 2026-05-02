/**
 * Composable tests for production task assignment (Feature 005 US3, T052).
 *
 * Covers:
 *   - GET /api/production-tasks/assignees with search + limit params.
 *   - PUT /api/production-tasks/{id}/assignment with assignment payload.
 *   - 409 stale-version error propagation for caller-side toast.
 */
import { describe, expect, it, vi } from 'vitest'
import {
  fetchProductionTaskAssignees,
  putProductionTaskAssignment,
} from '@/api/composables/use-production-task-detail'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  put: vi.fn(),
}))

vi.mock('@/api/api-client', () => ({
  httpClient: {
    get: mocks.get,
    put: mocks.put,
  },
}))

describe('fetchProductionTaskAssignees (US3 T052)', () => {
  it('calls /api/production-tasks/assignees with search + limit params', async () => {
    mocks.get.mockResolvedValueOnce({
      data: {
        items: [{ id: 'exec-1', displayName: 'Иван Исполнитель', login: 'worker1' }],
      },
    })

    const result = await fetchProductionTaskAssignees('ив', 25)

    expect(mocks.get).toHaveBeenCalledWith('/api/production-tasks/assignees', {
      params: { search: 'ив', limit: 25 },
    })
    expect(result.items).toHaveLength(1)
    expect(result.items[0]?.displayName).toBe('Иван Исполнитель')
  })

  it('defaults limit to 20 when omitted', async () => {
    mocks.get.mockResolvedValueOnce({ data: { items: [] } })
    await fetchProductionTaskAssignees('ив')
    expect(mocks.get).toHaveBeenCalledWith('/api/production-tasks/assignees', {
      params: { search: 'ив', limit: 20 },
    })
  })
})

describe('putProductionTaskAssignment (US3 T052)', () => {
  it('puts assignment payload at /api/production-tasks/{id}/assignment', async () => {
    mocks.put.mockResolvedValueOnce({
      data: {
        id: 'task-1',
        taskNumber: 'PT-000001',
        version: 1,
        purpose: 'X',
        order: { id: 'o', orderNumber: 'ORD-1', customerDisplayName: 'C' },
        quantity: 1,
        uom: 'шт',
        status: 'NOT_STARTED',
        statusLabel: 'не начато',
        allowedActions: [],
        history: [],
        createdAt: '2026-04-27T10:00:00Z',
        updatedAt: '2026-04-27T10:30:00Z',
      },
    })

    const detail = await putProductionTaskAssignment('task-1', {
      expectedVersion: 0,
      executorUserId: 'exec-1',
      plannedStartDate: '2026-05-01',
      plannedFinishDate: '2026-05-03',
      note: 'Планирование смены',
    })

    expect(mocks.put).toHaveBeenCalledWith('/api/production-tasks/task-1/assignment', {
      expectedVersion: 0,
      executorUserId: 'exec-1',
      plannedStartDate: '2026-05-01',
      plannedFinishDate: '2026-05-03',
      note: 'Планирование смены',
    })
    expect(detail.version).toBe(1)
  })

  it('propagates 409 stale version conflicts so the page can show a toast', async () => {
    mocks.put.mockRejectedValueOnce({
      response: { status: 409, data: { code: 'stale_production_task_version' } },
    })

    await expect(putProductionTaskAssignment('task-1', {
      expectedVersion: 0,
      executorUserId: 'exec-1',
    })).rejects.toMatchObject({
      response: { status: 409 },
    })
  })

  it('encodes the task id segment to keep the URL safe with special characters', async () => {
    mocks.put.mockResolvedValueOnce({
      data: { id: 'a/b', taskNumber: 'PT', version: 0, purpose: 'X', order: { id: 'o', orderNumber: '', customerDisplayName: '' }, quantity: 1, uom: '', status: 'NOT_STARTED', statusLabel: '', allowedActions: [], history: [], createdAt: '2026-04-27T10:00:00Z', updatedAt: '2026-04-27T10:00:00Z' },
    })
    await putProductionTaskAssignment('a/b', { expectedVersion: 0, executorUserId: 'exec-1' })
    expect(mocks.put.mock.calls.at(-1)?.[0]).toBe('/api/production-tasks/a%2Fb/assignment')
  })
})
