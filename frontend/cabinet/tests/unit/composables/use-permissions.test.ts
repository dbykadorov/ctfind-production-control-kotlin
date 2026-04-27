import { describe, expect, it } from 'vitest'
import { buildPermissions, hasAnyRole } from '@/api/composables/use-permissions'

describe('buildPermissions', () => {
  it('administrator получает все рабочие флаги, включая admin-correction', () => {
    const p = buildPermissions('Administrator', [])
    expect(p.isAdmin).toBe(true)
    expect(p.canManageOrders).toBe(true)
    expect(p.canManageCustomers).toBe(true)
    expect(p.hasOrderCorrection).toBe(true)
    expect(p.canSeeCabinetWorkArea).toBe(true)
  })

  it('system Manager эквивалентен Administrator по правам Кабинета', () => {
    const p = buildPermissions('user@x', ['System Manager'])
    expect(p.isAdmin).toBe(true)
    expect(p.hasOrderCorrection).toBe(true)
  })

  it('order Manager управляет заказами и клиентами, но не имеет admin-correction (FR-022)', () => {
    const p = buildPermissions('user@x', ['Order Manager'])
    expect(p.isOrderManager).toBe(true)
    expect(p.canManageOrders).toBe(true)
    expect(p.canManageCustomers).toBe(true)
    expect(p.hasOrderCorrection).toBe(false)
    expect(p.canSeeCabinetWorkArea).toBe(true)
  })

  it('shop Supervisor — read-only, видит рабочую область, не управляет', () => {
    const p = buildPermissions('user@x', ['Shop Supervisor'])
    expect(p.isShopSupervisor).toBe(true)
    expect(p.canManageOrders).toBe(false)
    expect(p.canManageCustomers).toBe(false)
    expect(p.canSeeCabinetWorkArea).toBe(true)
  })

  it('executor sees cabinet work area for assigned production tasks but cannot manage orders', () => {
    const p = buildPermissions('user@x', ['Executor', 'Warehouse'])
    expect(p.isExecutor).toBe(true)
    expect(p.isWarehouse).toBe(true)
    expect(p.canManageOrders).toBe(false)
    expect(p.canSeeCabinetWorkArea).toBe(true)
    expect(p.canWorkAssignedProductionTasks).toBe(true)
    expect(p.canViewAllProductionTasks).toBe(false)
  })

  it('recognizes backend production role codes', () => {
    const supervisor = buildPermissions('user@x', ['PRODUCTION_SUPERVISOR'])
    expect(supervisor.isShopSupervisor).toBe(true)
    expect(supervisor.canAssignProductionTasks).toBe(true)
    expect(supervisor.canUpdateAnyProductionTaskStatus).toBe(true)

    const executor = buildPermissions('user@x', ['PRODUCTION_EXECUTOR'])
    expect(executor.isExecutor).toBe(true)
    expect(executor.canWorkAssignedProductionTasks).toBe(true)
    expect(executor.canUpdateAnyProductionTaskStatus).toBe(false)
  })

  it('order Corrector наследует право admin-correction только если совмещён с Admin', () => {
    const corrector = buildPermissions('user@x', ['Order Corrector'])
    expect(corrector.isOrderCorrector).toBe(true)
    expect(corrector.hasOrderCorrection).toBe(false)

    const adminCorrector = buildPermissions('Administrator', ['Order Corrector'])
    expect(adminCorrector.hasOrderCorrection).toBe(true)
  })
})

describe('hasAnyRole', () => {
  it('пустой allowed-список означает «доступ всем»', () => {
    expect(hasAnyRole([], [])).toBe(true)
  })

  it('возвращает true при пересечении', () => {
    expect(hasAnyRole(['Order Manager'], ['Order Manager', 'Shop Supervisor'])).toBe(true)
  })

  it('возвращает false без пересечения', () => {
    expect(hasAnyRole(['Executor'], ['Order Manager'])).toBe(false)
  })
})
