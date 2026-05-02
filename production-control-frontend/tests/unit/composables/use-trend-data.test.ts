/**
 * Unit-тесты для use-trend-data.ts (007 / US1, T027):
 *  - bucketByDay: правильное распределение строк по дням, нулевые дни заполняются;
 *  - computeDeltaPct: корректное вычисление дельты и null при делении на 0;
 *  - константы периодов соответствуют контракту dashboard-stats.contract.md §3.
 *
 * Тестируем только чистые helper-функции.
 */

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope, nextTick } from 'vue'
import { bucketByDay, computeDeltaPct, trendDataInternals, useTrendData } from '@/api/composables/use-trend-data'

const { subscribeListUpdateMock, getMock } = vi.hoisted(() => ({
  subscribeListUpdateMock: vi.fn(() => () => {}),
  getMock: vi.fn(),
}))

vi.mock('@/api/socket', () => ({
  subscribeListUpdate: (...args: unknown[]) => (subscribeListUpdateMock as (...a: unknown[]) => unknown)(...args),
}))

vi.mock('@/api/api-client', () => ({
  httpClient: {
    get: getMock,
  },
}))

async function flushPromises(): Promise<void> {
  await nextTick()
  await Promise.resolve()
  await nextTick()
}

describe('use-trend-data / constants', () => {
  it('тренд = 30 дней, fetch — 60 дней (для сравнения с предыдущим периодом)', () => {
    expect(trendDataInternals.TREND_DAYS).toBe(30)
    expect(trendDataInternals.TREND_FETCH_DAYS).toBe(60)
  })

  it('debounce = 1500 мс, polling fallback = 30 секунд', () => {
    expect(trendDataInternals.REALTIME_DEBOUNCE_MS).toBe(1500)
    expect(trendDataInternals.FALLBACK_POLL_MS).toBe(30_000)
  })
})

describe('use-trend-data / bucketByDay', () => {
  const fromDate = new Date(2026, 3, 1) // 1 апреля 2026 (месяцы — 0-индекс)

  it('возвращает массив длины days, все нули при отсутствии данных', () => {
    const result = bucketByDay([], fromDate, 5)
    expect(result).toHaveLength(5)
    expect(result.every(p => p.count === 0)).toBe(true)
    expect(result[0]?.date).toBe('2026-04-01')
    expect(result[4]?.date).toBe('2026-04-05')
  })

  it('правильно бакетит несколько строк в один день', () => {
    const rows = [
      { creation: '2026-04-01 10:00:00' },
      { creation: '2026-04-01 12:30:00' },
      { creation: '2026-04-02 09:15:00' },
    ]
    const result = bucketByDay(rows, fromDate, 3)
    expect(result[0]).toEqual({ date: '2026-04-01', count: 2 })
    expect(result[1]).toEqual({ date: '2026-04-02', count: 1 })
    expect(result[2]).toEqual({ date: '2026-04-03', count: 0 })
  })

  it('игнорирует строки без creation', () => {
    const rows = [
      { creation: '2026-04-01 10:00:00' },
      { creation: '' },
    ]
    const result = bucketByDay(rows, fromDate, 1)
    expect(result[0]?.count).toBe(1)
  })

  it('строки за пределами окна не попадают в результат, но не падают', () => {
    const rows = [
      { creation: '2026-04-01 10:00:00' },
      { creation: '2026-05-15 10:00:00' }, // вне окна
    ]
    const result = bucketByDay(rows, fromDate, 5)
    const total = result.reduce((acc, p) => acc + p.count, 0)
    expect(total).toBe(1)
  })
})

describe('use-trend-data / computeDeltaPct', () => {
  it('возвращает null когда предыдущий период == 0 (избегаем деления на 0)', () => {
    expect(computeDeltaPct(10, 0)).toBeNull()
    expect(computeDeltaPct(0, 0)).toBeNull()
  })

  it('положительная дельта при росте', () => {
    expect(computeDeltaPct(150, 100)).toBe(50)
  })

  it('отрицательная дельта при снижении', () => {
    expect(computeDeltaPct(50, 100)).toBe(-50)
  })

  it('округление до 1 знака после запятой', () => {
    expect(computeDeltaPct(123, 100)).toBe(23)
    expect(computeDeltaPct(1234, 1000)).toBe(23.4)
  })

  it('0% при равенстве', () => {
    expect(computeDeltaPct(100, 100)).toBe(0)
  })
})

describe('use-trend-data / Spring dashboard', () => {
  beforeEach(() => {
    subscribeListUpdateMock.mockClear()
    subscribeListUpdateMock.mockImplementation(() => () => {})
    getMock.mockReset()
  })

  it('maps dashboard trend created values to chart points and delta', async () => {
    getMock.mockResolvedValueOnce({
      data: {
        totalOrders: 0,
        activeOrders: 0,
        overdueOrders: 0,
        statusCounts: {},
        recentChanges: [],
        trend: [
          { date: '2026-03-01', created: 2, shipped: 0 },
          { date: '2026-03-02', created: 3, shipped: 0 },
        ],
      },
    })

    const scope = effectScope()
    const result = scope.run(() => useTrendData())!
    await flushPromises()
    scope.stop()

    expect(getMock).toHaveBeenCalledWith('/api/orders/dashboard', {
      signal: expect.any(AbortSignal),
    })
    expect(result.data.value).toEqual({
      points: [
        { date: '2026-03-01', count: 2 },
        { date: '2026-03-02', count: 3 },
      ],
      totalLast30: 5,
      totalPrev30: 0,
      delta30vsPrev30Pct: null,
    })
  })
})
