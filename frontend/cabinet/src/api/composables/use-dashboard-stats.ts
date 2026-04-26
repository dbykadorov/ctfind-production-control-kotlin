/**
 * 007-cabinet-dashboard-theme: KPI-карточки + status distribution для дашборда.
 * Пока Spring endpoints для заказов не реализованы, виджет возвращает пустую
 * статистику без сетевых запросов.
 */

import type { DashboardKpis, StatusDistributionEntry } from '@/api/types/dashboard'
import type { ApiError, OrderStatus } from '@/api/types/domain'
import { onScopeDispose, ref, type Ref } from 'vue'
import { subscribeListUpdate } from '@/api/socket'

const ORDER_DOCTYPE = 'Customer Order'
const REALTIME_DEBOUNCE_MS = 1500
const FALLBACK_POLL_MS = 30_000

const STATUS_ORDER: OrderStatus[] = ['новый', 'в работе', 'готов', 'отгружен']

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
    const counts: Record<OrderStatus, number> = {
      'новый': 0,
      'в работе': 0,
      'готов': 0,
      'отгружен': 0,
    }
    kpis.value = { totalActive: 0, inProgress: 0, ready: 0, overdue: 0 }
    distribution.value = STATUS_ORDER.map(status => ({
      status,
      count: counts[status],
      percent: 0,
    }))
    loading.value = false
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
