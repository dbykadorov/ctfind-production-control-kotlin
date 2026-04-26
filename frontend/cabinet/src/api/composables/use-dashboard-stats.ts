/**
 * 007-cabinet-dashboard-theme: KPI-карточки + status distribution для дашборда.
 *
 * Только стандартные `frappe.client.get_count` (никаких новых серверных методов).
 * Realtime-инвалидация через socket-канал `list_update:Customer Order`,
 * fallback — `setInterval` 30 сек (см. dashboard-stats.contract.md §1, §2, §6).
 */

import type { DashboardKpis, StatusDistributionEntry } from '@/api/types/dashboard'
import type { ApiError, OrderStatus } from '@/api/types/domain'
import { onScopeDispose, ref, type Ref } from 'vue'
import { frappeCall } from '@/api/frappe-client'
import { subscribeListUpdate } from '@/api/socket'
import { toApiError } from '@/utils/errors'

const ORDER_DOCTYPE = 'Customer Order'
const REALTIME_DEBOUNCE_MS = 1500
const FALLBACK_POLL_MS = 30_000

const STATUS_ORDER: OrderStatus[] = ['новый', 'в работе', 'готов', 'отгружен']

function todayIso(): string {
  return new Date().toISOString().slice(0, 10)
}

async function getCount(filters: Array<[string, string, unknown]>, signal?: AbortSignal): Promise<number> {
  const result = await frappeCall<number>('frappe.client.get_count', {
    doctype: ORDER_DOCTYPE,
    filters,
  }, { signal, method: 'GET' })
  return Number(result) || 0
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
    const signal = abortController.signal
    loading.value = true
    error.value = null
    const today = todayIso()
    try {
      // 6 параллельных счётчиков:
      //   §1.1 totalActive  — переиспользуется как «не отгружен»
      //   §1.2 inProgress    — переиспользуется в donut
      //   §1.3 ready         — переиспользуется в donut
      //   §1.4 overdue       — только KPI
      //   §2 «новый»         — только donut
      //   §2 «отгружен»      — только donut
      const [
        totalActive,
        inProgress,
        ready,
        overdue,
        newCount,
        shippedCount,
      ] = await Promise.all([
        getCount([['status', '!=', 'отгружен']], signal),
        getCount([['status', '=', 'в работе']], signal),
        getCount([['status', '=', 'готов']], signal),
        getCount([['status', '!=', 'отгружен'], ['delivery_date', '<', today]], signal),
        getCount([['status', '=', 'новый']], signal),
        getCount([['status', '=', 'отгружен']], signal),
      ])

      kpis.value = { totalActive, inProgress, ready, overdue }

      const counts: Record<OrderStatus, number> = {
        'новый': newCount,
        'в работе': inProgress,
        'готов': ready,
        'отгружен': shippedCount,
      }
      const total = STATUS_ORDER.reduce((acc, s) => acc + counts[s], 0)
      distribution.value = STATUS_ORDER.map(status => ({
        status,
        count: counts[status],
        percent: total === 0 ? 0 : Math.round((counts[status] * 1000) / total) / 10,
      }))
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
