/**
 * Composables для работы с заказами.
 * Spring endpoints для заказов. UI сохраняет legacy-форму документа,
 * а этот слой мапит её в backend contracts.
 */

import type {
  ApiError,
  CustomerOrder,
  OrderFilters,
  OrderListItem,
  OrderStatus,
  ScreenState,
} from '@/api/types/domain'
import type {
  BackendOrderStatus,
  CreateOrderPayload,
  OrderDetailResponse,
  OrderListRowResponse,
  OrdersPageResponse,
  UpdateOrderPayload,
} from '@/api/types/orders'
import { onScopeDispose, ref, type Ref, shallowRef, type ShallowRef, watch } from 'vue'
import { httpClient } from '@/api/api-client'
import { subscribeDocUpdate, subscribeListUpdate } from '@/api/socket'
import { toApiError } from '@/utils/errors'

const ORDER_DOCTYPE = 'Customer Order'
const LIST_PAGE_SIZE = 50
const REALTIME_DEBOUNCE_MS = 500

interface UseOrdersListResult {
  data: ShallowRef<OrderListItem[]>
  total: Ref<number>
  state: Ref<ScreenState>
  error: Ref<ApiError | null>
  hasMore: Ref<boolean>
  reload: () => Promise<void>
  loadMore: () => Promise<void>
}

/**
 * 007: Опции виджета «Последние заказы» — позволяют переопределить страничный размер
 * и порядок сортировки, не нарушая существующего контракта (все поля опциональны).
 */
export interface UseOrdersListOptions {
  pageLength?: number
  orderBy?: { field: keyof OrderListItem | 'creation' | 'modified' | 'delivery_date', order: 'asc' | 'desc' }
}

function todayIsoDate(): string {
  return new Date().toISOString().slice(0, 10)
}

function buildFilterPayload(filters: OrderFilters): Record<string, unknown> {
  const f: Array<[string, string, unknown]> = []
  if (filters.status) {
    f.push(['status', '=', filters.status])
  }
  else if (filters.activeOnly || filters.overdue) {
    // 007: KPI-deeplink. «Все активные» / «Просроченные» оба исключают «отгружен».
    f.push(['status', '!=', 'отгружен'])
  }
  if (filters.overdue)
    f.push(['delivery_date', '<', todayIsoDate()])
  if (filters.customer)
    f.push(['customer', '=', filters.customer])
  if (filters.dateFrom)
    f.push(['delivery_date', '>=', filters.dateFrom])
  if (filters.dateTo)
    f.push(['delivery_date', '<=', filters.dateTo])
  return { filters: f }
}

function buildOrFilters(search: string | undefined): Record<string, unknown> {
  if (!search)
    return {}
  const like = `%${search}%`
  return {
    or_filters: [
      ['name', 'like', like],
      ['customer', 'like', like],
    ],
  }
}

const UI_TO_BACKEND_STATUS: Record<OrderStatus, BackendOrderStatus> = {
  'новый': 'NEW',
  'в работе': 'IN_WORK',
  'готов': 'READY',
  'отгружен': 'SHIPPED',
}

const BACKEND_TO_UI_STATUS: Record<BackendOrderStatus, OrderStatus> = {
  NEW: 'новый',
  IN_WORK: 'в работе',
  READY: 'готов',
  SHIPPED: 'отгружен',
}

type VersionedCustomerOrder = CustomerOrder & { version: number }

function buildOrderQueryParams(filters: OrderFilters, start: number, pageSize: number): Record<string, unknown> {
  return {
    ...(filters.search ? { search: filters.search } : {}),
    ...(filters.status ? { status: UI_TO_BACKEND_STATUS[filters.status] } : {}),
    ...(filters.customer ? { customerId: filters.customer } : {}),
    ...(filters.activeOnly ? { activeOnly: true } : {}),
    ...(filters.overdue ? { overdueOnly: true } : {}),
    ...(filters.dateFrom ? { deliveryDateFrom: filters.dateFrom } : {}),
    ...(filters.dateTo ? { deliveryDateTo: filters.dateTo } : {}),
    page: Math.floor(start / pageSize),
    size: pageSize,
  }
}

