import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/components/domain/orders/BomLineDialog.vue'),
  'utf8',
)

describe('BomLineDialog', () => {
  it('contains material selector and quantity input', () => {
    expect(SOURCE).toContain('warehouse.fields.name')
    expect(SOURCE).toContain('Select')
    expect(SOURCE).toContain('warehouse.fields.quantity')
    expect(SOURCE).toContain('type="number"')
  })

  it('keeps footer actions outside form via form attribute', () => {
    expect(SOURCE).toContain('id="bom-line-form"')
    expect(SOURCE).toContain('form="bom-line-form"')
  })

  it('validates positive quantity before save', () => {
    expect(SOURCE).toContain('quantity.value === undefined')
    expect(SOURCE).toContain('quantity.value <= 0')
    expect(SOURCE).toContain('bom.quantityPositive')
  })

  it('emits saved payload on submit', () => {
    expect(SOURCE).toContain("emit('saved'")
  })
})
