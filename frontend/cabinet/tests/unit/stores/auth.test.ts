import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AUTH_TOKEN_STORAGE_KEY } from '@/api/auth-service'
import { useAuthStore } from '@/stores/auth'

const authServiceMocks = vi.hoisted(() => ({
  loginViaCabinet: vi.fn(),
  logoutFromCabinet: vi.fn(),
  fetchAuthenticatedUser: vi.fn(),
}))

vi.mock('@/api/auth-service', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/auth-service')>()
  return {
    ...actual,
    loginViaCabinet: authServiceMocks.loginViaCabinet,
    logoutFromCabinet: authServiceMocks.logoutFromCabinet,
    fetchAuthenticatedUser: authServiceMocks.fetchAuthenticatedUser,
  }
})

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    authServiceMocks.loginViaCabinet.mockReset()
    authServiceMocks.logoutFromCabinet.mockReset()
    authServiceMocks.fetchAuthenticatedUser.mockReset()
  })

  it('инициализируется из window.__BOOT__', () => {
    const auth = useAuthStore()
    expect(auth.user).toBe('test@example.com')
    expect(auth.roles).toContain('Order Manager')
    expect(auth.isAuthenticated).toBe(true)
  })

  it('markSessionExpired переводит isAuthenticated в false без сброса user', () => {
    const auth = useAuthStore()
    auth.markSessionExpired()
    expect(auth.sessionExpired).toBe(true)
    expect(auth.isAuthenticated).toBe(false)
    expect(auth.user).toBe('test@example.com')
  })

  it('clearSessionExpired восстанавливает isAuthenticated', () => {
    const auth = useAuthStore()
    auth.markSessionExpired()
    auth.clearSessionExpired()
    expect(auth.sessionExpired).toBe(false)
    expect(auth.isAuthenticated).toBe(true)
  })

  it('applyBoot обновляет user/roles/csrf', () => {
    const auth = useAuthStore()
    auth.applyBoot({
      user: 'admin@x',
      roles: ['Administrator'],
      language: 'ru',
      csrfToken: 'new-csrf',
      siteName: 'site',
      cabinetVersion: '0.1.0',
    })
    expect(auth.user).toBe('admin@x')
    expect(auth.csrfToken).toBe('new-csrf')
    expect(auth.permissions.isAdmin).toBe(true)
  })

  it('applyBoot с user="Guest" обнуляет user', () => {
    const auth = useAuthStore()
    auth.applyBoot({
      user: 'Guest',
      roles: [],
      language: 'ru',
      csrfToken: '',
      siteName: '',
      cabinetVersion: '0.0.0',
    })
    expect(auth.user).toBeNull()
    expect(auth.isAuthenticated).toBe(false)
  })

  it('login success сохраняет Bearer JWT и обновляет пользователя', async () => {
    authServiceMocks.loginViaCabinet.mockResolvedValueOnce({
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
    const assignSpy = vi.spyOn(window.location, 'assign').mockImplementation(() => {})

    const auth = useAuthStore()
    const outcome = await auth.login('admin', 'admin', '/cabinet/orders')

    expect(outcome.kind).toBe('success')
    expect(window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBe('jwt-admin')
    expect(auth.user).toBe('admin')
    expect(auth.roles).toEqual(['ADMIN'])
    expect(assignSpy).toHaveBeenCalledWith('/cabinet/orders')
  })

  it('bootstrapFromStoredToken populates auth state from /api/auth/me', async () => {
    window.localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, 'jwt-admin')
    authServiceMocks.fetchAuthenticatedUser.mockResolvedValueOnce({
      login: 'admin',
      displayName: 'Local Administrator',
      roles: ['ADMIN'],
      expiresAt: '2026-04-27T00:00:00Z',
    })

    const auth = useAuthStore()
    const restored = await auth.bootstrapFromStoredToken()

    expect(restored).toBe(true)
    expect(authServiceMocks.fetchAuthenticatedUser).toHaveBeenCalledWith('jwt-admin')
    expect(auth.user).toBe('admin')
    expect(auth.roles).toEqual(['ADMIN'])
  })

  it('logout удаляет Bearer JWT из localStorage', async () => {
    window.localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, 'jwt-admin')
    authServiceMocks.logoutFromCabinet.mockResolvedValueOnce(undefined)

    const auth = useAuthStore()
    await auth.logout()

    expect(window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBeNull()
    expect(auth.user).toBeNull()
    expect(auth.roles).toEqual([])
  })
})
