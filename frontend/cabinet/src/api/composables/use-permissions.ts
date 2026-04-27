/**
 * Composable, рассчитывающий `PermissionFlags` из текущей сессии.
 * Источник истины — `auth` store, который инициализируется из `readBoot()`.
 *
 * Соответствие ролям из 003-auth-rbac (см. data-model.md §3.1, plan.md §Constitution).
 */

import type { PermissionFlags } from '@/api/types/domain'
import { computed, type ComputedRef } from 'vue'
import { useAuthStore } from '@/stores/auth'

const ROLE_ORDER_MANAGER = 'Order Manager'
const ROLE_SHOP_SUPERVISOR = 'Shop Supervisor'
const ROLE_EXECUTOR = 'Executor'
const ROLE_WAREHOUSE = 'Warehouse'
const ROLE_ORDER_CORRECTOR = 'Order Corrector'
const ROLE_SYSTEM_MANAGER = 'System Manager'
const ROLE_ADMINISTRATOR = 'Administrator'
const ROLE_BACKEND_ADMIN = 'ADMIN'
const ROLE_BACKEND_ORDER_MANAGER = 'ORDER_MANAGER'
const ROLE_BACKEND_PRODUCTION_SUPERVISOR = 'PRODUCTION_SUPERVISOR'
const ROLE_BACKEND_PRODUCTION_EXECUTOR = 'PRODUCTION_EXECUTOR'
const ADMIN_USER = 'Administrator'

export function usePermissions(): ComputedRef<PermissionFlags> {
  const auth = useAuthStore()
  return computed<PermissionFlags>(() => buildPermissions(auth.user, auth.roles))
}

export function buildPermissions(user: string | null, roles: readonly string[]): PermissionFlags {
  const set = new Set(roles)
  const isAdmin = user === ADMIN_USER
    || set.has(ROLE_SYSTEM_MANAGER)
    || set.has(ROLE_ADMINISTRATOR)
    || set.has(ROLE_BACKEND_ADMIN)
  const isOrderManager = set.has(ROLE_ORDER_MANAGER) || set.has(ROLE_BACKEND_ORDER_MANAGER)
  const isShopSupervisor = set.has(ROLE_SHOP_SUPERVISOR) || set.has(ROLE_BACKEND_PRODUCTION_SUPERVISOR)
  const isExecutor = set.has(ROLE_EXECUTOR) || set.has(ROLE_BACKEND_PRODUCTION_EXECUTOR)
  const isWarehouse = set.has(ROLE_WAREHOUSE)
  const isOrderCorrector = set.has(ROLE_ORDER_CORRECTOR) || isAdmin

  // На MVP «admin correction» доступна только Administrator/System Manager (R-019, FR-022).
  const hasOrderCorrection = isAdmin

  const canManageOrders = isAdmin || isOrderManager
  const canManageCustomers = isAdmin || isOrderManager
  const canCreateProductionTasks = isAdmin || isOrderManager
  const canAssignProductionTasks = isAdmin || isOrderManager || isShopSupervisor
  const canViewAllProductionTasks = isAdmin || isOrderManager || isShopSupervisor
  const canUpdateAnyProductionTaskStatus = isAdmin || isShopSupervisor
  const canWorkAssignedProductionTasks = isExecutor

  return {
    isAdmin,
    isOrderManager,
    isShopSupervisor,
    isExecutor,
    isWarehouse,
    isOrderCorrector,
    canSeeCabinetWorkArea: isAdmin || isOrderManager || isShopSupervisor || isExecutor,
    hasOrderCorrection,
    canManageOrders,
    canManageCustomers,
    canCreateProductionTasks,
    canAssignProductionTasks,
    canViewAllProductionTasks,
    canUpdateAnyProductionTaskStatus,
    canWorkAssignedProductionTasks,
  }
}

export function hasAnyRole(roles: readonly string[], allowed: readonly string[]): boolean {
  if (allowed.length === 0)
    return true
  const set = new Set(roles)
  return allowed.some(r => set.has(r))
}
