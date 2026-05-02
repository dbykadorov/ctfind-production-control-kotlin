/**
 * 007-cabinet-dashboard-theme: «Последние изменения статусов» (виджет дашборда).
 * Читает последние изменения статусов из Spring dashboard endpoint.
 */

import type { RecentStatusChange } from '@/api/types/dashboard'
import type { ApiError, OrderStatus } from '@/api/types/domain'
import type { BackendOrderStatus, DashboardSummaryResponse } from '@/api/types/orders'
import { onScopeDispose, ref, type Ref } from 'vue'
import { httpClient } from '@/api/api-client'
import { subscribeListUpdate } from '@/api/socket'
import { toApiError } from '@/utils/errors'

const STATUS_CHANGE_DOCTYPE = 'Customer Order Status Change'
const PAGE_SIZE = 10
const REALTIME_DEBOUNCE_MS = 1500
const FALLBACK_POLL_MS = 30_000
const BACKEND_TO_UI_STATUS: Record<BackendOrderStatus, OrderStatus> = {
  NEW: 'новый',
  IN_WORK: 'в работе',
  READY: 'готов',
  SHIPPED: 'отгружен',
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
    loading.value = true
    error.value = null
    try {
      const response = await httpClient.get<DashboardSummaryResponse>('/api/orders/dashboard', {
        signal: abortController.signal,
      })
      data.value = response.data.recentChanges.slice(0, PAGE_SIZE).map(change => ({
        name: `status:${change.orderId}:${change.changedAt}`,
        order: change.orderId,
        fromStatus: change.fromStatus ? BACKEND_TO_UI_STATUS[change.fromStatus] : 'новый',
        toStatus: BACKEND_TO_UI_STATUS[change.toStatus],
        actorUser: change.actorDisplayName,
        eventAt: change.changedAt,
      }))
    }
    catch (e) {
      if ((e as { name?: string }).name === 'CanceledError')
        return
      error.value = toApiError(e)
      data.value = []
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
