/** Composable для переходов статуса заказа. */

import type { ApiError, OrderTransition } from '@/api/types/domain'
import { onScopeDispose, ref, type Ref, shallowRef, type ShallowRef, watch } from 'vue'

interface UseTransitionsResult {
  data: ShallowRef<OrderTransition[]>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  reload: () => Promise<void>
}

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
    data.value = []
    loading.value = false
  }

  watch(name, () => { void reload() }, { immediate: true })

  onScopeDispose(() => abortController?.abort())

  return { data, loading, error, reload }
}

/** Применить workflow-переход. Бросает `ApiError` при ошибке. */
export async function applyTransition(name: string, action: string): Promise<{ status: string }> {
  void name
  void action
  throw new Error('Order transition API is not implemented yet')
}
