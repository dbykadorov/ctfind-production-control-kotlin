import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@/stores/auth'

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
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
      deskUrl: '/app',
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
      deskUrl: '/app',
      cabinetVersion: '0.0.0',
    })
    expect(auth.user).toBeNull()
    expect(auth.isAuthenticated).toBe(false)
  })
})
