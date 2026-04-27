/**
 * Unit-тесты для use-orders.ts (T064 / US1):
 *  - построение фильтров для всех комбинаций OrderFilters;
 *  - построение or_filters для поиска;
 *  - публичные константы (page size, debounce) соответствуют контракту FR-014a.
 *
 * Тесты проверяют только чистую логику фильтров, не HTTP/realtime слой.
 */

import type { OrderFilters } from '@/api/types/domain'
import { describe, expect, it, vi } from 'vitest'
import { effectScope, nextTick, ref } from 'vue'
import { ordersInternals, useOrder } from '@/api/composables/use-orders'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}))

vi.mock('@/api/api-client', () => ({
  httpClient: {
    get: mocks.get,
    post: mocks.post,
    put: mocks.put,
  },
}))

async function flushPromises(): Promise<void> {
  await nextTick()
  await Promise.resolve()
  await nextTick()
}

const {
  LIST_PAGE_SIZE,
  REALTIME_DEBOUNCE_MS,
  buildFilterPayload,
  buildOrFilters,
  buildOrderQueryParams,
  mapCreateOrderPayload,
  mapUpdateOrderPayload,
  mapOrderListRow,
} = ordersInternals

describe('use-orders / constants', () => {
  it('страница списка по 50 — соответствует FR-014a', () => {
    expect(LIST_PAGE_SIZE).toBe(50)
  })

  it('дебаунс realtime-обновлений = 500 мс', () => {
    expect(REALTIME_DEBOUNCE_MS).toBe(500)
  })
})

describe('use-orders / buildFilterPayload', () => {
  it('возвращает пустой массив для пустых фильтров', () => {
    expect(buildFilterPayload({})).toEqual({ filters: [] })
  })

  it('сериализует status как простое равенство', () => {
    const filters: OrderFilters = { status: 'в работе' }
    expect(buildFilterPayload(filters)).toEqual({
      filters: [['status', '=', 'в работе']],
    })
  })

  it('сериализует customer как простое равенство', () => {
    const filters: OrderFilters = { customer: 'CUST-0001' }
    expect(buildFilterPayload(filters)).toEqual({
      filters: [['customer', '=', 'CUST-0001']],
    })
  })

  it('сериализует диапазон delivery_date как два неравенства', () => {
    const filters: OrderFilters = { dateFrom: '2026-04-01', dateTo: '2026-04-30' }
    expect(buildFilterPayload(filters)).toEqual({
      filters: [
        ['delivery_date', '>=', '2026-04-01'],
        ['delivery_date', '<=', '2026-04-30'],
      ],
    })
  })

  it('комбинирует все фильтры в правильном порядке', () => {
    const filters: OrderFilters = {
      status: 'новый',
      customer: 'CUST-0001',
      dateFrom: '2026-04-01',
      dateTo: '2026-04-30',
    }
    const result = buildFilterPayload(filters) as { filters: unknown[] }
    expect(result.filters).toHaveLength(4)
    expect(result.filters[0]).toEqual(['status', '=', 'новый'])
    expect(result.filters[1]).toEqual(['customer', '=', 'CUST-0001'])
    expect(result.filters[2]).toEqual(['delivery_date', '>=', '2026-04-01'])
    expect(result.filters[3]).toEqual(['delivery_date', '<=', '2026-04-30'])
  })

  it('игнорирует поле search (его обрабатывает buildOrFilters)', () => {
    const filters: OrderFilters = { search: 'test' }
    expect(buildFilterPayload(filters)).toEqual({ filters: [] })
  })
})

describe('use-orders / buildOrFilters', () => {
  it('возвращает пустой объект, если поиск не задан', () => {
    expect(buildOrFilters(undefined)).toEqual({})
    expect(buildOrFilters('')).toEqual({})
  })

  it('строит or_filters с like-паттернами для name и customer', () => {
    expect(buildOrFilters('ABC')).toEqual({
      or_filters: [
        ['name', 'like', '%ABC%'],
        ['customer', 'like', '%ABC%'],
      ],
    })
  })

  it('сохраняет регистр поискового запроса', () => {
    expect(buildOrFilters('Заказ')).toEqual({
      or_filters: [
        ['name', 'like', '%Заказ%'],
        ['customer', 'like', '%Заказ%'],
      ],
    })
  })
})

