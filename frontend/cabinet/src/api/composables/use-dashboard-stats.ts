/**
 * 007-cabinet-dashboard-theme: KPI-карточки + status distribution для дашборда.
 * Виджет читает агрегированную статистику из Spring dashboard endpoint.
 */

import type { DashboardKpis, StatusDistributionEntry } from '@/api/types/dashboard'
import type { ApiError, OrderStatus } from '@/api/types/domain'
import type { BackendOrderStatus, DashboardSummaryResponse } from '@/api/types/orders'
import { onScopeDispose, ref, type Ref } from 'vue'
import { httpClient } from '@/api/api-client'
import { subscribeListUpdate } from '@/api/socket'
import { toApiError } from '@/utils/errors'

const ORDER_DOCTYPE = 'Customer Order'
const REALTIME_DEBOUNCE_MS = 1500
const FALLBACK_POLL_MS = 30_000

const STATUS_ORDER: OrderStatus[] = ['новый', 'в работе', 'готов', 'отгружен']
const UI_TO_BACKEND_STATUS: Record<OrderStatus, BackendOrderStatus> = {
  'новый': 'NEW',
  'в работе': 'IN_WORK',
  'готов': 'READY',
  'отгружен': 'SHIPPED',
}

interface UseDashboardStatsResult {
  kpis: Ref<DashboardKpis | null>
  distribution: Ref<StatusDistributionEntry[]>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  refetch: () => Promise<void>
}

export function useDashboardStats(): UseDashboardStatsResult {
  const kpis = ref<DashboardKpis | null>(null)
  const distribution = ref<StatusDistributionEntry[]>([])
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
      const total = response.data.totalOrders || 0
      kpis.value = {
        totalActive: response.data.activeOrders,
        inProgress: response.data.statusCounts.IN_WORK ?? 0,
        ready: response.data.statusCounts.READY ?? 0,
        overdue: response.data.overdueOrders,
      }
      distribution.value = STATUS_ORDER.map((status) => {
        const count = response.data.statusCounts[UI_TO_BACKEND_STATUS[status]] ?? 0
        return {
          status,
          count,
          percent: total === 0 ? 0 : Math.round((count / total) * 100),
        }
      })
    }
    catch (e) {
      if ((e as { name?: string }).name === 'CanceledError')
        return
      error.value = toApiError(e)
    }
    finally {
      loading.value = false
    }
  }

  // Initial fetch
  void refetch()

  // Realtime + polling fallback
  let debounceTimer: ReturnType<typeof setTimeout> | null = null
  function scheduleRefetch(): void {
    if (debounceTimer)
      clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => {
      void refetch()
    }, REALTIME_DEBOUNCE_MS)
  }
  const unsubscribe = subscribeListUpdate(ORDER_DOCTYPE, scheduleRefetch)
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

  return { kpis, distribution, loading, error, refetch }
}

export const dashboardStatsInternals = { REALTIME_DEBOUNCE_MS, FALLBACK_POLL_MS, STATUS_ORDER }
