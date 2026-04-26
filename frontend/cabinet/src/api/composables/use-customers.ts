import type { ApiError } from '@/api/types/domain'
import type { Customer } from '@/api/types/frappe.generated'
/**
 * Composable для подбора клиентов (Customer DocType).
 * MVP: один search-call, страничная пагинация на стороне Frappe (limit_page_length).
 * См. data-model.md §2.1 (Customer), spec 005.
 */
import { onScopeDispose, ref, type Ref, shallowRef, type ShallowRef } from 'vue'
import { frappeCall } from '@/api/frappe-client'
import { toApiError } from '@/utils/errors'

const CUSTOMER_DOCTYPE = 'Customer'
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
    try {
      const rows = await frappeCall<Customer[]>(
        'frappe.client.get_list',
        {
          doctype: CUSTOMER_DOCTYPE,
          fields: SEARCH_FIELDS,
          filters,
          ...(orFilters ? { or_filters: orFilters } : {}),
          order_by: 'customer_name asc',
          limit_page_length: 25,
        },
        { signal: abortController.signal },
      )
      data.value = rows
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

  onScopeDispose(() => abortController?.abort())

  return { data, loading, error, search }
}

/** Получить одного клиента по name. */
export async function getCustomer(name: string): Promise<Customer> {
  return frappeCall<Customer>('frappe.client.get', { doctype: CUSTOMER_DOCTYPE, name }, { method: 'GET' })
}
