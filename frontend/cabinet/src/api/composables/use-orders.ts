/**
 * Composables для работы с заказами.
 * Spring endpoints для заказов ещё не реализованы, поэтому data-layer пока
 * возвращает пустые состояния без сетевых запросов.
 */

import type {
  ApiError,
  CustomerOrder,
  OrderFilters,
  OrderListItem,
  ScreenState,
} from '@/api/types/domain'
import { onScopeDispose, ref, type Ref, shallowRef, type ShallowRef, watch } from 'vue'
import { subscribeDocUpdate, subscribeListUpdate } from '@/api/socket'
import { toApiError } from '@/utils/errors'

const ORDER_DOCTYPE = 'Customer Order'
const LIST_PAGE_SIZE = 50
const REALTIME_DEBOUNCE_MS = 500

const ORDER_LIST_FIELDS = [
  'name',
  'status',
  'delivery_date',
  'modified',
  'creation',
  'customer',
  'created_by_staff',
]

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

interface OrderListResponse {
  name: string
  status: OrderListItem['status']
  delivery_date: string
  modified: string
  creation: string
  customer: string
  created_by_staff?: string
  customer_name?: string
}

async function fetchOrdersListPage(
  filters: OrderFilters,
  start: number,
  pageSize: number,
  _signal?: AbortSignal,
  orderBy?: UseOrdersListOptions['orderBy'],
): Promise<OrderListItem[]> {
  const orderClause = orderBy ? `${orderBy.field} ${orderBy.order}` : 'delivery_date asc, modified desc'
  void {
    fields: ORDER_LIST_FIELDS,
    ...buildFilterPayload(filters),
    ...buildOrFilters(filters.search),
    order_by: orderClause,
    limit_start: start,
    limit_page_length: pageSize,
  }
  const rows: OrderListResponse[] = []
  return rows.map(row => ({
    name: row.name,
    status: row.status,
    delivery_date: row.delivery_date,
    modified: row.modified,
    creation: row.creation,
    customer: row.customer,
    customer_name: row.customer_name,
    created_by_staff: row.created_by_staff,
  }))
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
      const rows = await fetchOrdersListPage(
        filters.value,
        start,
        pageSize + 1,
        abortController.signal,
        options.orderBy,
      )
      const more = rows.length > pageSize
      const page = more ? rows.slice(0, pageSize) : rows
      const enriched = await fetchCustomerNames(page)
      data.value = append ? [...data.value, ...enriched] : enriched
      total.value = data.value.length
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
      data.value = null
      state.value = 'empty'
    }
    catch (e) {
      if ((e as { name?: string }).name === 'CanceledError')
        return
      error.value = toApiError(e)
      state.value = 'error'
    }
  }

  async function save(patch: Partial<CustomerOrder>, modified: string): Promise<CustomerOrder> {
    if (!data.value)
      throw new Error('Order not loaded')
    state.value = 'saving'
    error.value = null
    const doc = {
      ...data.value,
      ...patch,
      modified,
      doctype: ORDER_DOCTYPE,
      name: name.value,
    }
    try {
      void doc
      throw new Error('Order API is not implemented yet')
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
  void payload
  throw new Error('Order API is not implemented yet')
}

export const ordersInternals = { LIST_PAGE_SIZE, buildFilterPayload, buildOrFilters, REALTIME_DEBOUNCE_MS }