describe('use-orders / Spring API mapping', () => {
  it('maps UI filters to Spring query params', () => {
    expect(buildOrderQueryParams({
      status: 'в работе',
      customer: 'customer-1',
      search: 'ORD',
      activeOnly: true,
      overdue: true,
      dateFrom: '2026-04-01',
      dateTo: '2026-04-30',
    }, 0, 50)).toEqual({
      status: 'IN_WORK',
      customerId: 'customer-1',
      search: 'ORD',
      activeOnly: true,
      overdueOnly: true,
      deliveryDateFrom: '2026-04-01',
      deliveryDateTo: '2026-04-30',
      page: 0,
      size: 50,
    })
  })

  it('maps backend list rows to existing cabinet order rows', () => {
    expect(mapOrderListRow({
      id: 'order-1',
      orderNumber: 'ORD-000001',
      customer: {
        id: 'customer-1',
        displayName: 'ООО Ромашка',
        status: 'ACTIVE',
      },
      deliveryDate: '2026-05-15',
      status: 'NEW',
      statusLabel: 'новый',
      createdAt: '2026-04-26T18:00:00Z',
      updatedAt: '2026-04-26T18:30:00Z',
      version: 2,
      overdue: false,
    })).toMatchObject({
      name: 'order-1',
      customer: 'customer-1',
      customer_name: 'ООО Ромашка',
      status: 'новый',
      delivery_date: '2026-05-15',
      modified: '2026-04-26T18:30:00Z',
    })
  })

  it('maps existing order form payload to Spring create request', () => {
    expect(mapCreateOrderPayload({
      customer: 'customer-1',
      delivery_date: '2026-05-15',
      notes: 'New order',
      items: [
        {
          name: 'row-1',
          owner: 'spring',
          creation: '2026-04-26T18:00:00Z',
          modified: '2026-04-26T18:00:00Z',
          modified_by: 'spring',
          docstatus: 0,
          item_name: 'Столешница',
          quantity: 2,
          uom: 'шт',
        },
      ],
    })).toEqual({
      customerId: 'customer-1',
      deliveryDate: '2026-05-15',
      notes: 'New order',
      items: [
        {
          itemName: 'Столешница',
          quantity: 2,
          uom: 'шт',
        },
      ],
    })
  })

  it('maps existing order edit payload to Spring update request with optimistic version', () => {
    expect(mapUpdateOrderPayload({
      customer: 'customer-1',
      delivery_date: '2026-05-20',
      notes: 'Updated order',
      items: [
        {
          name: 'row-1',
          owner: 'spring',
          creation: '2026-04-26T18:00:00Z',
          modified: '2026-04-26T18:00:00Z',
          modified_by: 'spring',
          docstatus: 0,
          item_name: 'Фасад',
          quantity: 3,
          uom: 'шт',
        },
      ],
    }, 2)).toEqual({
      expectedVersion: 2,
      customerId: 'customer-1',
      deliveryDate: '2026-05-20',
      notes: 'Updated order',
      items: [
        {
          itemName: 'Фасад',
          quantity: 3,
          uom: 'шт',
        },
      ],
    })
  })
})

describe('useOrder / update API', () => {
  it('saves order edits through Spring PUT with stored version', async () => {
    mocks.get.mockResolvedValueOnce({
      data: orderDetailResponse({ version: 2 }),
    })
    mocks.put.mockResolvedValueOnce({
      data: orderDetailResponse({ version: 3, notes: 'Updated order' }),
    })

    const scope = effectScope()
    const result = scope.run(() => useOrder(ref('order-1')))!
    await flushPromises()

    await result.save({
      customer: 'customer-1',
      delivery_date: '2026-05-20',
      notes: 'Updated order',
      items: [
        {
          name: 'row-1',
          owner: 'spring',
          creation: '2026-04-26T18:00:00Z',
          modified: '2026-04-26T18:00:00Z',
          modified_by: 'spring',
          docstatus: 0,
          item_name: 'Фасад',
          quantity: 3,
          uom: 'шт',
        },
      ],
    }, '2026-04-26T18:30:00Z')
    scope.stop()

    expect(mocks.put).toHaveBeenCalledWith('/api/orders/order-1', {
      expectedVersion: 2,
      customerId: 'customer-1',
      deliveryDate: '2026-05-20',
      notes: 'Updated order',
      items: [{ itemName: 'Фасад', quantity: 3, uom: 'шт' }],
    })
    expect(result.data.value?.notes).toBe('Updated order')
  })
})

function orderDetailResponse(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    id: 'order-1',
    orderNumber: 'ORD-000001',
    customer: {
      id: 'customer-1',
      displayName: 'ООО Ромашка',
      status: 'ACTIVE',
    },
    deliveryDate: '2026-05-15',
    status: 'NEW',
    statusLabel: 'новый',
    notes: 'Test order',
    items: [],
    history: [],
    changeDiffs: [],
    createdAt: '2026-04-26T18:00:00Z',
    updatedAt: '2026-04-26T18:30:00Z',
    version: 2,
    overdue: false,
    ...overrides,
  }
}
