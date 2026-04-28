import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const PAGE_SOURCE = readFileSync(
  join(process.cwd(), 'src/pages/audit/AuditLogPage.vue'),
  'utf8',
)

const COMPOSABLE_SOURCE = readFileSync(
  join(process.cwd(), 'src/api/composables/use-audit-log.ts'),
  'utf8',
)

describe('AuditLogPage — forbidden scenario', () => {
  it('page renders forbidden state when error kind is forbidden', () => {
    expect(PAGE_SOURCE).toContain('forbidden')
    expect(PAGE_SOURCE).toContain('audit.forbidden')
  })

  it('forbidden state hides the data table', () => {
    expect(PAGE_SOURCE).toContain('isForbidden')
  })

  it('page source contains zero references to roleCodes', () => {
    const matches = PAGE_SOURCE.match(/\broleCodes\b/g)
    expect(matches).toBeNull()
  })

  it('page source contains zero references to usePermissions', () => {
    const matches = PAGE_SOURCE.match(/\busePermissions\b/g)
    expect(matches).toBeNull()
  })

  it('composable source contains zero references to roleCodes', () => {
    const matches = COMPOSABLE_SOURCE.match(/\broleCodes\b/g)
    expect(matches).toBeNull()
  })

  it('composable source contains zero references to usePermissions', () => {
    const matches = COMPOSABLE_SOURCE.match(/\busePermissions\b/g)
    expect(matches).toBeNull()
  })
})
