import type { AuditLogFilters, AuditLogPageResponse } from '@/api/types/audit-log'
import { httpClient } from '@/api/api-client'
import { onScopeDispose, ref, type Ref } from 'vue'

export interface AuditLogError {
  kind: 'forbidden' | 'error'
  message?: string
}

export interface UseAuditLogResult {
  data: Ref<AuditLogPageResponse | null>
  loading: Ref<boolean>
  error: Ref<AuditLogError | null>
  refetch: (filters?: AuditLogFilters) => Promise<void>
}

function buildParams(filters: AuditLogFilters): Record<string, string | string[] | number> {
  const now = new Date()
  const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)

  const p: Record<string, string | string[] | number> = {
    from: (filters.from ?? sevenDaysAgo.toISOString()),
    to: (filters.to ?? now.toISOString()),
    page: filters.page ?? 0,
    size: filters.size ?? 50,
  }

  if (filters.category && filters.category.length > 0)
    p.category = filters.category

  if (filters.actorUserId)
    p.actorUserId = filters.actorUserId

  const search = filters.search?.trim()
  if (search)
    p.search = search

  return p
}

export function useAuditLog(): UseAuditLogResult {
  const data = ref<AuditLogPageResponse | null>(null)
  const loading = ref(false)
  const error = ref<AuditLogError | null>(null)
  let abortController: AbortController | null = null

  async function refetch(filters: AuditLogFilters = {}): Promise<void> {
    abortController?.abort()
    abortController = new AbortController()
    loading.value = true
    error.value = null
    try {
      const response = await httpClient.get<AuditLogPageResponse>('/api/audit', {
        params: buildParams(filters),
        signal: abortController.signal,
      })
      data.value = response.data
    }
    catch (e) {
      if ((e as { name?: string }).name === 'CanceledError')
        return
      const resp = (e as { response?: { status?: number } }).response
      if (resp?.status === 403) {
        error.value = { kind: 'forbidden' }
      }
      else {
        error.value = { kind: 'error', message: (e as Error).message }
      }
      data.value = null
    }
    finally {
      loading.value = false
    }
  }

  onScopeDispose(() => abortController?.abort())

  return { data, loading, error, refetch }
}
