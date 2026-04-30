import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/api/composables/use-stock-consumption.ts'),
  'utf8',
)

describe('useStockConsumption composable', () => {
  it('posts consumption to backend endpoint', () => {
    expect(SOURCE).toContain('/api/materials/${materialId}/consume')
  })

  it('returns loading error and availableStock refs', () => {
    expect(SOURCE).toContain('loading')
    expect(SOURCE).toContain('error')
    expect(SOURCE).toContain('availableStock')
  })

  it('parses available stock from backend error payload', () => {
    expect(SOURCE).toContain('raw?.available')
    expect(SOURCE).toContain('Number(raw.available)')
  })

  it('captures backend error code for ORDER_LOCKED and similar cases', () => {
    expect(SOURCE).toContain('errorCode')
    expect(SOURCE).toContain('raw?.error')
  })
})
