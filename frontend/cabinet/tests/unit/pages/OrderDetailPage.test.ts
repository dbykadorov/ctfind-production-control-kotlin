import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/pages/office/OrderDetailPage.vue'),
  'utf8',
)

describe('OrderDetailPage inventory extensions', () => {
  it('imports and renders BOM and usage sections', () => {
    expect(SOURCE).toContain("import BomSection from '@/components/domain/orders/BomSection.vue'")
    expect(SOURCE).toContain("import MaterialUsageSection from '@/components/domain/orders/MaterialUsageSection.vue'")
    expect(SOURCE).toContain('<BomSection')
    expect(SOURCE).toContain('<MaterialUsageSection')
  })

  it('shows inventory sections only when user can view BOM', () => {
    expect(SOURCE).toContain('permissions.canViewOrderBom')
  })

  it('passes order id and shipped flag to inventory sections', () => {
    expect(SOURCE).toContain(':order-id="order.name"')
    expect(SOURCE).toContain("order.status === 'отгружен'")
  })

  it('synchronizes BOM and usage refresh via revision counter', () => {
    expect(SOURCE).toContain('orderMaterialsRevision')
    expect(SOURCE).toContain('@changed="onBomChanged"')
    expect(SOURCE).toContain('@consumed="onMaterialConsumed"')
  })
})