function mapOrderListRow(row: OrderListRowResponse): OrderListItem {
  return {
    name: row.id,
    status: row.statusLabel ?? BACKEND_TO_UI_STATUS[row.status],
    delivery_date: row.deliveryDate,
    modified: row.updatedAt,
    creation: row.createdAt ?? row.updatedAt,
    customer: row.customer.id,
    customer_name: row.customer.displayName,
    created_by_staff: undefined,
  }
}

function mapOrderDetail(row: OrderDetailResponse): CustomerOrder {
  const order: VersionedCustomerOrder = {
    name: row.id,
    owner: 'spring',
    creation: row.createdAt,
    modified: row.updatedAt,
    modified_by: 'spring',
    docstatus: 0,
    customer: row.customer.id,
    delivery_date: row.deliveryDate,
    status: row.statusLabel ?? BACKEND_TO_UI_STATUS[row.status],
    notes: row.notes,
    version: row.version,
    items: row.items.map(item => ({
      name: item.id,
      owner: 'spring',
      creation: row.createdAt,
      modified: row.updatedAt,
      modified_by: 'spring',
      docstatus: 0,
      parent: row.id,
      parenttype: ORDER_DOCTYPE,
      parentfield: 'items',
      idx: item.lineNo,
      item_name: item.itemName,
      quantity: item.quantity,
      uom: item.uom,
    })),
  }
  return order
}

function mapCreateOrderPayload(payload: Partial<CustomerOrder>): CreateOrderPayload {
  return {
    customerId: payload.customer ?? '',
    deliveryDate: payload.delivery_date ?? '',
    notes: payload.notes,
    items: (payload.items ?? []).map(item => ({
      itemName: item.item_name,
      quantity: Number(item.quantity),
      uom: item.uom,
    })),
  }
}

function mapUpdateOrderPayload(payload: Partial<CustomerOrder>, expectedVersion: number): UpdateOrderPayload {
  return {
    expectedVersion,
    ...mapCreateOrderPayload(payload),
  }
}

async function fetchOrdersListPage(
  filters: OrderFilters,
  start: number,
  pageSize: number,
  _signal?: AbortSignal,
  _orderBy?: UseOrdersListOptions['orderBy'],
): Promise<OrdersPageResponse> {
  const response = await httpClient.get<OrdersPageResponse>('/api/orders', {
    params: buildOrderQueryParams(filters, start, pageSize),
    signal: _signal,
  })
  return response.data
}

async function fetchCustomerNames(rows: OrderListItem[]): Promise<OrderListItem[]> {
  return rows
}

export function useOrdersList(filters: Ref<OrderFilters>, options: UseOrdersListOptions = {}): UseOrdersListResult {
  const pageSize = options.pageLength ?? LIST_PAGE_SIZE
  const data = shallowRef<OrderListItem[]>([])
  const state = ref<ScreenState>('idle')
  const error = ref<ApiError | null>(null)
  const total = ref(0)
  const hasMore = ref(false)
  let abortController: AbortController | null = null

  async function loadFrom(start: number, append: boolean): Promise<void> {
    abortController?.abort()
    abortController = new AbortController()
    state.value = append ? 'loading' : 'loading'
    error.value = null
    try {
      const response = await fetchOrdersListPage(
        filters.value,
        start,
        pageSize,
        abortController.signal,
        options.orderBy,
      )
      const more = response.page + 1 < response.totalPages
      const page = response.items.map(mapOrderListRow)
      const enriched = await fetchCustomerNames(page)
      data.value = append ? [...data.value, ...enriched] : enriched
      total.value = response.totalItems
      hasMore.value = more
      state.value = data.value.length === 0 ? 'empty' : 'loaded'
    }
    catch (e) {
      if ((e as { name?: string }).name === 'CanceledError')
        return
      error.value = toApiError(e)
      state.value = 'error'
    }
  }

  async function reload(): Promise<void> {
    await loadFrom(0, false)
  }

  async function loadMore(): Promise<void> {
    if (!hasMore.value)
      return
    await loadFrom(data.value.length, true)
  }

  watch(filters, () => { void reload() }, { deep: true, immediate: true })

  // Realtime: подписка на list_update, дебаунс reload.
  let debounceTimer: ReturnType<typeof setTimeout> | null = null
  const unsubscribe = subscribeListUpdate(ORDER_DOCTYPE, () => {
    if (debounceTimer)
      clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => { void reload() }, REALTIME_DEBOUNCE_MS)
  })

  onScopeDispose(() => {
    abortController?.abort()
    if (debounceTimer)
      clearTimeout(debounceTimer)
    unsubscribe()
  })

  return { data, total, state, error, hasMore, reload, loadMore }
}

