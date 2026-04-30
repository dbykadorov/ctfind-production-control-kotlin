import type { ApiError } from '@/api/types/domain'
import type { ConsumeRequest, StockMovementResponse } from '@/api/types/warehouse'
import { ref, type Ref } from 'vue'
import { httpClient } from '@/api/api-client'
import { toApiError } from '@/utils/errors'

interface UseStockConsumptionResult {
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  errorCode: Ref<string | null>
  availableStock: Ref<number | null>
  consume: (materialId: string, request: ConsumeRequest) => Promise<StockMovementResponse | null>
}

export function useStockConsumption(): UseStockConsumptionResult {
  const loading = ref(false)
  const error = ref<ApiError | null>(null)
  const errorCode = ref<string | null>(null)
  const availableStock = ref<number | null>(null)

  async function consume(materialId: string, request: ConsumeRequest): Promise<StockMovementResponse | null> {
    loading.value = true
    error.value = null
    errorCode.value = null
    availableStock.value = null
    try {
      const response = await httpClient.post<StockMovementResponse>(`/api/materials/${materialId}/consume`, request)
      return response.data
    }
    catch (e) {
      const apiErr = toApiError(e)
      error.value = apiErr
      const raw = apiErr.raw as { available?: string | number, error?: string } | undefined
      if (typeof raw?.error === 'string')
        errorCode.value = raw.error
      if (raw?.available !== undefined) {
        const parsed = Number(raw.available)
        availableStock.value = Number.isFinite(parsed) ? parsed : null
      }
      return null
    }
    finally {
      loading.value = false
    }
  }

  return {
    loading,
    error,
    errorCode,
    availableStock,
    consume,
  }
}
