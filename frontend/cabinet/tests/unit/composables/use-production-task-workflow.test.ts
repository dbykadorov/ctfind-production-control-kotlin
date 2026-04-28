/**
 * Workflow composable tests (Feature 005 US4, T065).
 *
 * Verifies that availableProductionTaskActions returns a fresh array
 * containing the same actions as the backend-provided allowedActions.
 * Workflow mutations live in postProductionTaskStatus
 * (use-production-task-detail.ts) and are exercised here to confirm the
 * stale-version path and assigned-only forbidden path surface to callers.
 */
import type { ProductionTaskAction } from '@/api/types/production-tasks'
import { describe, expect, it, vi } from 'vitest'
import { availableProductionTaskActions } from '@/api/composables/use-production-task-workflow'
import { postProductionTaskStatus } from '@/api/composables/use-production-task-detail'

const mocks = vi.hoisted(() => ({
  post: vi.fn(),
}))

vi.mock('@/api/api-client', () => ({
  httpClient: {
    post: mocks.post,
  },
}))

describe('availableProductionTaskActions (US4 T065)', () => {
  it('returns a copy of the backend-provided allowed actions', () => {
    const actions: ProductionTaskAction[] = ['START', 'BLOCK']
    const result = availableProductionTaskActions(actions)
    expect(result).toEqual(['START', 'BLOCK'])
    expect(result).not.toBe(actions)
  })

  it('returns an empty array when no actions are allowed', () => {
    expect(availableProductionTaskActions([])).toEqual([])
  })

  it('preserves order from the API contract', () => {
    expect(availableProductionTaskActions(['ASSIGN', 'PLAN', 'COMPLETE', 'BLOCK'])).toEqual([
      'ASSIGN',
      'PLAN',
      'COMPLETE',
      'BLOCK',
    ])
  })
})

describe('postProductionTaskStatus payload (US4 T065)', () => {
  it('posts to /api/production-tasks/{id}/status with the workflow payload', async () => {
    mocks.post.mockResolvedValueOnce({
      data: {
        id: 'task-1',
        taskNumber: 'PT-000001',
        purpose: 'X',
        order: { id: 'o', orderNumber: 'ORD-1', customerDisplayName: 'C' },
        quantity: 1,
        uom: 'шт',
        status: 'IN_PROGRESS',
        statusLabel: 'в работе',
        allowedActions: ['COMPLETE', 'BLOCK'],
        history: [],
        createdAt: '2026-04-27T10:00:00Z',
        updatedAt: '2026-04-27T11:00:00Z',
        version: 2,
      },
    })

    await postProductionTaskStatus('task-1', {
      expectedVersion: 1,
      toStatus: 'IN_PROGRESS',
      note: 'Начал работу',
    })

    expect(mocks.post).toHaveBeenCalledWith('/api/production-tasks/task-1/status', {
      expectedVersion: 1,
      toStatus: 'IN_PROGRESS',
      note: 'Начал работу',
    })
  })

  it('sends a block reason when transitioning to BLOCKED', async () => {
    mocks.post.mockResolvedValueOnce({
      data: { id: 'task-1', taskNumber: 'PT', purpose: 'X', order: { id: 'o', orderNumber: '', customerDisplayName: '' }, quantity: 1, uom: '', status: 'BLOCKED', statusLabel: '', allowedActions: [], history: [], createdAt: '2026-04-27T10:00:00Z', updatedAt: '2026-04-27T10:00:00Z', version: 1 },
    })

    await postProductionTaskStatus('task-1', {
      expectedVersion: 1,
      toStatus: 'BLOCKED',
      reason: 'Нет материала',
    })

    expect(mocks.post.mock.calls.at(-1)?.[1]).toMatchObject({
      toStatus: 'BLOCKED',
      reason: 'Нет материала',
    })
  })

  it('propagates 409 stale-version errors so the page can prompt reload', async () => {
    mocks.post.mockRejectedValueOnce({
      response: { status: 409, data: { code: 'stale_production_task_version' } },
    })

    await expect(postProductionTaskStatus('task-1', {
      expectedVersion: 0,
      toStatus: 'IN_PROGRESS',
    })).rejects.toMatchObject({ response: { status: 409 } })
  })

  it('propagates 403 forbidden errors so executors get the assigned-only message', async () => {
    mocks.post.mockRejectedValueOnce({
      response: { status: 403, data: { code: 'forbidden' } },
    })

    await expect(postProductionTaskStatus('task-1', {
      expectedVersion: 0,
      toStatus: 'IN_PROGRESS',
    })).rejects.toMatchObject({ response: { status: 403 } })
  })

  it('propagates 422 invalid-transition errors so the page can show "переход запрещён"', async () => {
    mocks.post.mockRejectedValueOnce({
      response: { status: 422, data: { code: 'invalid_task_status_transition' } },
    })

    await expect(postProductionTaskStatus('task-1', {
      expectedVersion: 0,
      toStatus: 'COMPLETED',
    })).rejects.toMatchObject({ response: { status: 422 } })
  })
})