interface UseOrderResult {
  data: ShallowRef<CustomerOrder | null>
  state: Ref<ScreenState>
  error: Ref<ApiError | null>
  reload: () => Promise<void>
  /** Сохранить doc; resolve = новый snapshot, reject = `ApiError` (включая `kind: 'conflict'`). */
  save: (patch: Partial<CustomerOrder>, modified: string) => Promise<CustomerOrder>
}

export function useOrder(name: Ref<string>): UseOrderResult {
  const data = shallowRef<CustomerOrder | null>(null)
  const state = ref<ScreenState>('idle')
  const error = ref<ApiError | null>(null)
  let abortController: AbortController | null = null

  async function reload(): Promise<void> {
    if (!name.value)
      return
    abortController?.abort()
    abortController = new AbortController()
    state.value = 'loading'
    error.value = null
    try {
      const response = await httpClient.get<OrderDetailResponse>(`/api/orders/${encodeURIComponent(name.value)}`, {
        signal: abortController.signal,
      })
      data.value = mapOrderDetail(response.data)
      state.value = 'loaded'
    }
    catch (e) {
      if ((e as { name?: string }).name === 'CanceledError')
        return
      error.value = toApiError(e)
      state.value = 'error'
    }
  }

  async function save(patch: Partial<CustomerOrder>, _modified: string): Promise<CustomerOrder> {
    if (!data.value)
      throw new Error('Order not loaded')
    state.value = 'saving'
    error.value = null
    const expectedVersion = (data.value as VersionedCustomerOrder).version
    const doc: Partial<CustomerOrder> = {
      ...data.value,
      ...patch,
      name: name.value,
    }
    try {
      const response = await httpClient.put<OrderDetailResponse>(
        `/api/orders/${encodeURIComponent(name.value)}`,
        mapUpdateOrderPayload(doc, expectedVersion),
      )
      const saved = mapOrderDetail(response.data)
      data.value = saved
      state.value = 'saved'
      return saved
    }
    catch (e) {
      const apiErr = toApiError(e)
      error.value = apiErr
      state.value = apiErr.kind === 'conflict' ? 'conflict' : 'error'
      throw apiErr
    }
  }

  watch(name, () => { void reload() }, { immediate: true })

  let unsubscribe: (() => void) | null = null
  watch(name, (newName, _oldName) => {
    unsubscribe?.()
    if (!newName)
      return
    unsubscribe = subscribeDocUpdate(ORDER_DOCTYPE, newName, () => { void reload() })
  }, { immediate: true })

  onScopeDispose(() => {
    abortController?.abort()
    unsubscribe?.()
  })

  return { data, state, error, reload, save }
}

/** Создать новый заказ (insert), возвращает имя созданного документа. */
export async function createOrder(payload: Partial<CustomerOrder>): Promise<CustomerOrder> {
  const response = await httpClient.post<OrderDetailResponse>('/api/orders', mapCreateOrderPayload(payload))
  return mapOrderDetail(response.data)
}

export const ordersInternals = {
  LIST_PAGE_SIZE,
  buildFilterPayload,
  buildOrFilters,
  REALTIME_DEBOUNCE_MS,
  buildOrderQueryParams,
  mapOrderListRow,
  mapCreateOrderPayload,
  mapUpdateOrderPayload,
}
