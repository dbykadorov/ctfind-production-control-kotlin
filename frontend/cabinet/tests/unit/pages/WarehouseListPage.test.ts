import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/pages/warehouse/WarehouseListPage.vue'),
  'utf8',
)

describe('WarehouseListPage', () => {
  it('imports and uses useMaterials composable', () => {
    expect(SOURCE).toContain('useMaterials')
  })

  it('uses usePermissions', () => {
    expect(SOURCE).toContain('usePermissions')
  })

  it('renders table with material columns: name, unit, currentStock', () => {
    expect(SOURCE).toMatch(/name|warehouse\.fields\.name/i)
    expect(SOURCE).toMatch(/unit|warehouse\.fields\.unit/i)
    expect(SOURCE).toMatch(/currentStock|warehouse\.fields\.currentStock|warehouse\.currentStock/i)
  })

  it('has search input', () => {
    expect(SOURCE).toMatch(/search|поиск/i)
  })

  it('has add material button', () => {
    expect(SOURCE).toMatch(/addMaterial|warehouse\.addMaterial|Добавить материал/i)
  })

  it('renders with pagination controls', () => {
    expect(SOURCE).toMatch(/prevPage|nextPage|prev|next/i)
    expect(SOURCE).toMatch(/totalPages/i)
  })

  it('has receipt button per row', () => {
    expect(SOURCE).toMatch(/receipt|warehouse\.receipt|Приход/i)
  })

  it('has loading skeleton', () => {
    expect(SOURCE).toContain('Skeleton')
    expect(SOURCE).toContain('loading')
  })

  it('has empty state', () => {
    expect(SOURCE).toMatch(/emptyMaterials|warehouse\.emptyMaterials|empty/i)
  })

  it('imports MaterialCreateDialog', () => {
    expect(SOURCE).toContain('MaterialCreateDialog')
  })
})
