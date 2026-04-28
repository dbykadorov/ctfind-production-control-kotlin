/**
 * Production tasks board page (Feature 006 US1, T006).
 *
 * Verifies the page wires the board composable through data/loading/error
 * branches, renders four columns in fixed order, embeds RouterLink to the
 * detail page, surfaces the truncation banner and the forbidden empty
 * state, and exposes a refresh button.
 */
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/pages/production/ProductionTasksBoardPage.vue'),
  'utf8',
)

describe('ProductionTasksBoardPage (US1 T006)', () => {
  it('uses the board composable for data, loading, error, forbidden, refetch', () => {
    expect(SOURCE).toContain('useProductionTasksBoard()')
    expect(SOURCE).toContain('const { data, loading, error, forbidden, refetch }')
  })

  it('imports and renders ProductionTaskBoardCard inside RouterLink to detail', () => {
    expect(SOURCE).toContain("import ProductionTaskBoardCard from '@/components/domain/ProductionTaskBoardCard.vue'")
    expect(SOURCE).toContain("name: 'production-tasks.detail'")
    expect(SOURCE).toContain('<ProductionTaskBoardCard :row="row"')
  })

  it('renders all four status columns in fixed left-to-right order', () => {
    const idx = (status: string) => SOURCE.indexOf(`status: '${status}'`)
    const notStarted = idx('NOT_STARTED')
    const inProgress = idx('IN_PROGRESS')
    const blocked = idx('BLOCKED')
    const completed = idx('COMPLETED')
    expect(notStarted).toBeGreaterThan(-1)
    expect(inProgress).toBeGreaterThan(notStarted)
    expect(blocked).toBeGreaterThan(inProgress)
    expect(completed).toBeGreaterThan(blocked)
  })

  it('uses Russian labels for the four columns', () => {
    expect(SOURCE).toContain("'Не начато'")
    expect(SOURCE).toContain("'В работе'")
    expect(SOURCE).toContain("'Заблокировано'")
    expect(SOURCE).toContain("'Выполнено'")
  })

  it('shows skeleton placeholders when loading and data is null', () => {
    expect(SOURCE).toContain('loading && !data')
    expect(SOURCE).toContain('Skeleton')
  })

  it('shows the forbidden empty state with link back to the list', () => {
    expect(SOURCE).toContain('v-if="forbidden"')
    expect(SOURCE).toContain('Нет доступа к доске задач')
    expect(SOURCE).toContain("name: 'production-tasks.list'")
  })

  it('shows the truncation banner when data.truncated is true', () => {
    expect(SOURCE).toContain('data?.truncated')
    expect(SOURCE).toContain('Показаны первые 200 задач — уточните фильтры')
  })

  it('shows generic error banner when error.value is set', () => {
    expect(SOURCE).toContain('v-else-if="error"')
    expect(SOURCE).toContain('error.message')
  })

  it('exposes a manual refresh button that calls refetch(filters)', () => {
    expect(SOURCE).toContain('refetch(filters)')
    expect(SOURCE).toContain("t('common.refresh')")
  })

  it('renders a per-column empty state when the column is empty', () => {
    expect(SOURCE).toContain('byStatus[column.status].length === 0')
    expect(SOURCE).toContain("t('common.empty')")
  })

  it('uses a tablet-friendly horizontal scroll container for columns below the lg breakpoint', () => {
    expect(SOURCE).toContain('overflow-x-auto')
    expect(SOURCE).toContain('lg:overflow-visible')
    expect(SOURCE).toContain('min-w-[18rem]')
  })

  it('refetches on mount with the active filters', () => {
    expect(SOURCE).toContain('onMounted')
    expect(SOURCE).toContain('refetch(filters.value)')
  })
})
