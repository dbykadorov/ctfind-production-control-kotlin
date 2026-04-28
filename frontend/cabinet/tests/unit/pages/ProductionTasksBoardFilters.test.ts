/**
 * Production tasks board filter panel (Feature 006 US2, T012).
 *
 * Source-text inspection: verifies the page exposes the executor picker,
 * date range, «only overdue» toggle, and a reset action — without
 * mounting the full layout (which would require Pinia + axios + i18n).
 */
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/pages/production/ProductionTasksBoardPage.vue'),
  'utf8',
)

describe('ProductionTasksBoardPage filters (US2 T012)', () => {
  it('mounts the executor picker bound to executorFilter ref', () => {
    expect(SOURCE).toContain("import ProductionTaskAssigneePicker from '@/components/domain/ProductionTaskAssigneePicker.vue'")
    expect(SOURCE).toContain('<ProductionTaskAssigneePicker v-model="executorFilter"')
    expect(SOURCE).toContain("executorFilter = ref<string | null>(null)")
  })

  it('exposes planned-finish from/to date inputs', () => {
    expect(SOURCE).toContain('id="board-due-from"')
    expect(SOURCE).toContain('id="board-due-to"')
    expect(SOURCE).toContain('v-model="dueDateFrom"')
    expect(SOURCE).toContain('v-model="dueDateTo"')
  })

  it('exposes the «только просроченные» checkbox bound to overdueOnly', () => {
    expect(SOURCE).toContain('Только просроченные')
    expect(SOURCE).toContain('v-model="overdueOnly"')
  })

  it('refetches when any non-search filter changes (no debounce on those)', () => {
    expect(SOURCE).toContain('watch([executorFilter, dueDateFrom, dueDateTo, overdueOnly]')
    expect(SOURCE).toContain('void refetch(filters.value)')
  })

  it('passes overdueOnly through to the composable filters', () => {
    expect(SOURCE).toContain('overdueOnly: overdueOnly.value || undefined')
  })

  it('renders a reset-filters control when any filter is active', () => {
    expect(SOURCE).toContain('Сбросить фильтры')
    expect(SOURCE).toContain('executorFilter || dueDateFrom || dueDateTo || overdueOnly || searchInput')
  })
})
