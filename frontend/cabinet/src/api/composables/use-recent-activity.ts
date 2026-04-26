/**
 * 007-cabinet-dashboard-theme: «Последние изменения статусов» (виджет дашборда).
 *
 * Запрашивает последние 10 записей `Customer Order Status Change` через
 * стандартный `frappe.client.get_list`. Realtime — через socket-канал
 * `list_update:Customer Order Status Change` (см. dashboard-stats.contract.md §5).
 */

import type { RecentStatusChange } from '@/api/types/dashboard'
import type { ApiError, OrderStatus } from '@/api/types/domain'
import { onScopeDispose, ref, type Ref } from 'vue'
import { frappeCall } from '@/api/frappe-client'
import { subscribeListUpdate } from '@/api/socket'
import { toApiError } from '@/utils/errors'

const STATUS_CHANGE_DOCTYPE = 'Customer Order Status Change'
const PAGE_SIZE = 10
const REALTIME_DEBOUNCE_MS = 1500
const FALLBACK_POLL_MS = 30_000

interface FrappeStatusChangeRow {
  name: string
  order: string
  from_status: OrderStatus
  to_status: OrderStatus
  actor_user: string
  event_at: string
}

export interface UseRecentActivityResult {
  data: Ref<RecentStatusChange[]>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  refetch: () => Promise<void>
}

export function useRecentActivity(): UseRecentActivityResult {
  const data = ref<RecentStatusChange[]>([])
  const loading = ref(false)
  const error = ref<ApiError | null>(null)
  let abortController: AbortController | null = null

  async function refetch(): Promise<void> {
    abortController?.abort()
    abortController = new AbortController()
    const signal = abortController.signal
    loading.value = true
    error.value = null
    try {
      const rows = await frappeCall<FrappeStatusChangeRow[]>('frappe.client.get_list', {
        doctype: STATUS_CHANGE_DOCTYPE,
        fields: ['name', 'order', 'from_status', 'to_status', 'actor_user', 'event_at'],
        order_by: 'event_at desc',
        limit_page_length: PAGE_SIZE,
      }, { signal, method: 'GET' })

      data.value = rows.map(row => ({
        name: row.name,
        order: row.order,
        fromStatus: row.from_status,
        toStatus: row.to_status,
        actorUser: row.actor_user,
        eventAt: row.event_at,
      }))
    }
    catch (e) {
      if ((e as { name?: string }).name === 'CanceledError')
        return
      // Permission-ошибка для Customer Order Status Change — нормальный кейс
      // (FR-011): просто оставляем виджет пустым с error-стейтом.
      error.value = toApiError(e)
    }
    finally {
      loading.value = false
    }
  }

  void refetch()

  let debounceTimer: ReturnType<typeof setTimeout> | null = null
  function scheduleRefetch(): void {
    if (debounceTimer)
      clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => {
      void refetch()
    }, REALTIME_DEBOUNCE_MS)
  }
  const unsubscribe = subscribeListUpdate(STATUS_CHANGE_DOCTYPE, scheduleRefetch)
  const pollTimer = setInterval(() => {
    void refetch()
  }, FALLBACK_POLL_MS)

  onScopeDispose(() => {
    abortController?.abort()
    if (debounceTimer)
      clearTimeout(debounceTimer)
    clearInterval(pollTimer)
    unsubscribe()
  })

  return { data, loading, error, refetch }
}

export const recentActivityInternals = { PAGE_SIZE, REALTIME_DEBOUNCE_MS, FALLBACK_POLL_MS }
