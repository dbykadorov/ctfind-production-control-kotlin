import type { ApiError } from '@/api/types/domain'
import type { CustomerOption, CustomerSearchResponse } from '@/api/types/orders'
import type { Customer } from '@/api/types/legacy.generated'
/**
 * Composable для подбора существующих клиентов через Spring API.
 */
import { onScopeDispose, ref, type Ref, shallowRef, type ShallowRef } from 'vue'
import { httpClient } from '@/api/api-client'
import { toApiError } from '@/utils/errors'

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
    try {
      const response = await httpClient.get<CustomerSearchResponse>('/api/customers', {
        params: {
          search: trimmed,
          activeOnly: options.onlyActive ?? true,
          limit: 20,
        },
        signal: abortController.signal,
      })
      data.value = response.data.items.map(mapCustomer)
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
  const response = await httpClient.get<CustomerSearchResponse>('/api/customers', {
    params: {
      search: name,
      activeOnly: false,
      limit: 50,
    },
  })
  const match = response.data.items.find(customer => customer.id === name)
    ?? response.data.items.find(customer => customer.displayName === name)
  if (!match)
    throw new Error('Customer not found')
  return mapCustomer(match)
}

function mapCustomer(customer: CustomerOption): Customer {
  const now = new Date(0).toISOString()
  return {
    name: customer.id,
    owner: 'spring',
    creation: now,
    modified: now,
    modified_by: 'spring',
    docstatus: 0,
    customer_name: customer.displayName,
    status: customer.status === 'ACTIVE' ? 'active' : 'inactive',
    contact_person: customer.contactPerson,
    phone: customer.phone,
    email: customer.email,
  }
}
