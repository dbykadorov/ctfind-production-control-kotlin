import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/components/domain/orders/MaterialUsageSection.vue'),
  'utf8',
)

describe('MaterialUsageSection', () => {
  it('uses usage composable and renders usage columns', () => {
    expect(SOURCE).toContain('useOrderMaterialUsage')
    expect(SOURCE).toContain('usage.column.required')
    expect(SOURCE).toContain('usage.column.consumed')
    expect(SOURCE).toContain('usage.column.remaining')
  })

  it('shows overconsumption badge when value > 0', () => {
    expect(SOURCE).toContain('row.overconsumption > 0')
    expect(SOURCE).toContain('usage.overconsumption')
  })

  it('opens StockConsumeDialog for eligible users', () => {
    expect(SOURCE).toContain('permissions.canConsumeStock')
    expect(SOURCE).toContain('<StockConsumeDialog')
    expect(SOURCE).toContain('usage.consumeButton')
  })
})
