/**
 * Production tasks board — executor scenario (Feature 006 US3, T016).
 *
 * Verifies that for an executor session the page renders all four columns
 * even when most are empty, and that the page contains zero references to
 * client-side role branching (server-side visibility is the only gate).
 */
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const PAGE_SOURCE = readFileSync(
  join(process.cwd(), 'src/pages/production/ProductionTasksBoardPage.vue'),
  'utf8',
)
const COMPOSABLE_SOURCE = readFileSync(
  join(process.cwd(), 'src/api/composables/use-production-tasks-board.ts'),
  'utf8',
)
const CARD_SOURCE = readFileSync(
  join(process.cwd(), 'src/components/domain/ProductionTaskBoardCard.vue'),
  'utf8',
)

function listAllBoardSources(): string[] {
  const out: string[] = []
  out.push(PAGE_SOURCE)
  out.push(COMPOSABLE_SOURCE)
  out.push(CARD_SOURCE)
  return out
}

describe('ProductionTasksBoardPage executor scenario (US3 T016)', () => {
  it('always renders four columns regardless of which are empty', () => {
    // The page maps over a fixed COLUMNS array; columns render unconditionally
    // and each renders its own per-column empty-state when its array is empty.
    expect(PAGE_SOURCE).toContain('COLUMNS: BoardColumn[]')
    expect(PAGE_SOURCE).toContain("v-for=\"column in COLUMNS\"")
    expect(PAGE_SOURCE).toContain('byStatus[column.status].length === 0')
  })

  it('does not gate the columns or cards behind a role / permissions check', () => {
    for (const source of listAllBoardSources()) {
      // No client-side branching on roles. Visibility comes from the server
      // via /api/production-tasks; executors get only their assigned tasks.
      expect(source).not.toContain('usePermissions')
      expect(source).not.toContain('canViewAllProductionTasks')
      expect(source).not.toContain('canWorkAssignedProductionTasks')
      expect(source).not.toMatch(/role(Codes|s)?\s*[=:]/)
      expect(source).not.toContain('PRODUCTION_EXECUTOR')
      expect(source).not.toContain('PRODUCTION_SUPERVISOR')
      expect(source).not.toContain('ORDER_MANAGER')
      expect(source).not.toContain('ADMIN_ROLE')
    }
  })

  it('passes assignedToMe flag through only when caller opts in (server-side only filter)', () => {
    // Composable forwards `assignedToMe` to the backend params only when the
    // caller passes it; visibility for executors comes from the server
    // interpreting the JWT, not from the composable forcing the flag.
    expect(COMPOSABLE_SOURCE).toMatch(/if \(f\.assignedToMe\)\s*\n?\s*p\.assignedToMe = true/)
  })

  it('does not introduce a special hidden-from-executor render branch on the page', () => {
    // FR-014 (clarified Q1=A): executor sees the same full board as supervisors;
    // there must NOT be a v-if that hides the columns for any role.
    const hidesColumnsByRole = /v-if=".*role/i.test(PAGE_SOURCE)
    expect(hidesColumnsByRole).toBe(false)
  })
})

describe('No frontend role-branching across board files (US3 T017)', () => {
  it('grep-style: board source files do not reference any production role constants', () => {
    const boardFiles = collectBoardFiles()
    for (const path of boardFiles) {
      const source = readFileSync(path, 'utf8')
      expect(source).not.toContain('PRODUCTION_SUPERVISOR')
      expect(source).not.toContain('PRODUCTION_EXECUTOR')
      expect(source).not.toContain('ORDER_MANAGER')
      expect(source).not.toContain('ADMIN_ROLE')
    }
  })
})

function collectBoardFiles(): string[] {
  const targets: string[] = []
  // The board page itself
  targets.push(join(process.cwd(), 'src/pages/production/ProductionTasksBoardPage.vue'))
  // The board composable
  targets.push(join(process.cwd(), 'src/api/composables/use-production-tasks-board.ts'))
  // Every component file under domain/ whose name starts with ProductionTaskBoard
  const domainDir = join(process.cwd(), 'src/components/domain')
  for (const entry of readdirSync(domainDir)) {
    const path = join(domainDir, entry)
    if (statSync(path).isFile() && /^ProductionTaskBoard/.test(entry))
      targets.push(path)
  }
  return targets
}
