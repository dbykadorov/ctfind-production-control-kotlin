/**
 * Доменные типы Кабинета, перенесённые из первой версии модели данных.
 * См. specs/006-spa-cabinet-ui/data-model.md §2.2.
 *
 * ВАЖНО: UI по-прежнему работает с русскими статусами; Spring API использует
 * стабильные enum-коды, которые мапятся в composables.
 */

import type { CustomerOrder, CustomerOrderItem, CustomerOrderStatusChange } from './legacy.generated'

export type OrderStatus = 'новый' | 'в работе' | 'готов' | 'отгружен'

/** Сериализованный snapshot для `window.__BOOT__`. */
export interface BootPayload {
  user: string
  roles: string[]
  language: string
  csrfToken: string
  siteName: string
  deskUrl: string
  cabinetVersion: string
}

/** Item в списке заказов: проекция Customer Order + join customer.customer_name. */
export interface OrderListItem {
  name: string
  status: OrderStatus
  delivery_date: string
  modified: string
  creation: string
  customer: string
  customer_name?: string
  created_by_staff?: string
}

/** Фильтры списка заказов в UI. Сохраняются в `ui` store + URL-query. */
export interface OrderFilters {
  status?: OrderStatus
  customer?: string
  dateFrom?: string
  dateTo?: string
  search?: string
  /** 007: «Все активные» — статус ≠ отгружен (deeplink из KPI-карточек). */
  activeOnly?: boolean
  /** 007: «Только просроченные» — delivery_date < сегодня И статус ≠ отгружен. */
  overdue?: boolean
}

/**
 * Editability state, рассчитываемый на клиенте на основе статуса и ролей.
 * Серверная инфорсмент-логика — единственный источник истины (см. orders.py
 * `enforce_status_based_editability`); это лишь UX-зеркало (FR-018, FR-019).
 */
export interface OrderEditability {
  canEdit: boolean
  readonly: boolean
  frozen: string[]
  reason: 'shipped' | 'after-new' | 'admin-correction' | 'none'
  hint?: string
}

/** Workflow-переход, доступный текущему пользователю. */
export interface OrderTransition {
  action: string
  state: string
  next_state: string
  allowed: string
  allow_self_approval?: 0 | 1
}

/** Запись истории — объединённый Version + Customer Order Status Change. */
export type TimelineEntryKind = 'edit' | 'status'

export interface TimelineEntry {
  id: string
  kind: TimelineEntryKind
  at: string
  actor: string
  actor_label?: string
  via_admin_correction?: boolean
  details: ParsedDiff[]
}

/** Разбор Version.data (JSON-строка с changed/added/removed). */
export interface ParsedDiff {
  fieldname: string
  field_label?: string
  from_value?: unknown
  to_value?: unknown
}

export type ApiErrorKind =
  | 'validation'
  | 'permission'
  | 'conflict'
  | 'network'
  | 'server'
  | 'session-expired'
  | 'unknown'

export interface ApiError {
  kind: ApiErrorKind
  message: string
  /** Backend exception code, when provided. */
  excType?: string
  /** HTTP-статус ответа. */
  status?: number
  /** Поле формы, к которому относится ошибка валидации. */
  field?: string
  /** Полный сырой ответ сервера для логирования. */
  raw?: unknown
}

/**
 * Permission flags, рассчитанные из `auth.roles`. Соответствуют ролям из 003-auth-rbac.
 * См. specs/006-spa-cabinet-ui/data-model.md §3.1.
 */
export interface PermissionFlags {
  isAdmin: boolean
  isOrderManager: boolean
  isShopSupervisor: boolean
  isExecutor: boolean
  isWarehouse: boolean
  isOrderCorrector: boolean
  /** Имеет ли пользователь хоть одну роль, для которой Кабинет показывает рабочую область. */
  canSeeCabinetWorkArea: boolean
  /** Может ли отменить freeze-логику (admin correction). На MVP = isAdmin. */
  hasOrderCorrection: boolean
  /** Может ли создавать/редактировать заказы. */
  canManageOrders: boolean
  /** Может ли создавать/редактировать клиентов. */
  canManageCustomers: boolean
  /** Может ли создавать производственные задачи из заказов. */
  canCreateProductionTasks: boolean
  /** Может ли назначать исполнителя и плановые даты производственной задачи. */
  canAssignProductionTasks: boolean
  /** Может ли видеть все производственные задачи. */
  canViewAllProductionTasks: boolean
  /** Может ли менять статус любой производственной задачи. */
  canUpdateAnyProductionTaskStatus: boolean
  /** Может ли исполнитель работать со своими назначенными задачами. */
  canWorkAssignedProductionTasks: boolean
}

/**
 * Generic UI state machine для экранов с асинхронной загрузкой.
 * См. data-model.md §4.
 */
export type ScreenState =
  | 'idle'
  | 'loading'
  | 'loaded'
  | 'empty'
  | 'saving'
  | 'saved'
  | 'conflict'
  | 'error'

export type { CustomerOrder, CustomerOrderItem, CustomerOrderStatusChange }
export type { OrderDetailResponse, OrderListRowResponse, OrdersPageResponse } from './orders'
