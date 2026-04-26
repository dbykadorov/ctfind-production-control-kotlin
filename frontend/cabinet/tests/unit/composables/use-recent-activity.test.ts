/**
 * Unit-тесты для use-recent-activity.ts (007 / US1, T028):
 *  - константы (PAGE_SIZE = 10, debounce 1500мс, polling 30с) — соответствуют контракту;
 *  - корректный camelCase-маппинг ответа Frappe;
 *  - обработка ошибки (например, permission denied для Status Change);
 *  - корректная очистка при dispose scope (нет утечек таймеров).
 *
 * Сетевые вызовы (frappeCall) и socket мокаются на уровне модулей.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope, nextTick } from 'vue'
import { recentActivityInternals, useRecentActivity } from '@/api/composables/use-recent-activity'

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
  // Ждём цепочки промисов — несколько тиков на всякий случай (refetch + then + finally).
  await nextTick()
  await Promise.resolve()
  await nextTick()
}

describe('use-recent-activity / constants', () => {
  it('последние 10 записей, debounce 1500мс, fallback poll 30с', () => {
    expect(recentActivityInternals.PAGE_SIZE).toBe(10)
    expect(recentActivityInternals.REALTIME_DEBOUNCE_MS).toBe(1500)
    expect(recentActivityInternals.FALLBACK_POLL_MS).toBe(30_000)
  })
})

describe('use-recent-activity / data fetching', () => {
  beforeEach(() => {
    frappeCallMock.mockReset()
    subscribeListUpdateMock.mockClear()
    subscribeListUpdateMock.mockImplementation(() => () => {})
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('маппит snake_case ответ Frappe в camelCase RecentStatusChange', async () => {
    frappeCallMock.mockResolvedValueOnce([
      {
        name: 'CHG-001',
        order: 'CO-2026-0001',
        from_status: 'новый',
        to_status: 'в работе',
        actor_user: 'manager@ctfind.test',
        event_at: '2026-04-15 10:30:00',
      },
    ])

    const scope = effectScope()
    const result = scope.run(() => useRecentActivity())!
    await flushPromises()

    expect(result.data.value).toHaveLength(1)
    expect(result.data.value[0]).toEqual({
      name: 'CHG-001',
      order: 'CO-2026-0001',
      fromStatus: 'новый',
      toStatus: 'в работе',
      actorUser: 'manager@ctfind.test',
      eventAt: '2026-04-15 10:30:00',
    })
    scope.stop()
  })

  it('запрашивает order_by event_at desc и limit_page_length 10', async () => {
    frappeCallMock.mockResolvedValueOnce([])
    const scope = effectScope()
    scope.run(() => useRecentActivity())
    await flushPromises()

    expect(frappeCallMock).toHaveBeenCalledWith(
      'frappe.client.get_list',
      expect.objectContaining({
        doctype: 'Customer Order Status Change',
        order_by: 'event_at desc',
        limit_page_length: 10,
      }),
      expect.objectContaining({ method: 'GET' }),
    )
    scope.stop()
  })

  it('обрабатывает ошибку запроса (permission denied) — выставляет error, не падает', async () => {
    frappeCallMock.mockRejectedValueOnce(new Error('PermissionError'))

    const scope = effectScope()
    const result = scope.run(() => useRecentActivity())!
    await flushPromises()

    expect(result.error.value).not.toBeNull()
    expect(result.data.value).toEqual([])
    scope.stop()
  })

  it('пустой ответ → пустой массив без ошибки', async () => {
    frappeCallMock.mockResolvedValueOnce([])
    const scope = effectScope()
    const result = scope.run(() => useRecentActivity())!
    await flushPromises()

    expect(result.data.value).toEqual([])
    expect(result.error.value).toBeNull()
    scope.stop()
  })

  it('подписывается на socket-канал Customer Order Status Change', async () => {
    frappeCallMock.mockResolvedValueOnce([])
    const scope = effectScope()
    scope.run(() => useRecentActivity())
    await flushPromises()

    expect(subscribeListUpdateMock).toHaveBeenCalledWith(
      'Customer Order Status Change',
      expect.any(Function),
    )
    scope.stop()
  })

  it('при dispose scope — отписка от socket выполняется', async () => {
    const unsubscribe = vi.fn()
    subscribeListUpdateMock.mockReturnValueOnce(unsubscribe)
    frappeCallMock.mockResolvedValueOnce([])

    const scope = effectScope()
    scope.run(() => useRecentActivity())
    await flushPromises()
    scope.stop()

    expect(unsubscribe).toHaveBeenCalled()
  })
})
