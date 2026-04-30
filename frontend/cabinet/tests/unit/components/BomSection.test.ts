import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/components/domain/orders/BomSection.vue'),
  'utf8',
)

describe('BomSection', () => {
  it('uses useOrderBom composable', () => {
    expect(SOURCE).toContain('useOrderBom')
  })

  it('renders BOM table columns and actions', () => {
    expect(SOURCE).toContain('usage.column.required')
    expect(SOURCE).toContain('warehouse.fields.comment')
    expect(SOURCE).toContain('common.actions')
    expect(SOURCE).toContain('bom.edit')
    expect(SOURCE).toContain('bom.delete')
  })

  it('opens BomLineDialog for create and edit', () => {
    expect(SOURCE).toContain('<BomLineDialog')
    expect(SOURCE).toContain('openCreate')
    expect(SOURCE).toContain('openEdit')
  })

  it('guards editing by permission and shipped status', () => {
    expect(SOURCE).toContain('permissions.value.canEditOrderBom')
    expect(SOURCE).toContain('props.orderShipped')
  })
})
