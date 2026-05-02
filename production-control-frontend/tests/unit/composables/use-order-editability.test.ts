/**
 * Unit-тесты для use-order-editability.ts (T065 / US1).
 * Матрица «статус × роль» — единственный источник правды на клиенте (см.
 * data-model.md §4.2 и orders.py::enforce_status_based_editability).
 */

import type { CustomerOrder, OrderStatus, PermissionFlags } from '@/api/types/domain'
import { describe, expect, it } from 'vitest'
import { computeEditability } from '@/api/composables/use-order-editability'
import { buildPermissions } from '@/api/composables/use-permissions'

function makeOrder(status: OrderStatus): CustomerOrder {
  return {
    name: 'CO-0001',
    creation: '2026-04-10 10:00:00',
    modified: '2026-04-10 10:00:00',
    owner: 'om@example.com',
    modified_by: 'om@example.com',
    docstatus: 0,
    customer: 'CUST-1',
    delivery_date: '2026-05-01',
    status,
    items: [],
  }
}

function p(roles: string[], user = 'u@example.com'): PermissionFlags {
  return buildPermissions(user, roles)
}

describe('computeEditability — Order Manager', () => {
  const om = p(['Order Manager'])

  it('новый: всё доступно', () => {
    const e = computeEditability(makeOrder('новый'), om, false)
    expect(e.canEdit).toBe(true)
    expect(e.readonly).toBe(false)
    expect(e.frozen).toEqual([])
    expect(e.reason).toBe('none')
  })

  it('в работе: items и delivery_date заморожены', () => {
    const e = computeEditability(makeOrder('в работе'), om, false)
    expect(e.canEdit).toBe(true)
    expect(e.readonly).toBe(false)
    expect(e.frozen).toEqual(['items', 'delivery_date'])
    expect(e.reason).toBe('after-new')
    expect(e.hint).toContain('в работе')
  })

  it('готов: items и delivery_date заморожены', () => {
    const e = computeEditability(makeOrder('готов'), om, false)
    expect(e.frozen).toEqual(['items', 'delivery_date'])
    expect(e.reason).toBe('after-new')
  })

  it('отгружен: всё readonly', () => {
    const e = computeEditability(makeOrder('отгружен'), om, false)
    expect(e.canEdit).toBe(false)
    expect(e.readonly).toBe(true)
    expect(e.frozen).toEqual(['*'])
    expect(e.reason).toBe('shipped')
  })
})

describe('computeEditability — Shop Supervisor', () => {
  const ss = p(['Shop Supervisor'])

  it.each<OrderStatus>(['новый', 'в работе', 'готов', 'отгружен'])(
    'статус %s: всегда readonly без правок',
    (status) => {
      const e = computeEditability(makeOrder(status), ss, false)
      expect(e.canEdit).toBe(false)
      expect(e.readonly).toBe(true)
      expect(e.frozen).toEqual(['*'])
    },
  )
})

describe('computeEditability — Administrator', () => {
  const admin = p([], 'Administrator')

  it('в работе без admin-mode: ведёт себя как обычный менеджер с заморозкой', () => {
    const e = computeEditability(makeOrder('в работе'), admin, false)
    expect(e.canEdit).toBe(true)
    expect(e.frozen).toEqual(['items', 'delivery_date'])
    expect(e.reason).toBe('after-new')
  })

  it('в работе с admin-mode: всё разрешено, reason admin-correction', () => {
    const e = computeEditability(makeOrder('в работе'), admin, true)
    expect(e.canEdit).toBe(true)
    expect(e.frozen).toEqual([])
    expect(e.reason).toBe('admin-correction')
    expect(e.hint).toContain('корректировк')
  })

  it('отгружен с admin-mode: разрешено редактировать', () => {
    const e = computeEditability(makeOrder('отгружен'), admin, true)
    expect(e.canEdit).toBe(true)
    expect(e.readonly).toBe(false)
    expect(e.reason).toBe('admin-correction')
  })

  it('отгружен без admin-mode: всё readonly даже у админа', () => {
    const e = computeEditability(makeOrder('отгружен'), admin, false)
    expect(e.canEdit).toBe(false)
    expect(e.readonly).toBe(true)
  })
})

describe('computeEditability — Executor / Warehouse', () => {
  it('executor: нет прав на редактирование', () => {
    const e = computeEditability(makeOrder('новый'), p(['Executor']), false)
    expect(e.canEdit).toBe(false)
    expect(e.readonly).toBe(true)
    expect(e.frozen).toEqual(['*'])
  })

  it('warehouse: нет прав на редактирование', () => {
    const e = computeEditability(makeOrder('готов'), p(['Warehouse']), false)
    expect(e.canEdit).toBe(false)
    expect(e.readonly).toBe(true)
  })
})

describe('computeEditability — без заказа', () => {
  it('null order: readonly, ничего нельзя', () => {
    const e = computeEditability(null, p(['Order Manager']), false)
    expect(e.canEdit).toBe(false)
    expect(e.readonly).toBe(true)
    expect(e.frozen).toEqual(['*'])
  })
})
