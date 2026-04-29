import type { ApiError } from '@/api/types/domain'
import type {
  MaterialResponse,
  MaterialsPageResponse,
  StockMovementsPageResponse,
} from '@/api/types/warehouse'
import { ref, type Ref, shallowRef, type ShallowRef } from 'vue'
import { httpClient } from '@/api/api-client'
import { toApiError } from '@/utils/errors'

interface UseMaterialsResult {
  data: ShallowRef<MaterialResponse[]>
  totalPages: Ref<number>
  totalItems: Ref<number>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  page: Ref<number>
  search: Ref<string>
  refetch: () => Promise<void>
  nextPage: () => void
  prevPage: () => void
}

export function useMaterials(pageSize = 20): UseMaterialsResult {
  const data = shallowRef<MaterialResponse[]>([])
  const totalPages = ref(0)
  const totalItems = ref(0)
  const loading = ref(false)
  const error = ref<ApiError | null>(null)
  const page = ref(0)
  const search = ref('')

  async function refetch(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const response = await httpClient.get<MaterialsPageResponse>('/api/materials', {
        params: {
          page: page.value,
          size: pageSize,
          search: search.value || undefined,
        },
      })
      data.value = response.data.items
      totalPages.value = response.data.totalPages
      totalItems.value = response.data.totalItems
    }
    catch (e) {
      error.value = toApiError(e)
      data.value = []
    }
    finally {
      loading.value = false
    }
  }

  function nextPage() {
    if (page.value < totalPages.value - 1) {
      page.value++
      refetch()
    }
  }

  function prevPage() {
    if (page.value > 0) {
      page.value--
      refetch()
    }
  }

  refetch()

  return { data, totalPages, totalItems, loading, error, page, search, refetch, nextPage, prevPage }
}

interface UseMaterialDetailResult {
  data: ShallowRef<MaterialResponse | null>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  refetch: () => Promise<void>
}

export function useMaterialDetail(id: string): UseMaterialDetailResult {
  const data = shallowRef<MaterialResponse | null>(null)
  const loading = ref(false)
  const error = ref<ApiError | null>(null)

  async function refetch(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const response = await httpClient.get<MaterialResponse>(`/api/materials/${id}`)
      data.value = response.data
    }
    catch (e) {
      error.value = toApiError(e)
      data.value = null
    }
    finally {
      loading.value = false
    }
  }

  refetch()

  return { data, loading, error, refetch }
}

interface UseMaterialMovementsResult {
  data: ShallowRef<StockMovementsPageResponse['items']>
  totalPages: Ref<number>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  page: Ref<number>
  refetch: () => Promise<void>
  nextPage: () => void
  prevPage: () => void
}

export function useMaterialMovements(materialId: string, pageSize = 20): UseMaterialMovementsResult {
  const data = shallowRef<StockMovementsPageResponse['items']>([])
  const totalPages = ref(0)
  const loading = ref(false)
  const error = ref<ApiError | null>(null)
  const page = ref(0)

  async function refetch(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const response = await httpClient.get<StockMovementsPageResponse>(
        `/api/materials/${materialId}/movements`,
        { params: { page: page.value, size: pageSize } },
      )
      data.value = response.data.items
      totalPages.value = response.data.totalPages
    }
    catch (e) {
      error.value = toApiError(e)
      data.value = []
    }
    finally {
      loading.value = false
    }
  }

  function nextPage() {
    if (page.value < totalPages.value - 1) {
      page.value++
      refetch()
    }
  }

  function prevPage() {
    if (page.value > 0) {
      page.value--
      refetch()
    }
  }

  refetch()

  return { data, totalPages, loading, error, page, refetch, nextPage, prevPage }
}
