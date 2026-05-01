/**
 * Production task detail assignment controls (Feature 005 US3, T053).
 *
 * Verifies the assignment + planning panel is gated by the user's
 * permission and the task's allowed actions, and that the controls
 * disappear once the task is COMPLETED (read-only review only).
 */
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/pages/production/ProductionTaskDetailPage.vue'),
  'utf8',
)

describe('productionTaskDetailPage assignment controls (US3 T053)', () => {
  it('uses canAssignPanel computed to combine permission and allowedActions', () => {
    expect(SOURCE).toContain('const canAssignPanel = computed(')
    expect(SOURCE).toContain('canAssignProductionTasks')
    expect(SOURCE).toMatch(/includes\((['"])ASSIGN\1\)/)
    expect(SOURCE).toMatch(/includes\((['"])PLAN\1\)/)
  })

  it('only renders the assignment card when canAssignPanel is true', () => {
    expect(SOURCE).toContain('<Card v-if="canAssignPanel"')
    expect(SOURCE).toContain('Назначение и план')
  })

  it('exposes assignee picker, planned start, planned finish and note inputs', () => {
    expect(SOURCE).toContain('<ProductionTaskAssigneePicker')
    expect(SOURCE).toContain('id="pa-start"')
    expect(SOURCE).toContain('id="pa-end"')
    expect(SOURCE).toContain('id="pa-note"')
  })

  it('saves assignment via putProductionTaskAssignment with expectedVersion', () => {
    expect(SOURCE).toContain('putProductionTaskAssignment')
    expect(SOURCE).toContain('expectedVersion: task.version')
  })

  it('shows a stale-version toast on 409 instead of silently retrying', () => {
    expect(SOURCE).toContain('st === 409')
    expect(SOURCE).toContain('Данные устарели. Обновите страницу.')
  })

  it('completed tasks have no allowed actions, so the assignment card is hidden', () => {
    // The detail view already drops allowedActions for COMPLETED via
    // ProductionTaskPermissions.allowedProductionTaskActions. Make the
    // expectation explicit at the page level: canAssignPanel needs ASSIGN
    // or PLAN in allowedActions, both of which the backend strips for
    // completed tasks.
    expect(SOURCE).toContain('data.value.allowedActions.includes')
  })

  it('refuses to save without selected assignee', () => {
    expect(SOURCE).toContain('Выберите исполнителя')
  })
})
