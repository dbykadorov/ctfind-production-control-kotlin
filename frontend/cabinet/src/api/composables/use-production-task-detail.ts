/**
 * Карточка производственной задачи (`GET /api/production-tasks/{id}`).
 */
import type { ApiError } from '@/api/types/domain'
import type {
  PostProductionTaskStatusPayload,
  ProductionTaskAssigneesResponse,
  ProductionTaskDetailResponse,
  PutProductionTaskAssignmentPayload,
} from '@/api/types/production-tasks'
import { httpClient } from '@/api/api-client'
import { toApiError } from '@/utils/errors'
import { onScopeDispose, ref, watch, type Ref } from 'vue'

export interface UseProductionTaskDetailResult {
  data: Ref<ProductionTaskDetailResponse | null>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  forbidden: Ref<boolean>
  reload: () => Promise<void>
}

export function useProductionTaskDetail(taskId: Ref<string | undefined>): UseProductionTaskDetailResult {
  const data = ref<ProductionTaskDetailResponse | null>(null)
  const loading = ref(false)
  const error = ref<ApiError | null>(null)
  const forbidden = ref(false)
  let abortController: AbortController | null = null

  async function reload(): Promise<void> {
    const id = taskId.value?.trim()
    if (!id) {
      data.value = null
      loading.value = false
      return
    }
    abortController?.abort()
    abortController = new AbortController()
    loading.value = true
    error.value = null
    forbidden.value = false
    try {
      const response = await httpClient.get<ProductionTaskDetailResponse>(
        `/api/production-tasks/${encodeURIComponent(id)}`,
        { signal: abortController.signal },
      )
      data.value = response.data
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

  watch(taskId, () => {
    void reload()
  }, { immediate: true })

  onScopeDispose(() => abortController?.abort())

  return { data, loading, error, forbidden, reload }
}

export async function putProductionTaskAssignment(
  taskId: string,
  body: PutProductionTaskAssignmentPayload,
): Promise<ProductionTaskDetailResponse> {
  const response = await httpClient.put<ProductionTaskDetailResponse>(
    `/api/production-tasks/${encodeURIComponent(taskId)}/assignment`,
    body,
  )
  return response.data
}

export async function postProductionTaskStatus(
  taskId: string,
  body: PostProductionTaskStatusPayload,
): Promise<ProductionTaskDetailResponse> {
  const response = await httpClient.post<ProductionTaskDetailResponse>(
    `/api/production-tasks/${encodeURIComponent(taskId)}/status`,
    body,
  )
  return response.data
}

export async function fetchProductionTaskAssignees(
  search?: string,
  limit = 20,
): Promise<ProductionTaskAssigneesResponse> {
  const response = await httpClient.get<ProductionTaskAssigneesResponse>('/api/production-tasks/assignees', {
    params: { search, limit },
  })
  return response.data
}