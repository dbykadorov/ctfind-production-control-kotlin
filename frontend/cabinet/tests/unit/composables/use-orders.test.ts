/**
 * Unit-тесты для use-orders.ts (T064 / US1):
 *  - построение фильтров для всех комбинаций OrderFilters;
 *  - построение or_filters для поиска;
 *  - публичные константы (page size, debounce) соответствуют контракту FR-014a.
 *
 * Тесты проверяют только чистую логику фильтров, не HTTP/realtime слой.
 */

import type { OrderFilters } from '@/api/types/domain'
import { describe, expect, it } from 'vitest'
import { ordersInternals } from '@/api/composables/use-orders'

const { LIST_PAGE_SIZE, REALTIME_DEBOUNCE_MS, buildFilterPayload, buildOrFilters } = ordersInternals

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
