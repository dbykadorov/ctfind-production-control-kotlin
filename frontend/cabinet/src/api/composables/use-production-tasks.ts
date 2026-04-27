import type { ProductionTaskListFilters, ProductionTasksPageResponse } from '@/api/types/production-tasks'
import { ref } from 'vue'

export function useProductionTasksList() {
  const data = ref<ProductionTasksPageResponse | null>(null)
  const loading = ref(false)
  const error = ref<unknown>(null)

  async function refetch(_filters: ProductionTaskListFilters = {}) {
    loading.value = false
    error.value = null
    data.value = { items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 }
  }

  return { data, loading, error, refetch }
}
