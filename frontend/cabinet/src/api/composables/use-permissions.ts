/**
 * Composable, рассчитывающий `PermissionFlags` из текущей сессии.
 * Источник истины — `auth` store, который инициализируется из `readBoot()`.
 *
 * Соответствие ролям из 003-auth-rbac (см. data-model.md §3.1, plan.md §Constitution).
 */

import type { PermissionFlags } from '@/api/types/domain'
import { computed, type ComputedRef } from 'vue'
import { ADMIN_USER_LOGIN, BACKEND_ROLE_CODES, LEGACY_ROLE_LABELS } from '@/api/roles'
import { useAuthStore } from '@/stores/auth'

export function usePermissions(): ComputedRef<PermissionFlags> {
  const auth = useAuthStore()
  return computed<PermissionFlags>(() => buildPermissions(auth.user, auth.roles))
}

export function buildPermissions(user: string | null, roles: readonly string[]): PermissionFlags {
  const set = new Set(roles)
  const isAdmin = user === ADMIN_USER_LOGIN
    || hasRole(set, LEGACY_ROLE_LABELS.systemManager, LEGACY_ROLE_LABELS.administrator, BACKEND_ROLE_CODES.admin)
  const isOrderManager = hasRole(set, LEGACY_ROLE_LABELS.orderManager, BACKEND_ROLE_CODES.orderManager)
  const isShopSupervisor = hasRole(set, LEGACY_ROLE_LABELS.shopSupervisor, BACKEND_ROLE_CODES.productionSupervisor)
  const isExecutor = hasRole(set, LEGACY_ROLE_LABELS.executor, BACKEND_ROLE_CODES.productionExecutor)
  const isWarehouse = hasRole(set, LEGACY_ROLE_LABELS.warehouse, BACKEND_ROLE_CODES.warehouse)
  const isOrderCorrector = set.has(LEGACY_ROLE_LABELS.orderCorrector) || isAdmin

  // На MVP «admin correction» доступна только Administrator/System Manager (R-019, FR-022).
  const hasOrderCorrection = isAdmin

  const canManageOrders = isAdmin || isOrderManager
  const canManageCustomers = isAdmin || isOrderManager
  const canCreateProductionTasks = isAdmin || isOrderManager
  const canAssignProductionTasks = isAdmin || isOrderManager || isShopSupervisor
  const canViewAllProductionTasks = isAdmin || isOrderManager || isShopSupervisor
  const canUpdateAnyProductionTaskStatus = isAdmin || isShopSupervisor
  const canWorkAssignedProductionTasks = isExecutor
  const canEditOrderBom = isAdmin || isOrderManager
  const canViewOrderBom = isAdmin || isOrderManager || isWarehouse || isShopSupervisor || isOrderCorrector
  const canConsumeStock = isAdmin || isWarehouse

  return {
    isAdmin,
    isOrderManager,
    isShopSupervisor,
    isExecutor,
    isWarehouse,
    isOrderCorrector,
    canSeeCabinetWorkArea: isAdmin || isOrderManager || isShopSupervisor || isExecutor || isWarehouse,
    hasOrderCorrection,
    canManageOrders,
    canManageCustomers,
    canCreateProductionTasks,
    canAssignProductionTasks,
    canViewAllProductionTasks,
    canUpdateAnyProductionTaskStatus,
    canWorkAssignedProductionTasks,
    canEditOrderBom,
    canViewOrderBom,
    canConsumeStock,
  }
}

export function hasAnyRole(roles: readonly string[], allowed: readonly string[]): boolean {
  if (allowed.length === 0)
    return true
  const set = new Set(roles)
  return allowed.some(r => set.has(r))
}

function hasRole(roleSet: ReadonlySet<string>, ...allowed: string[]): boolean {
  return allowed.some(role => roleSet.has(role))
}
