import type { ApiError } from '@/api/types/domain'
import type { InventoryOrderList, InventoryOrderSummary } from '@/api/types/warehouse'
import { ref, type Ref, shallowRef, type ShallowRef } from 'vue'
import { httpClient } from '@/api/api-client'
import { toApiError } from '@/utils/errors'

interface UseActiveOrdersResult {
  items: ShallowRef<InventoryOrderSummary[]>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  search: (query: string, limit?: number) => Promise<void>
}

export function useActiveOrders(): UseActiveOrdersResult {
  const items = shallowRef<InventoryOrderSummary[]>([])
  const loading = ref(false)
  const error = ref<ApiError | null>(null)
  let lastRequestId = 0

  async function search(query: string, limit = 20): Promise<void> {
    const normalized = query.trim()
    if (normalized.length < 2) {
      items.value = []
      error.value = null
      return
    }

    const requestId = ++lastRequestId
    loading.value = true
    error.value = null

    try {
      const response = await httpClient.get<InventoryOrderList>('/api/orders/active-for-consumption', {
        params: { search: normalized, limit },
      })
      if (requestId === lastRequestId)
        items.value = response.data.items
    }
    catch (e) {
      if (requestId === lastRequestId) {
        error.value = toApiError(e)
        items.value = []
      }
    }
    finally {
      if (requestId === lastRequestId)
        loading.value = false
    }
  }

  return {
    items,
    loading,
    error,
    search,
  }
}
