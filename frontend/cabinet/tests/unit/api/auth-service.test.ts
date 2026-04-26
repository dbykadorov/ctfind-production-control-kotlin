/**
 * Unit-тесты loginViaCabinet (009-cabinet-custom-login, T010).
 *
 * В новой Spring/Kotlin платформе:
 *   - valid credentials → POST /api/auth/login → success payload
 *   - empty fields → error.empty
 *   - Frappe `/api/method/login` не вызывается
 */

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AUTH_TOKEN_STORAGE_KEY } from '@/api/auth-service'

const authClientMocks = vi.hoisted(() => ({
  frappeCall: vi.fn(),
  post: vi.fn(),
}))

vi.mock('@/api/frappe-client', () => ({
  frappeCall: authClientMocks.frappeCall,
  httpClient: {
    post: authClientMocks.post,
  },
  onSessionExpired: vi.fn(),
}))

// ВАЖНО: импорт ПОСЛЕ vi.mock, чтобы внутренний `frappeCall` уже был замокан.
const { loginViaCabinet, logoutFromCabinet } = await import('@/api/auth-service')

describe('loginViaCabinet', () => {
  beforeEach(() => {
    authClientMocks.frappeCall.mockReset()
    authClientMocks.post.mockReset()
  })

  it('пустой логин или пароль → error.empty без HTTP-вызова', async () => {
    expect(await loginViaCabinet('', 'pwd')).toEqual({ kind: 'error', messageKey: 'empty' })
    expect(await loginViaCabinet('user@x', '')).toEqual({ kind: 'error', messageKey: 'empty' })
    expect(authClientMocks.frappeCall).not.toHaveBeenCalled()
    expect(authClientMocks.post).not.toHaveBeenCalled()
  })

  it('valid credentials → success payload без Frappe login HTTP-вызова', async () => {
    authClientMocks.post.mockResolvedValueOnce({
      data: {
        tokenType: 'Bearer',
        accessToken: 'jwt-admin',
        expiresAt: '2026-04-27T00:00:00Z',
        user: {
          login: 'admin',
          displayName: 'Local Administrator',
          roles: ['ADMIN'],
        },
      },
    })

    expect(await loginViaCabinet('admin', 'admin')).toEqual({
      kind: 'success',
      tokenType: 'Bearer',
      accessToken: 'jwt-admin',
      expiresAt: '2026-04-27T00:00:00Z',
      user: {
        login: 'admin',
        displayName: 'Local Administrator',
        roles: ['ADMIN'],
      },
    })
    expect(authClientMocks.post).toHaveBeenCalledWith('/api/auth/login', { login: 'admin', password: 'admin' })
    expect(authClientMocks.frappeCall).not.toHaveBeenCalled()
  })

  it('invalid credentials → error.invalid без authenticated state', async () => {
    authClientMocks.post.mockRejectedValueOnce({ response: { status: 401 } })

    expect(await loginViaCabinet('admin', 'wrong')).toEqual({
      kind: 'error',
      messageKey: 'invalid',
    })
    expect(window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBeNull()
  })

  it('throttled credentials → error.rateLimit без authenticated state', async () => {
    authClientMocks.post.mockRejectedValueOnce({ response: { status: 429 } })

    expect(await loginViaCabinet('admin', 'wrong')).toEqual({
      kind: 'error',
      messageKey: 'rateLimit',
    })
    expect(window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBeNull()
  })
})

describe('logoutFromCabinet', () => {
  beforeEach(() => {
    authClientMocks.post.mockReset()
  })

  it('вызывает /api/auth/logout POST', async () => {
    authClientMocks.post.mockResolvedValueOnce({})

    await logoutFromCabinet()

    expect(authClientMocks.post).toHaveBeenCalledWith('/api/auth/logout', {})
  })

  it('не пробрасывает ошибку, если logout API падает', async () => {
    authClientMocks.post.mockRejectedValueOnce(new Error('Network down'))

    await expect(logoutFromCabinet()).resolves.toBeUndefined()
  })
})
