/**
 * Audit log page filter panel source-text tests.
 *
 * Following the TDD source-inspection pattern from AuditLogPage.test.ts,
 * these tests read the Vue SFC source and assert that the filter panel
 * contains date range inputs, category multi-select, actor picker,
 * debounced search, reset button, pagination controls, and page reset
 * on filter change.
 *
 * NOTE: These tests WILL FAIL until Phase 4 implementation (T024) adds
 * the filter panel to the page. That is expected (TDD).
 */
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/pages/audit/AuditLogPage.vue'),
  'utf8',
)

describe('AuditLogPage — filter panel', () => {
  describe('date range inputs', () => {
    it('has a date-from input bound to filter state', () => {
      expect(SOURCE).toMatch(/type=['"]date['"]/)
      expect(SOURCE).toMatch(/dateFrom|date-from|date_from/)
    })

    it('has a date-to input bound to filter state', () => {
      expect(SOURCE).toMatch(/dateTo|date-to|date_to/)
    })
  })

  describe('category multi-select', () => {
    it('renders checkboxes for AUTH category', () => {
      expect(SOURCE).toContain('AUTH')
    })

    it('renders checkboxes for ORDER category', () => {
      expect(SOURCE).toContain('ORDER')
    })

    it('renders checkboxes for PRODUCTION_TASK category', () => {
      expect(SOURCE).toContain('PRODUCTION_TASK')
    })

    it('uses checkbox inputs for category selection', () => {
      expect(SOURCE).toMatch(/type=['"]checkbox['"]|Checkbox/)
    })
  })

  describe('actor picker', () => {
    it('includes the AuditActorPicker component', () => {
      expect(SOURCE).toContain('AuditActorPicker')
    })
  })

  describe('search input', () => {
    it('has a search input with debounce', () => {
      expect(SOURCE).toMatch(/search|query/)
      expect(SOURCE).toMatch(/debounce/)
    })
  })

  describe('reset button', () => {
    it('has a reset button labeled in Russian', () => {
      expect(SOURCE).toMatch(/Сбросить|reset/i)
    })

    it('resets filters when clicked', () => {
      expect(SOURCE).toMatch(/reset\s*\(|resetFilters|handleReset/)
    })
  })

  describe('pagination controls', () => {
    it('has page navigation controls', () => {
      expect(SOURCE).toMatch(/page/)
      expect(SOURCE).toMatch(/totalPages|total-pages|pageCount/)
    })

    it('supports navigating between pages', () => {
      expect(SOURCE).toMatch(/page\s*[+=]|nextPage|prevPage|goToPage|page\.value/)
    })
  })

  describe('page reset on filter change', () => {
    it('resets page to 0 when filters change', () => {
      // When any filter changes, pagination should reset to the first page
      expect(SOURCE).toMatch(/page\s*=\s*0|page\.value\s*=\s*0|resetPage/)
    })
  })
})
