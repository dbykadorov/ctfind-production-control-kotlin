import type { ApiError } from '@/api/types/domain'
import type { AxiosError } from 'axios'
import type {
  AdminUserSummaryResponse,
  CreateUserRequest,
  RoleSummaryResponse,
  UpdateUserRequest,
} from '@/api/types/user-management'
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

export async function updateUser(userId: string, payload: UpdateUserRequest): Promise<AdminUserSummaryResponse> {
  const response = await httpClient.put<AdminUserSummaryResponse>(`/api/users/${userId}`, payload)
  return response.data
}

export type UpdateUserErrorCode =
  | 'validation_error'
  | 'invalid_roles'
  | 'user_not_found'
  | 'last_admin_role_removal_forbidden'
  | 'forbidden'
  | 'unauthorized'
  | 'unknown'

export interface UpdateUserApiError extends ApiError {
  code: UpdateUserErrorCode
}

interface BackendErrorBody {
  code?: string
}

export function parseUpdateUserError(error: unknown): UpdateUserApiError {
  const apiError = toApiError(error)
  const backendCode = ((error as AxiosError<BackendErrorBody>)?.response?.data?.code ?? '').toLowerCase()

  const code: UpdateUserErrorCode = (() => {
    switch (backendCode) {
      case 'validation_error':
        return 'validation_error'
      case 'invalid_roles':
        return 'invalid_roles'
      case 'user_not_found':
        return 'user_not_found'
      case 'last_admin_role_removal_forbidden':
        return 'last_admin_role_removal_forbidden'
      case 'forbidden':
        return 'forbidden'
      case 'unauthorized':
        return 'unauthorized'
      default:
        break
    }
    if (apiError.status === 403 || apiError.kind === 'permission')
      return 'forbidden'
    if (apiError.status === 401 || apiError.kind === 'session-expired')
      return 'unauthorized'
    return 'unknown'
  })()

  return {
    ...apiError,
    code,
  }
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
