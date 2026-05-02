import { describe, expect, it, vi } from 'vitest'
import { effectScope } from 'vue'
import { useCustomersSearch } from '@/api/composables/use-customers'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
}))

vi.mock('@/api/api-client', () => ({
  httpClient: {
    get: mocks.get,
  },
}))

describe('useCustomersSearch', () => {
  it('loads active customers from Spring API', async () => {
    mocks.get.mockResolvedValueOnce({
      data: {
        items: [
          {
            id: 'customer-1',
            displayName: 'ООО Ромашка',
            status: 'ACTIVE',
            contactPerson: 'Иван Иванов',
            phone: '+7',
            email: 'orders@example.test',
          },
        ],
      },
    })

    const scope = effectScope()
    const search = scope.run(() => useCustomersSearch({ onlyActive: true }))!
    await search.search('ром')
    scope.stop()

    expect(mocks.get).toHaveBeenCalledWith('/api/customers', {
      params: { search: 'ром', activeOnly: true, limit: 20 },
      signal: expect.any(AbortSignal),
    })
    expect(search.data.value).toMatchObject([
      {
        name: 'customer-1',
        customer_name: 'ООО Ромашка',
        status: 'active',
      },
    ])
  })
})
