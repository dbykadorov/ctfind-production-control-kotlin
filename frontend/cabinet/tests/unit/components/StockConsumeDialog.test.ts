import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/components/domain/warehouse/StockConsumeDialog.vue'),
  'utf8',
)

describe('StockConsumeDialog', () => {
  it('supports order and material selection', () => {
    expect(SOURCE).toContain('consume.pickOrder')
    expect(SOURCE).toContain('consume.pickMaterial')
    expect(SOURCE).toContain('selectedOrderId')
    expect(SOURCE).toContain('selectedMaterialId')
  })

  it('uses active order search and stock consumption composables', () => {
    expect(SOURCE).toContain('useActiveOrders')
    expect(SOURCE).toContain('useStockConsumption')
  })

  it('shows overconsumption warning', () => {
    expect(SOURCE).toContain('overconsumption')
    expect(SOURCE).toContain('consume.overconsumption')
  })

  it('posts consume request and emits consumed event', () => {
    expect(SOURCE).toContain("emit('consumed')")
    expect(SOURCE).toContain('consume(')
  })
})
