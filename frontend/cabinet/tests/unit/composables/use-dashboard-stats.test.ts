/**
 * Unit-тесты для use-dashboard-stats.ts (007 / US1, T026):
 *  - корректное маппирование 6 параллельных счётчиков → DashboardKpis + Distribution;
 *  - корректные filters в каждом запросе (status, today для overdue);
 *  - корректный расчёт процентов в распределении;
 *  - частичный сбой (один из 6 счётчиков упал) → весь KPI уходит в error,
 *    но не оставляет hung loading.
 *
 * Сетевые вызовы (frappeCall) и socket мокаются на уровне модулей.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope, nextTick } from 'vue'
import { dashboardStatsInternals, useDashboardStats } from '@/api/composables/use-dashboard-stats'

const { frappeCallMock, subscribeListUpdateMock } = vi.hoisted(() => ({
  frappeCallMock: vi.fn(),
  subscribeListUpdateMock: vi.fn(() => () => {}),
}))

vi.mock('@/api/frappe-client', () => ({
  frappeCall: (...args: unknown[]) => (frappeCallMock as (...a: unknown[]) => unknown)(...args),
}))

vi.mock('@/api/socket', () => ({
  subscribeListUpdate: (...args: unknown[]) => (subscribeListUpdateMock as (...a: unknown[]) => unknown)(...args),
}))

async function flushPromises(): Promise<void> {
  await nextTick()
  await Promise.resolve()
  await nextTick()
}

describe('use-dashboard-stats / constants', () => {
  it('debounce 1500мс, polling 30 секунд, статусы в порядке lifecycle', () => {
    expect(dashboardStatsInternals.REALTIME_DEBOUNCE_MS).toBe(1500)
    expect(dashboardStatsInternals.FALLBACK_POLL_MS).toBe(30_000)
    expect(dashboardStatsInternals.STATUS_ORDER).toEqual(['новый', 'в работе', 'готов', 'отгружен'])
  })
})

describe('use-dashboard-stats / data fetching', () => {
  beforeEach(() => {
    frappeCallMock.mockReset()
    subscribeListUpdateMock.mockClear()
    subscribeListUpdateMock.mockImplementation(() => () => {})
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('делает 6 параллельных счётчиков и корректно маппит в KPI + distribution', async () => {
    // Порядок ответов соответствует Promise.all в use-dashboard-stats.ts §1:
    // [totalActive, inProgress, ready, overdue, новый, отгружен]
    frappeCallMock.mockImplementation(() => Promise.resolve(0))
    frappeCallMock
      .mockResolvedValueOnce(50) // totalActive
      .mockResolvedValueOnce(20) // inProgress
      .mockResolvedValueOnce(15) // ready
      .mockResolvedValueOnce(5) // overdue
      .mockResolvedValueOnce(15) // новый
      .mockResolvedValueOnce(80) // отгружен

    const scope = effectScope()
    const result = scope.run(() => useDashboardStats())!
    await flushPromises()

    expect(result.kpis.value).toEqual({
      totalActive: 50,
      inProgress: 20,
      ready: 15,
      overdue: 5,
    })
    // distribution: новый=15, в работе=20, готов=15, отгружен=80; total=130.
    expect(result.distribution.value).toHaveLength(4)
    const map = Object.fromEntries(result.distribution.value.map(e => [e.status, e]))
    expect(map['новый']?.count).toBe(15)
    expect(map['в работе']?.count).toBe(20)
    expect(map['готов']?.count).toBe(15)
    expect(map['отгружен']?.count).toBe(80)
    // 15/130 ≈ 11.5; 20/130 ≈ 15.4; 15/130 ≈ 11.5; 80/130 ≈ 61.5
    expect(map['отгружен']?.percent ?? 0).toBeGreaterThan(60)
    expect(map['отгружен']?.percent ?? 0).toBeLessThan(62)
    scope.stop()
  })

  it('filter для overdue включает status != отгружен И delivery_date < today', async () => {
    frappeCallMock.mockResolvedValue(0)

    const scope = effectScope()
    scope.run(() => useDashboardStats())
    await flushPromises()

    // 4-й вызов (индекс 3) — это overdue.
    const overdueCall = frappeCallMock.mock.calls[3] as unknown[]
    expect(overdueCall[0]).toBe('frappe.client.get_count')
    const filters = (overdueCall[1] as { filters: unknown[] }).filters
    expect(filters).toHaveLength(2)
    expect(filters[0]).toEqual(['status', '!=', 'отгружен'])
    const f1 = filters[1] as unknown[]
    expect(f1[0]).toBe('delivery_date')
    expect(f1[1]).toBe('<')
    scope.stop()
  })

  it('всё распределение нулей → percent = 0 (без NaN)', async () => {
    frappeCallMock.mockResolvedValue(0)

    const scope = effectScope()
    const result = scope.run(() => useDashboardStats())!
    await flushPromises()

    expect(result.distribution.value.every(e => e.percent === 0)).toBe(true)
    expect(result.kpis.value).toEqual({ totalActive: 0, inProgress: 0, ready: 0, overdue: 0 })
    scope.stop()
  })

  it('один из счётчиков упал → ошибка во всём KPI, но loading сбрасывается', async () => {
    frappeCallMock
      .mockResolvedValueOnce(10)
      .mockResolvedValueOnce(5)
      .mockResolvedValueOnce(3)
      .mockRejectedValueOnce(new Error('PermissionError')) // overdue
      .mockResolvedValueOnce(2)
      .mockResolvedValueOnce(20)

    const scope = effectScope()
    const result = scope.run(() => useDashboardStats())!
    await flushPromises()

    expect(result.error.value).not.toBeNull()
    expect(result.loading.value).toBe(false)
    scope.stop()
  })

  it('подписывается на socket-канал Customer Order при mount', async () => {
    frappeCallMock.mockResolvedValue(0)

    const scope = effectScope()
    scope.run(() => useDashboardStats())
    await flushPromises()

    expect(subscribeListUpdateMock).toHaveBeenCalledWith(
      'Customer Order',
      expect.any(Function),
    )
    scope.stop()
  })

  it('socket-инвалидация дебаунсится: несколько событий → один refetch', async () => {
    frappeCallMock.mockResolvedValue(0)
    let socketCallback: (() => void) | null = null
    subscribeListUpdateMock.mockImplementation(((..._args: unknown[]) => {
      socketCallback = _args[1] as () => void
      return () => {}
    }) as unknown as () => () => void)

    // Fake-таймеры включаем ТОЛЬКО для debounce-окна — иначе polling-fallback (30с)
    // считается mock-ом и `runAllTimers` уходит в бесконечную рекурсию.
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })

    const scope = effectScope()
    scope.run(() => useDashboardStats())
    await flushPromises()

    const callsAfterInitial = frappeCallMock.mock.calls.length
    expect(socketCallback).not.toBeNull()

    socketCallback!()
    socketCallback!()
    socketCallback!()
    socketCallback!()
    socketCallback!()

    await vi.advanceTimersByTimeAsync(dashboardStatsInternals.REALTIME_DEBOUNCE_MS + 50)
    await flushPromises()

    // Один refetch = +6 вызовов (6 счётчиков).
    expect(frappeCallMock.mock.calls.length).toBe(callsAfterInitial + 6)
    scope.stop()
  })

  it('при dispose scope — отписка от socket выполняется', async () => {
    const unsubscribe = vi.fn()
    subscribeListUpdateMock.mockReturnValueOnce(unsubscribe)
    frappeCallMock.mockResolvedValue(0)

    const scope = effectScope()
    scope.run(() => useDashboardStats())
    await flushPromises()
    scope.stop()

    expect(unsubscribe).toHaveBeenCalled()
  })
})
