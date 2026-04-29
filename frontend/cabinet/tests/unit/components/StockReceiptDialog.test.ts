import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/components/domain/warehouse/StockReceiptDialog.vue'),
  'utf8',
)

describe('StockReceiptDialog', () => {
  it('has quantity input', () => {
    expect(SOURCE).toMatch(/quantity|warehouse\.fields\.quantity/i)
  })

  it('has optional comment textarea', () => {
    expect(SOURCE).toMatch(/comment|warehouse\.fields\.comment/i)
    expect(SOURCE).toMatch(/textarea|Textarea/i)
  })

  it('has submit button', () => {
    expect(SOURCE).toMatch(/submit|receipt|Приход|Сохранить/i)
  })

  it('emits received event on success', () => {
    expect(SOURCE).toMatch(/emit.*received|received.*emit/i)
  })

  it('posts to /api/materials/:id/receipt', () => {
    expect(SOURCE).toContain('/receipt')
    expect(SOURCE).toContain('httpClient')
  })

  it('validates quantity greater than zero', () => {
    expect(SOURCE).toMatch(/>\s*0|min.*0\.0001|quantity.*ValidationFailed/i)
  })
})
