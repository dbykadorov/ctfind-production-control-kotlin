import type { ApiError } from '@/api/types/domain'
import type { MaterialUsage } from '@/api/types/warehouse'
import { ref, type Ref, shallowRef, type ShallowRef } from 'vue'
import { httpClient } from '@/api/api-client'
import { toApiError } from '@/utils/errors'

interface UseOrderMaterialUsageResult {
  usage: ShallowRef<MaterialUsage | null>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  refetch: () => Promise<void>
}

export function useOrderMaterialUsage(orderId: string): UseOrderMaterialUsageResult {
  const usage = shallowRef<MaterialUsage | null>(null)
  const loading = ref(false)
  const error = ref<ApiError | null>(null)

  async function refetch(): Promise<void> {
    if (!orderId)
      return
    loading.value = true
    error.value = null
    try {
      const response = await httpClient.get<MaterialUsage>(`/api/orders/${orderId}/material-usage`)
      usage.value = response.data
    }
    catch (e) {
      error.value = toApiError(e)
      usage.value = null
    }
    finally {
      loading.value = false
    }
  }

  refetch()

  return {
    usage,
    loading,
    error,
    refetch,
  }
}
