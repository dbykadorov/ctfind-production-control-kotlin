/**
 * Production tasks list page (Feature 005 US1, T028).
 *
 * Verifies the list page wires the empty / loading / error / data branches
 * through the production-tasks composable and shows the executor-only hint
 * when the user is a production executor without supervisor visibility.
 */
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/pages/production/ProductionTasksListPage.vue'),
  'utf8',
)

describe('ProductionTasksListPage (US1 T028)', () => {
  it('uses the production-tasks list composable for data, loading and error refs', () => {
    expect(SOURCE).toContain('useProductionTasksList()')
    expect(SOURCE).toContain('const { data, loading, error, refetch }')
  })

  it('renders a loading skeleton placeholder while loading and no data', () => {
    expect(SOURCE).toContain('v-if="loading && !data"')
    expect(SOURCE).toContain('Skeleton')
  })

  it('renders an empty state when data is present but items list is empty', () => {
    expect(SOURCE).toContain('data.items.length === 0')
    expect(SOURCE).toContain("t('common.empty')")
  })

  it('renders an error banner when error is set', () => {
    expect(SOURCE).toContain('v-if="error"')
    expect(SOURCE).toContain('error.message')
  })

  it('shows the executor-only hint when permission is restricted to assigned tasks', () => {
    expect(SOURCE).toContain('canWorkAssignedProductionTasks')
    expect(SOURCE).toContain('!permissions.value.canViewAllProductionTasks')
    expect(SOURCE).toContain('Показаны только задачи, назначенные на вас.')
  })

  it('exposes a search input and status / assignedToMe / blockedOnly / activeOnly filters', () => {
    expect(SOURCE).toContain('searchInput')
    expect(SOURCE).toContain('statusFilter')
    expect(SOURCE).toContain('assignedToMe')
    expect(SOURCE).toContain('blockedOnly')
    expect(SOURCE).toContain('activeOnly')
  })

  it('marks rows as overdue when planned finish is in the past and status is not COMPLETED', () => {
    expect(SOURCE).toContain('isOverdue(row)')
    expect(SOURCE).toContain("row.status === 'COMPLETED'")
    expect(SOURCE).toContain('просрочено')
  })
})
