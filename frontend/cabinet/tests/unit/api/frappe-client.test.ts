/**
 * Unit-тесты для frappe-client / serializeForFrappe.
 *
 * Регрессия: `frappe.client.get_count` через GET с `filters: Array<[string,string,unknown]>`
 * падал с `TypeError: DatabaseQuery.execute() got an unexpected keyword argument 'filters[0][0]'`,
 * потому что дефолтный axios `paramsSerializer` сериализует массивы в bracket-notation.
 *
 * После фикса все нескалярные значения (массивы / объекты) уходят в query как одна
 * JSON-строка под исходным ключом — формат, который Frappe умеет распарсить.
 */

import { describe, expect, it } from 'vitest'
import { serializeForFrappe } from '@/api/frappe-client'

function asObject(p: URLSearchParams): Record<string, string[]> {
  const out: Record<string, string[]> = {}
  for (const [k, v] of p.entries()) {
    out[k] = out[k] ?? []
    out[k].push(v)
  }
  return out
}

describe('serializeForFrappe', () => {
  it('скалярные значения проходят как есть', () => {
    const result = asObject(serializeForFrappe({
      doctype: 'Customer Order',
      limit: 20,
      asc: true,
    }))
    expect(result).toEqual({
      doctype: ['Customer Order'],
      limit: ['20'],
      asc: ['true'],
    })
  })

  it('массивы (включая вложенные) сериализуются одной JSON-строкой под исходным ключом', () => {
    const filters = [['status', '=', 'Active'], ['delivery_date', '<', '2026-01-01']]
    const result = asObject(serializeForFrappe({
      doctype: 'Customer Order',
      filters,
    }))
    expect(result.doctype).toEqual(['Customer Order'])
    const filtersValues = result.filters ?? []
    expect(filtersValues).toHaveLength(1)
    expect(JSON.parse(filtersValues[0] ?? '')).toEqual(filters)
    // Регрессия: bracket-notation не должна появляться.
    expect(Object.keys(result).some(k => k.startsWith('filters['))).toBe(false)
  })

  it('объекты сериализуются как JSON-строка', () => {
    const result = asObject(serializeForFrappe({
      fields: ['name', 'status'],
      order_by: 'creation desc',
      payload: { customer: 'C-001', priority: 2 },
    }))
    expect(JSON.parse(result.fields?.[0] ?? '')).toEqual(['name', 'status'])
    expect(JSON.parse(result.payload?.[0] ?? '')).toEqual({ customer: 'C-001', priority: 2 })
    expect(result.order_by).toEqual(['creation desc'])
  })

  it('null и undefined игнорируются (Frappe их не ждёт в query)', () => {
    const result = asObject(serializeForFrappe({
      a: 1,
      b: null,
      c: undefined,
      d: 'x',
    }))
    expect(result).toEqual({ a: ['1'], d: ['x'] })
  })

  it('boolean конвертируются в строки "true"/"false"', () => {
    const result = asObject(serializeForFrappe({
      enabled: true,
      disabled: false,
    }))
    expect(result).toEqual({ enabled: ['true'], disabled: ['false'] })
  })
})
