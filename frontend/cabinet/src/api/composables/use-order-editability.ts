/**
 * Расчёт UX-зеркала прав редактирования заказа на клиенте.
 * Серверная истина — `production_control/orders.py::enforce_status_based_editability`
 * (см. 005-orders). На клиенте дублируем для предотвращения попыток отправить заведомо
 * неудачный save и отрисовки disabled/hint'ов (FR-018, FR-019).
 *
 * Spec: data-model.md §4.2.
 */

import type {
  CustomerOrder,
  OrderEditability,
  PermissionFlags,
} from '@/api/types/domain'
import { computed, type ComputedRef, type Ref } from 'vue'

const FROZEN_AFTER_NEW = ['items', 'delivery_date']
const FROZEN_SHIPPED = ['*']

interface UseEditabilityOptions {
  /** Включён ли режим административной корректировки (US4). На MVP только Admin. */
  adminMode?: Ref<boolean>
}

export function computeEditability(
  order: CustomerOrder | null,
  permissions: PermissionFlags,
  adminMode: boolean,
): OrderEditability {
  if (!order) {
    return { canEdit: false, readonly: true, frozen: ['*'], reason: 'none' }
  }

  // Shop Supervisor — read-only независимо от статуса (US3).
  if (permissions.isShopSupervisor && !permissions.isAdmin) {
    return {
      canEdit: false,
      readonly: true,
      frozen: ['*'],
      reason: 'none',
      hint: 'Режим просмотра',
    }
  }

  if (!permissions.canManageOrders && !permissions.isOrderCorrector) {
    return {
      canEdit: false,
      readonly: true,
      frozen: ['*'],
      reason: 'none',
      hint: 'Нет прав на редактирование',
    }
  }

  const status = order.status
  const hasCorrection = permissions.hasOrderCorrection && adminMode

  if (status === 'отгружен') {
    if (hasCorrection) {
      return {
        canEdit: true,
        readonly: false,
        frozen: [],
        reason: 'admin-correction',
        hint: 'Режим административной корректировки активен',
      }
    }
    return {
      canEdit: false,
      readonly: true,
      frozen: FROZEN_SHIPPED,
      reason: 'shipped',
      hint: 'Заказ отгружен — редактирование недоступно',
    }
  }

  if (status !== 'новый') {
    if (hasCorrection) {
      return {
        canEdit: true,
        readonly: false,
        frozen: [],
        reason: 'admin-correction',
        hint: 'Режим административной корректировки активен',
      }
    }
    return {
      canEdit: true,
      readonly: false,
      frozen: FROZEN_AFTER_NEW,
      reason: 'after-new',
      hint: `Часть полей заморожена в статусе «${status}». Доступно роли Order Corrector.`,
    }
  }

  return { canEdit: true, readonly: false, frozen: [], reason: 'none' }
}

export function useOrderEditability(
  order: Ref<CustomerOrder | null>,
  permissions: ComputedRef<PermissionFlags>,
  options: UseEditabilityOptions = {},
): ComputedRef<OrderEditability> {
  const adminMode = options.adminMode
  return computed(() => computeEditability(order.value, permissions.value, adminMode?.value ?? false))
}

export const editabilityInternals = { FROZEN_AFTER_NEW, FROZEN_SHIPPED }
