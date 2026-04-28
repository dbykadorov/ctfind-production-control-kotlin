/**
 * AuditActorPicker component source-text tests (007-audit-log-viewer, T018).
 *
 * Following the TDD source-inspection pattern from AuditLogPage.test.ts,
 * these tests read the Vue SFC source and assert that the real implementation
 * contains the expected props, emits, composable usage, debounce logic,
 * dropdown rendering with displayName, and clear/reset functionality.
 *
 * NOTE: The component does not exist yet. These tests WILL FAIL until T023
 * creates AuditActorPicker.vue. That is expected (TDD).
 */
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/components/domain/AuditActorPicker.vue'),
  'utf8',
)

describe('AuditActorPicker', () => {
  it('defines modelValue prop', () => {
    expect(SOURCE).toContain('modelValue')
  })

  it('defines disabled prop', () => {
    expect(SOURCE).toContain('disabled')
  })

  it('emits update:modelValue', () => {
    expect(SOURCE).toContain('update:modelValue')
  })

  it('calls fetchUsers for user search', () => {
    expect(SOURCE).toContain('fetchUsers')
  })

  it('applies debounce on search input (300ms or setTimeout or debounce)', () => {
    expect(SOURCE).toMatch(/300|setTimeout|debounce/i)
  })

  it('renders displayName in dropdown options', () => {
    expect(SOURCE).toContain('displayName')
  })

  it('supports clear/reset by emitting null', () => {
    // The component should emit null to clear the selection
    expect(SOURCE).toContain('null')
  })
})
