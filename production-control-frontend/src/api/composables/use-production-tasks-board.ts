/**
 * Доска производственных задач (Feature 006 §M4).
 *
 * Делает один запрос `GET /api/production-tasks?size=200&...filters`,
 * группирует ответ по статусу для четырёх колонок доски и применяет
 * UI-правила, которые в backend отсутствуют:
 *
 * - COMPLETED column капается на 30 последних задач, обновлённых за
 *   последние 7 дней (FR-015 / R-002). На задачах со статусом COMPLETED
 *   `updatedAt` равен моменту завершения, потому что feature 005
 *   запрещает редактирование завершённых задач.
 * - Клиентский фильтр `overdueOnly` (UI-only — на API такого параметра
 *   нет) убирает все не-просроченные задачи перед группировкой; колонка
 *   COMPLETED по определению становится пустой при этом фильтре.
 *
 * Видимость («исполнитель видит только назначенные») остаётся серверным
 * правилом из feature 005 — composable НЕ добавляет проверок ролей.
 */
import type { ApiError } from '@/api/types/domain'
import type {
  ProductionTaskListFilters,
  ProductionTaskListRowResponse,
  ProductionTaskStatus,
  ProductionTasksPageResponse,
} from '@/api/types/production-tasks'
import { httpClient } from '@/api/api-client'
import { toApiError } from '@/utils/errors'
import { onScopeDispose, ref, type Ref } from 'vue'

const BOARD_PAGE_SIZE = 200
const COMPLETED_COLUMN_LIMIT = 30
const COMPLETED_WINDOW_MS = 7 * 24 * 60 * 60 * 1000

export interface ProductionTasksBoardFilters extends ProductionTaskListFilters {
  /**
   * UI-only флажок «только просроченные». Не отправляется на сервер;
   * применяется на клиенте после группировки (см. R-006).
   */
  overdueOnly?: boolean
}

export interface ProductionTasksBoardData {
  byStatus: Record<ProductionTaskStatus, ProductionTaskListRowResponse[]>
  totalVisible: number
  truncated: boolean
}

export interface UseProductionTasksBoardResult {
  data: Ref<ProductionTasksBoardData | null>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  forbidden: Ref<boolean>
  refetch: (filters?: ProductionTasksBoardFilters) => Promise<void>
}

function emptyByStatus(): Record<ProductionTaskStatus, ProductionTaskListRowResponse[]> {
  return {
    NOT_STARTED: [],
    IN_PROGRESS: [],
    BLOCKED: [],
    COMPLETED: [],
  }
}

function safeParseTimestamp(input?: string | null): number | null {
  if (!input)
    return null
  const ms = Date.parse(input)
  return Number.isNaN(ms) ? null : ms
}

function isOverdue(row: ProductionTaskListRowResponse, now: Date = new Date()): boolean {
  if (row.status === 'COMPLETED')
    return false
  const due = safeParseTimestamp(row.plannedFinishDate)
  if (due === null)
    return false
  const startOfToday = new Date(now)
  startOfToday.setHours(0, 0, 0, 0)
  return due < startOfToday.getTime()
}

function buildParams(f: ProductionTasksBoardFilters): Record<string, string | number | boolean> {
  const p: Record<string, string | number | boolean> = { size: BOARD_PAGE_SIZE }
  if (f.search)
    p.search = f.search
  if (f.executorUserId)
    p.executorUserId = f.executorUserId
  if (f.dueDateFrom)
    p.dueDateFrom = f.dueDateFrom
  if (f.dueDateTo)
    p.dueDateTo = f.dueDateTo
  if (f.assignedToMe)
    p.assignedToMe = true
  if (f.sort)
    p.sort = f.sort
  return p
}

function groupAndCap(
  rows: readonly ProductionTaskListRowResponse[],
  now: Date,
): Record<ProductionTaskStatus, ProductionTaskListRowResponse[]> {
  const byStatus = emptyByStatus()
  for (const row of rows)
    byStatus[row.status].push(row)

  const cutoff = now.getTime() - COMPLETED_WINDOW_MS
  byStatus.COMPLETED = byStatus.COMPLETED
    .filter((r) => {
      const updatedAt = safeParseTimestamp(r.updatedAt)
      return updatedAt !== null && updatedAt >= cutoff
    })
    .sort((a, b) => (safeParseTimestamp(b.updatedAt) ?? 0) - (safeParseTimestamp(a.updatedAt) ?? 0))
    .slice(0, COMPLETED_COLUMN_LIMIT)
  return byStatus
}

export function useProductionTasksBoard(): UseProductionTasksBoardResult {
  const data = ref<ProductionTasksBoardData | null>(null)
  const loading = ref(false)
  const error = ref<ApiError | null>(null)
  const forbidden = ref(false)
  let abortController: AbortController | null = null

  async function refetch(filters: ProductionTasksBoardFilters = {}): Promise<void> {
    abortController?.abort()
    abortController = new AbortController()
    loading.value = true
    error.value = null
    forbidden.value = false
    try {
      const response = await httpClient.get<ProductionTasksPageResponse>('/api/production-tasks', {
        params: buildParams(filters),
        signal: abortController.signal,
      })
      const now = new Date()
      const rows = filters.overdueOnly
        ? response.data.items.filter(r => isOverdue(r, now))
        : response.data.items
      const byStatus = groupAndCap(rows, now)
      const totalVisible = byStatus.NOT_STARTED.length
        + byStatus.IN_PROGRESS.length
        + byStatus.BLOCKED.length
        + byStatus.COMPLETED.length
      data.value = {
        byStatus,
        totalVisible,
        truncated: response.data.totalItems > BOARD_PAGE_SIZE,
      }
    }
    catch (e) {
      if ((e as { name?: string }).name === 'CanceledError')
        return
      const status = (e as { response?: { status?: number } }).response?.status
      if (status === 403) {
        forbidden.value = true
        data.value = null
      }
      else {
        error.value = toApiError(e)
        data.value = null
      }
    }
    finally {
      loading.value = false
    }
  }

  onScopeDispose(() => abortController?.abort())

  return { data, loading, error, forbidden, refetch }
}
