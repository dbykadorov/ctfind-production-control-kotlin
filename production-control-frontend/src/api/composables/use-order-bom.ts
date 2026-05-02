import type { ApiError } from '@/api/types/domain'
import type {
  BomLine,
  BomLineCreateRequest,
  BomLineListResponse,
  BomLineUpdateRequest,
} from '@/api/types/warehouse'
import { ref, type Ref, shallowRef, type ShallowRef } from 'vue'
import { httpClient } from '@/api/api-client'
import { toApiError } from '@/utils/errors'

interface UseOrderBomResult {
  lines: ShallowRef<BomLine[]>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  refetch: () => Promise<void>
  addLine: (request: BomLineCreateRequest) => Promise<BomLine | null>
  updateLine: (lineId: string, request: BomLineUpdateRequest) => Promise<BomLine | null>
  removeLine: (lineId: string) => Promise<boolean>
}

export function useOrderBom(orderId: string): UseOrderBomResult {
  const lines = shallowRef<BomLine[]>([])
  const loading = ref(false)
  const error = ref<ApiError | null>(null)

  async function refetch(): Promise<void> {
    if (!orderId)
      return
    loading.value = true
    error.value = null
    try {
      const response = await httpClient.get<BomLineListResponse>(`/api/orders/${orderId}/bom`)
      lines.value = response.data.items
    }
    catch (e) {
      error.value = toApiError(e)
      lines.value = []
    }
    finally {
      loading.value = false
    }
  }

  async function addLine(request: BomLineCreateRequest): Promise<BomLine | null> {
    error.value = null
    try {
      const response = await httpClient.post<BomLine>(`/api/orders/${orderId}/bom`, request)
      await refetch()
      return response.data
    }
    catch (e) {
      error.value = toApiError(e)
      return null
    }
  }

  async function updateLine(lineId: string, request: BomLineUpdateRequest): Promise<BomLine | null> {
    error.value = null
    try {
      const response = await httpClient.put<BomLine>(`/api/orders/${orderId}/bom/${lineId}`, request)
      await refetch()
      return response.data
    }
    catch (e) {
      error.value = toApiError(e)
      return null
    }
  }

  async function removeLine(lineId: string): Promise<boolean> {
    error.value = null
    try {
      await httpClient.delete(`/api/orders/${orderId}/bom/${lineId}`)
      await refetch()
      return true
    }
    catch (e) {
      error.value = toApiError(e)
      return false
    }
  }

  refetch()

  return {
    lines,
    loading,
    error,
    refetch,
    addLine,
    updateLine,
    removeLine,
  }
}
