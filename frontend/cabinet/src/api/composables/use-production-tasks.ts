/**
 * Список производственных задач через Spring API (`/api/production-tasks`).
 */
import type { ApiError } from '@/api/types/domain'
import type { ProductionTaskListFilters, ProductionTasksPageResponse } from '@/api/types/production-tasks'
import { httpClient } from '@/api/api-client'
import { toApiError } from '@/utils/errors'
import { onScopeDispose, ref, type Ref } from 'vue'

export interface UseProductionTasksListResult {
  data: Ref<ProductionTasksPageResponse | null>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  refetch: (filters?: ProductionTaskListFilters) => Promise<void>
}

function buildParams(f: ProductionTaskListFilters): Record<string, string | number | boolean> {
  const p: Record<string, string | number | boolean> = {}
  if (f.search)
    p.search = f.search
  if (f.status)
    p.status = f.status
  if (f.orderId)
    p.orderId = f.orderId
  if (f.orderItemId)
    p.orderItemId = f.orderItemId
  if (f.executorUserId)
    p.executorUserId = f.executorUserId
  if (f.assignedToMe)
    p.assignedToMe = true
  if (f.blockedOnly)
    p.blockedOnly = true
  if (f.activeOnly)
    p.activeOnly = true
  if (f.dueDateFrom)
    p.dueDateFrom = f.dueDateFrom
  if (f.dueDateTo)
    p.dueDateTo = f.dueDateTo
  if (f.page !== undefined)
    p.page = f.page
  if (f.size !== undefined)
    p.size = f.size
  if (f.sort)
    p.sort = f.sort
  return p
}

export function useProductionTasksList(): UseProductionTasksListResult {
  const data = ref<ProductionTasksPageResponse | null>(null)
  const loading = ref(false)
  const error = ref<ApiError | null>(null)
  let abortController: AbortController | null = null

  async function refetch(filters: ProductionTaskListFilters = {}): Promise<void> {
    abortController?.abort()
    abortController = new AbortController()
    loading.value = true
    error.value = null
    try {
      const response = await httpClient.get<ProductionTasksPageResponse>('/api/production-tasks', {
        params: buildParams(filters),
        signal: abortController.signal,
      })
      data.value = response.data
    }
    catch (e) {
      if ((e as { name?: string }).name === 'CanceledError')
        return
      error.value = toApiError(e)
      data.value = null
    }
    finally {
      loading.value = false
    }
  }

  onScopeDispose(() => abortController?.abort())

  return { data, loading, error, refetch }
}
