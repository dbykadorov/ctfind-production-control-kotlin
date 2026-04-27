import type { ProductionTaskDetailResponse } from '@/api/types/production-tasks'
import { ref } from 'vue'

export function useProductionTaskDetail() {
  const data = ref<ProductionTaskDetailResponse | null>(null)
  const loading = ref(false)
  const error = ref<unknown>(null)

  async function load(_id: string) {
    loading.value = false
    error.value = null
  }

  return { data, loading, error, load }
}
