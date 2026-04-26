import type { ApiError } from '@/api/types/domain'
import type { Customer } from '@/api/types/legacy.generated'
/**
 * Composable для подбора клиентов (Customer DocType).
 * Spring endpoint для клиентов ещё не реализован, поэтому поиск пока возвращает
 * пустой список без сетевого запроса.
 * См. data-model.md §2.1 (Customer), spec 005.
 */
import { onScopeDispose, ref, type Ref, shallowRef, type ShallowRef } from 'vue'

const SEARCH_FIELDS = ['name', 'customer_name', 'status', 'phone', 'contact_person']

interface UseCustomersSearchResult {
  data: ShallowRef<Customer[]>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  search: (query: string) => Promise<void>
}

/** Активный поиск клиентов: вызывается каждый раз при изменении поискового запроса. */
export function useCustomersSearch(options: { onlyActive?: boolean } = {}): UseCustomersSearchResult {
  const data = shallowRef<Customer[]>([])
  const loading = ref(false)
  const error = ref<ApiError | null>(null)
  let abortController: AbortController | null = null

  async function search(query: string): Promise<void> {
    abortController?.abort()
    abortController = new AbortController()
    loading.value = true
    error.value = null
    const trimmed = query.trim()
    const filters: Array<[string, string, unknown]> = []
    if (options.onlyActive ?? true)
      filters.push(['status', '=', 'active'])
    const orFilters = trimmed
      ? [
          ['name', 'like', `%${trimmed}%`],
          ['customer_name', 'like', `%${trimmed}%`],
        ]
      : undefined
    void { fields: SEARCH_FIELDS, filters, orFilters }
    data.value = []
    loading.value = false
  }

  onScopeDispose(() => abortController?.abort())

  return { data, loading, error, search }
}

/** Получить одного клиента по name. */
export async function getCustomer(name: string): Promise<Customer> {
  void name
  throw new Error('Customer API is not implemented yet')
}
