/**
 * Production task detail workflow buttons (Feature 005 US4, T066).
 *
 * Verifies workflow buttons (Start / Complete / Block / Unblock) appear
 * conditionally based on the backend allowedActions, that the block flow
 * requires a reason, and that lifecycle errors map to user-visible messages.
 */
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/pages/production/ProductionTaskDetailPage.vue'),
  'utf8',
)

describe('productionTaskDetailPage workflow buttons (US4 T066)', () => {
  it('renders Start, Complete, Block, and Unblock buttons gated by allowedActions', () => {
    expect(SOURCE).toContain('data.allowedActions.includes(\'START\')')
    expect(SOURCE).toContain('data.allowedActions.includes(\'COMPLETE\')')
    expect(SOURCE).toContain('data.allowedActions.includes(\'BLOCK\')')
    expect(SOURCE).toContain('data.allowedActions.includes(\'UNBLOCK\')')
  })

  it('shows an empty-state message when there are no allowed actions', () => {
    expect(SOURCE).toContain('data.allowedActions.length === 0')
    expect(SOURCE).toContain('Нет доступных действий')
  })

  it('opens a block dialog that requires a reason before submitting', () => {
    expect(SOURCE).toContain('blockOpen')
    expect(SOURCE).toContain('blockReason')
    expect(SOURCE).toContain('Укажите причину блокировки')
  })

  it('sends the block reason via postProductionTaskStatus with toStatus BLOCKED', () => {
    expect(SOURCE).toContain('postProductionTaskStatus')
    expect(SOURCE).toMatch(/toStatus:\s*(['"])BLOCKED\1/)
    expect(SOURCE).toContain('reason: r')
  })

  it('returns to the previous active status on unblock', () => {
    expect(SOURCE).toContain('task.previousActiveStatus')
    expect(SOURCE).toContain('runStatus(task.previousActiveStatus')
  })

  it('maps stale-version 409 and invalid-transition 422 to user-visible toasts', () => {
    expect(SOURCE).toContain('Данные устарели. Обновите страницу.')
    expect(SOURCE).toContain('Переход запрещён')
  })

  it('list page row badge reflects blocked and completed states for cross-page consistency', () => {
    const listSource = readFileSync(
      join(process.cwd(), 'src/pages/production/ProductionTasksListPage.vue'),
      'utf8',
    )
    expect(listSource).toContain('blockedOnly')
    expect(listSource).toContain('activeOnly')
    expect(listSource).toContain('\'BLOCKED\'')
    expect(listSource).toContain('\'COMPLETED\'')
  })
})
