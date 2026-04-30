import type { ApiError } from '@/api/types/domain'
import type { AdminUserSummaryResponse, CreateUserRequest, RoleSummaryResponse } from '@/api/types/user-management'
import { httpClient } from '@/api/api-client'
import { toApiError } from '@/utils/errors'
import { onScopeDispose, ref, type Ref } from 'vue'

export interface UseUsersListResult {
  data: Ref<AdminUserSummaryResponse[]>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  refetch: (search?: string, limit?: number) => Promise<void>
}

function buildParams(search?: string, limit?: number): Record<string, string | number> {
  const params: Record<string, string | number> = {}
  if (search?.trim())
    params.search = search.trim()
  if (typeof limit === 'number')
    params.limit = limit
  return params
}

export async function fetchUsers(search?: string, limit?: number): Promise<AdminUserSummaryResponse[]> {
  const response = await httpClient.get<AdminUserSummaryResponse[]>('/api/users', {
    params: buildParams(search, limit),
  })
  return response.data
}

export async function fetchRoleCatalog(): Promise<RoleSummaryResponse[]> {
  const response = await httpClient.get<RoleSummaryResponse[]>('/api/users/roles')
  return response.data
}

export async function createUser(payload: CreateUserRequest): Promise<AdminUserSummaryResponse> {
  const response = await httpClient.post<AdminUserSummaryResponse>('/api/users', payload)
  return response.data
}

export function useUsersList(): UseUsersListResult {
  const data = ref<AdminUserSummaryResponse[]>([])
  const loading = ref(false)
  const error = ref<ApiError | null>(null)
  let abortController: AbortController | null = null

  async function refetch(search?: string, limit?: number): Promise<void> {
    abortController?.abort()
    abortController = new AbortController()
    loading.value = true
    error.value = null
    try {
      const response = await httpClient.get<AdminUserSummaryResponse[]>('/api/users', {
        params: buildParams(search, limit),
        signal: abortController.signal,
      })
      data.value = response.data
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

  onScopeDispose(() => abortController?.abort())

  return { data, loading, error, refetch }
}
