import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/api/composables/use-order-bom.ts'),
  'utf8',
)

describe('useOrderBom composable', () => {
  it('exposes list state and refetch', () => {
    expect(SOURCE).toContain('lines')
    expect(SOURCE).toContain('loading')
    expect(SOURCE).toContain('error')
    expect(SOURCE).toContain('refetch')
  })

  it('provides add update and remove operations', () => {
    expect(SOURCE).toContain('addLine')
    expect(SOURCE).toContain('updateLine')
    expect(SOURCE).toContain('removeLine')
  })

  it('uses /api/orders/{orderId}/bom endpoints', () => {
    expect(SOURCE).toContain('/api/orders/${orderId}/bom')
    expect(SOURCE).toContain('/api/orders/${orderId}/bom/${lineId}')
  })

  it('maps errors via toApiError', () => {
    expect(SOURCE).toContain('toApiError')
  })
})
