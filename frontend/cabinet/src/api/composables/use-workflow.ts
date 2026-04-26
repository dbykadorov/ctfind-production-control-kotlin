/**
 * Composable для работы с Frappe Workflow для заказов.
 * Контракты: см. http-endpoints.md §Workflow.
 */

import type { ApiError, OrderTransition } from '@/api/types/domain'
import { onScopeDispose, ref, type Ref, shallowRef, type ShallowRef, watch } from 'vue'
import { frappeCall } from '@/api/frappe-client'
import { toApiError } from '@/utils/errors'

const ORDER_DOCTYPE = 'Customer Order'

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
    try {
      const transitions = await frappeCall<OrderTransition[]>(
        'frappe.model.workflow.get_transitions',
        { doc: { doctype: ORDER_DOCTYPE, name: name.value } },
        { signal: abortController.signal },
      )
      data.value = Array.isArray(transitions) ? transitions : []
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
  return frappeCall<{ status: string }>(
    'frappe.model.workflow.apply_workflow',
    { doc: { doctype: ORDER_DOCTYPE, name }, action },
  )
}
