/**
 * 007-cabinet-dashboard-theme: «Последние изменения статусов» (виджет дашборда).
 * Пока Spring endpoints для истории статусов не реализованы, возвращает пустой
 * список без сетевых запросов.
 */

import type { RecentStatusChange } from '@/api/types/dashboard'
import type { ApiError } from '@/api/types/domain'
import { onScopeDispose, ref, type Ref } from 'vue'
import { subscribeListUpdate } from '@/api/socket'

const STATUS_CHANGE_DOCTYPE = 'Customer Order Status Change'
const PAGE_SIZE = 10
const REALTIME_DEBOUNCE_MS = 1500
const FALLBACK_POLL_MS = 30_000

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
    data.value = []
    loading.value = false
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
