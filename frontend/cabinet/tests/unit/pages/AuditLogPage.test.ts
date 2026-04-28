/**
 * Audit log page source-text tests.
 *
 * Following the TDD source-inspection pattern from ProductionTasksBoardPage.test.ts,
 * these tests read the Vue SFC source and assert that the real implementation
 * (when it replaces the stub) contains the expected composable usage, column
 * headers, loading/error/empty/forbidden states, router links for different
 * target types, and a refresh button.
 *
 * NOTE: The page is a stub during T014. These tests WILL FAIL until T015
 * replaces the stub with the real implementation. That is expected (TDD).
 */
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/pages/audit/AuditLogPage.vue'),
  'utf8',
)

describe('AuditLogPage', () => {
  it('uses the audit log composable', () => {
    expect(SOURCE).toContain('useAuditLog()')
  })

  it('renders table columns: time, category, event, actor, summary', () => {
    // The page should declare column headers (i18n keys or raw labels)
    expect(SOURCE).toMatch(/occurredAt|time|audit\.column\.time/i)
    expect(SOURCE).toMatch(/category|audit\.column\.category/i)
    expect(SOURCE).toMatch(/eventType|event|audit\.column\.event/i)
    expect(SOURCE).toMatch(/actorDisplayName|actor|audit\.column\.actor/i)
    expect(SOURCE).toMatch(/summary|audit\.column\.summary/i)
  })

  it('shows loading skeleton when loading and data is null', () => {
    expect(SOURCE).toContain('loading')
    expect(SOURCE).toContain('Skeleton')
  })

  it('shows empty state message', () => {
    expect(SOURCE).toMatch(/empty|no.?data|audit\.empty/i)
  })

  it('shows error banner with refresh button', () => {
    expect(SOURCE).toContain('error')
    expect(SOURCE).toContain('refetch')
  })

  it('shows forbidden state', () => {
    expect(SOURCE).toContain('forbidden')
  })

  it('renders RouterLink for ORDER targets to orders.detail', () => {
    expect(SOURCE).toContain("name: 'orders.detail'")
    expect(SOURCE).toContain('targetId')
  })

  it('renders RouterLink for PRODUCTION_TASK targets to production-tasks.detail', () => {
    expect(SOURCE).toContain("name: 'production-tasks.detail'")
  })

  it('no link rendered for AUTH category rows — conditional link logic based on targetType', () => {
    // The page should conditionally render links only when targetType is present
    expect(SOURCE).toContain('targetType')
  })

  it('exposes a refresh button', () => {
    expect(SOURCE).toContain('refetch')
  })
})
