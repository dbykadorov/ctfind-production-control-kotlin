import type { NotificationsPageResponse } from '@/api/types/notifications'
import { httpClient } from '@/api/api-client'
import { onScopeDispose, ref, type Ref } from 'vue'

export interface UseNotificationsError {
  kind: 'error'
  message?: string
}

export interface UseNotificationsResult {
  data: Ref<NotificationsPageResponse | null>
  loading: Ref<boolean>
  error: Ref<UseNotificationsError | null>
  refetch: (params?: { page?: number, unreadOnly?: boolean }) => Promise<void>
}

export function useNotifications(): UseNotificationsResult {
  const data = ref<NotificationsPageResponse | null>(null)
  const loading = ref(false)
  const error = ref<UseNotificationsError | null>(null)
  let abortController: AbortController | null = null

  async function refetch(params: { page?: number, unreadOnly?: boolean } = {}): Promise<void> {
    abortController?.abort()
    abortController = new AbortController()
    loading.value = true
    error.value = null
    try {
      const res = await httpClient.get<NotificationsPageResponse>('/api/notifications', {
        params: {
          page: params.page ?? 0,
          size: 20,
          unreadOnly: params.unreadOnly ?? false,
        },
        signal: abortController.signal,
      })
      data.value = res.data
    } catch (e) {
      if ((e as { name?: string }).name === 'CanceledError') return
      error.value = { kind: 'error', message: (e as Error).message }
      data.value = null
    } finally {
      loading.value = false
    }
  }

  onScopeDispose(() => abortController?.abort())

  return { data, loading, error, refetch }
}
