/**
 * 007-cabinet-dashboard-theme: «Динамика заказов за 30 дней» (главный график).
 * Читает агрегированный тренд из Spring dashboard endpoint.
 */

import type { OrderTrendPoint, OrderTrendSeries } from '@/api/types/dashboard'
import type { ApiError } from '@/api/types/domain'
import type { DashboardSummaryResponse } from '@/api/types/orders'
import { onScopeDispose, ref, type Ref } from 'vue'
import { httpClient } from '@/api/api-client'
import { subscribeListUpdate } from '@/api/socket'
import { toApiError } from '@/utils/errors'

const ORDER_DOCTYPE = 'Customer Order'
const TREND_DAYS = 30
const TREND_FETCH_DAYS = 60
const TREND_PAGE_LIMIT = 5000
const REALTIME_DEBOUNCE_MS = 1500
const FALLBACK_POLL_MS = 30_000

export interface UseTrendDataResult {
  data: Ref<OrderTrendSeries | null>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  refetch: () => Promise<void>
}

function dayKey(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function enumerateDays(from: Date, days: number): string[] {
  const result: string[] = []
  const cursor = new Date(from)
  for (let i = 0; i < days; i++) {
    result.push(dayKey(cursor))
    cursor.setDate(cursor.getDate() + 1)
  }
  return result
}

/** Общая bucketing-функция, экспонируется для unit-тестов. */
export function bucketByDay(rows: Array<{ creation: string }>, fromDate: Date, days: number): OrderTrendPoint[] {
  const buckets = new Map<string, number>()
  for (const row of rows) {
    if (!row.creation)
      continue
    // creation формат: "YYYY-MM-DD HH:MM:SS" — берём первые 10 символов.
    const day = row.creation.slice(0, 10)
    buckets.set(day, (buckets.get(day) ?? 0) + 1)
  }
  return enumerateDays(fromDate, days).map(date => ({
    date,
    count: buckets.get(date) ?? 0,
  }))
}

/** Расчёт delta-процента по двум периодам. Возвращает null при делении на 0. */
export function computeDeltaPct(last: number, prev: number): number | null {
  if (prev === 0)
    return null
  return Math.round(((last - prev) / prev) * 1000) / 10
}

export function useTrendData(): UseTrendDataResult {
  const data = ref<OrderTrendSeries | null>(null)
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
      const points = response.data.trend.map(point => ({
        date: point.date,
        count: point.created,
      }))
      const totalLast30 = points.reduce((acc, point) => acc + point.count, 0)
      data.value = {
        points,
        totalLast30,
        totalPrev30: 0,
        delta30vsPrev30Pct: computeDeltaPct(totalLast30, 0),
      }
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

  void refetch()

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

  return { data, loading, error, refetch }
}

export const trendDataInternals = {
  TREND_DAYS,
  TREND_FETCH_DAYS,
  TREND_PAGE_LIMIT,
  REALTIME_DEBOUNCE_MS,
  FALLBACK_POLL_MS,
}
