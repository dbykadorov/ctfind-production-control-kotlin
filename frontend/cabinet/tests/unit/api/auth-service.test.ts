/**
 * Unit-тесты loginViaCabinet (009-cabinet-custom-login, T010).
 *
 * В новой Spring/Kotlin платформе реальная авторизация пока не подключена:
 *   - non-empty credentials → error.unavailable
 *   - empty fields → error.empty
 *   - Frappe `/api/method/login` не вызывается
 */

import { beforeEach, describe, expect, it, vi } from 'vitest'

const frappeCallMock = vi.fn()

vi.mock('@/api/frappe-client', () => ({
  frappeCall: frappeCallMock,
  onSessionExpired: vi.fn(),
}))

// ВАЖНО: импорт ПОСЛЕ vi.mock, чтобы внутренний `frappeCall` уже был замокан.
const { loginViaCabinet, logoutFromCabinet } = await import('@/api/auth-service')

describe('loginViaCabinet', () => {
  beforeEach(() => {
    frappeCallMock.mockReset()
  })

  it('пустой логин или пароль → error.empty без HTTP-вызова', async () => {
    expect(await loginViaCabinet('', 'pwd')).toEqual({ kind: 'error', messageKey: 'empty' })
    expect(await loginViaCabinet('user@x', '')).toEqual({ kind: 'error', messageKey: 'empty' })
    expect(frappeCallMock).not.toHaveBeenCalled()
  })

  it('non-empty credentials → error.unavailable без Frappe login HTTP-вызова', async () => {
    expect(await loginViaCabinet('user@x', 'pwd')).toEqual({
      kind: 'error',
      messageKey: 'unavailable',
    })
    expect(frappeCallMock).not.toHaveBeenCalled()
  })
})

describe('logoutFromCabinet', () => {
  beforeEach(() => {
    frappeCallMock.mockReset()
  })

  it('вызывает /api/method/logout POST', async () => {
    frappeCallMock.mockResolvedValueOnce({})

    await logoutFromCabinet()

    expect(frappeCallMock).toHaveBeenCalledWith('logout', {}, { method: 'POST' })
  })

  it('не пробрасывает ошибку, если frappeCall падает', async () => {
    frappeCallMock.mockRejectedValueOnce(new Error('Network down'))

    await expect(logoutFromCabinet()).resolves.toBeUndefined()
  })
})
