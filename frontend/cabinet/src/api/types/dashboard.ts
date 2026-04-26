/**
 * Клиент-only DTO для виджетов дашборда (007-cabinet-dashboard-theme).
 *
 * Никаких новых серверных сущностей — все типы строятся на основе уже существующих
 * `Customer Order` и `Customer Order Status Change` (см. data-model.md §3).
 */

import type { ApiError, OrderStatus } from './domain'

/** KPI-карточки на дашборде (4 счётчика). */
export interface DashboardKpis {
  totalActive: number
  inProgress: number
  ready: number
  overdue: number
}

/** Один день для линейного графика «Динамика заказов». */
export interface OrderTrendPoint {
  date: string
  count: number
}

/** Серия для главного графика + delta vs предыдущий период. */
export interface OrderTrendSeries {
  points: OrderTrendPoint[]
  totalLast30: number
  totalPrev30: number
  delta30vsPrev30Pct: number | null
}

/** Запись в donut «Распределение по статусам». */
export interface StatusDistributionEntry {
  status: OrderStatus
  count: number
  percent: number
}

/** Строка в виджете «Последние изменения статусов». */
export interface RecentStatusChange {
  name: string
  order: string
  fromStatus: OrderStatus
  toStatus: OrderStatus
  actorUser: string
  eventAt: string
}

/** Универсальный async-result-обёртка для дашборд-композаблов. */
export interface DashboardAsyncState<T> {
  data: T | null
  loading: boolean
  error: ApiError | null
}
