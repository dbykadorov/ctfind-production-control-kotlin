/** Composable для переходов статуса заказа. */

import type { ApiError, OrderTransition } from '@/api/types/domain'
import type { BackendOrderStatus, OrderDetailResponse } from '@/api/types/orders'
import { onScopeDispose, ref, type Ref, shallowRef, type ShallowRef, watch } from 'vue'
import { httpClient } from '@/api/api-client'
import { toApiError } from '@/utils/errors'

interface UseTransitionsResult {
  data: ShallowRef<OrderTransition[]>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  reload: () => Promise<void>
}

const BACKEND_TO_UI_STATUS: Record<BackendOrderStatus, string> = {
  NEW: 'новый',
  IN_WORK: 'в работе',
  READY: 'готов',
  SHIPPED: 'отгружен',
}

const NEXT_TRANSITION: Partial<Record<BackendOrderStatus, { action: string, toStatus: BackendOrderStatus }>> = {
  NEW: { action: 'В работу', toStatus: 'IN_WORK' },
  IN_WORK: { action: 'Готов', toStatus: 'READY' },
  READY: { action: 'Отгрузить', toStatus: 'SHIPPED' },
}

const transitionVersionCache = new Map<string, number>()
const actionStatusCache = new Map<string, BackendOrderStatus>()

export function useOrderTransitions(name: Ref<string>): UseTransitionsResult {
  const data = shallowRef<OrderTransition[]>([])
  const loading = ref(false)
  const error = ref<ApiError | null>(null)
  let abortController: AbortController | null = null

  async function reload(): Promise<void> {
    if (!name.value)
      return
    abortController?.abort()
    abortController = new AbortController()
    loading.value = true
    error.value = null
    try {
      const response = await httpClient.get<OrderDetailResponse>(
        `/api/orders/${encodeURIComponent(name.value)}`,
        { signal: abortController.signal },
      )
      transitionVersionCache.set(name.value, response.data.version)
      const currentStatus = response.data.status
      const transition = NEXT_TRANSITION[currentStatus]
      if (!transition) {
        data.value = []
        return
      }
      actionStatusCache.set(`${name.value}:${transition.action}`, transition.toStatus)
      data.value = [
        {
          action: transition.action,
          state: response.data.statusLabel ?? BACKEND_TO_UI_STATUS[currentStatus],
          next_state: BACKEND_TO_UI_STATUS[transition.toStatus],
          allowed: 'ORDER_MANAGER',
        },
      ]
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

  watch(name, () => { void reload() }, { immediate: true })

  onScopeDispose(() => abortController?.abort())

  return { data, loading, error, reload }
}

/** Применить workflow-переход. Бросает `ApiError` при ошибке. */
export async function applyTransition(name: string, action: string): Promise<{ status: string }> {
  const encodedName = encodeURIComponent(name)
  const toStatus = actionStatusCache.get(`${name}:${action}`) ?? actionToStatus(action)
  const expectedVersion = transitionVersionCache.get(name) ?? await loadExpectedVersion(name)
  const response = await httpClient.post<OrderDetailResponse>(`/api/orders/${encodedName}/status`, {
    expectedVersion,
    toStatus,
  })
  transitionVersionCache.set(name, response.data.version)
  return { status: response.data.statusLabel ?? BACKEND_TO_UI_STATUS[response.data.status] }
}

function actionToStatus(action: string): BackendOrderStatus {
  const found = Object.values(NEXT_TRANSITION).find(transition => transition.action === action)
  if (!found)
    throw new Error(`Unknown order transition action: ${action}`)
  return found.toStatus
}

async function loadExpectedVersion(name: string): Promise<number> {
  const response = await httpClient.get<OrderDetailResponse>(`/api/orders/${encodeURIComponent(name)}`)
  transitionVersionCache.set(name, response.data.version)
  return response.data.version
}
