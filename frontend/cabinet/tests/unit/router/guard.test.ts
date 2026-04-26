/**
 * Unit-тесты navigation guard'а Кабинета (009-cabinet-custom-login, T027).
 *
 * Покрывает три новых правила:
 *   - already authenticated + to.name='login' → sanitizeFrom(from) ?? '/cabinet'
 *   - hasCabinetAccess=false + to.name != safe → {name:'no-modules'}
 *   - не публичный + не authenticated → {name:'login', query:{from}}
 *
 * Реализация guard'а живёт inline в src/router/index.ts; чтобы не плодить
 * экспортов, тестируем через прогон router.push() и проверку фактического
 * routed location.
 */

import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { router } from '@/router'

const authState = {
  isAuthenticated: true,
  hasCabinetAccess: true,
  roles: ['Order Manager'] as string[],
  permissions: { isAdmin: false },
  rememberRedirect: vi.fn(),
}

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => authState,
}))

beforeEach(async () => {
  setActivePinia(createPinia())
  authState.isAuthenticated = true
  authState.hasCabinetAccess = true
  authState.roles = ['Order Manager']
  authState.permissions = { isAdmin: false }
  authState.rememberRedirect = vi.fn()
  await router.replace('/cabinet')
  await router.isReady()
})

describe('router guard (009)', () => {
  it('правило 1: already authenticated + to=/cabinet/login → редирект на /cabinet', async () => {
    await router.push('/cabinet/login')
    expect(router.currentRoute.value.path).toBe('/cabinet')
  })

  it('правило 1: already authenticated + ?from=/cabinet/orders → редирект на /cabinet/orders (sanitized)', async () => {
    await router.push('/cabinet/login?from=/cabinet/orders')
    expect(router.currentRoute.value.path).toBe('/cabinet/orders')
  })

  it('правило 1: ?from с absolute URL → fallback на /cabinet (open redirect защита)', async () => {
    await router.push('/cabinet/login?from=https://evil.example.com/cabinet')
    expect(router.currentRoute.value.path).toBe('/cabinet')
  })

  it('правило 4: hasCabinetAccess=false + to=/cabinet/orders → редирект на /cabinet/no-modules', async () => {
    authState.hasCabinetAccess = false
    await router.push('/cabinet/orders')
    expect(router.currentRoute.value.path).toBe('/cabinet/no-modules')
  })

  it('правило 4: hasCabinetAccess=false + to=/cabinet/no-modules → пропускаем (без зацикливания)', async () => {
    authState.hasCabinetAccess = false
    await router.push('/cabinet/no-modules')
    expect(router.currentRoute.value.name).toBe('no-modules')
  })

  it('правило 3: не authenticated + to=/cabinet/orders → редирект на /cabinet/login?from=/cabinet/orders', async () => {
    authState.isAuthenticated = false
    authState.hasCabinetAccess = false
    await router.push('/cabinet/orders')
    expect(router.currentRoute.value.path).toBe('/cabinet/login')
    expect(router.currentRoute.value.query.from).toBe('/cabinet/orders')
    expect(authState.rememberRedirect).toHaveBeenCalledWith('/cabinet/orders')
  })

  it('правило 3: не authenticated + публичный route /cabinet/login → пропускаем', async () => {
    authState.isAuthenticated = false
    await router.push('/cabinet/login')
    expect(router.currentRoute.value.path).toBe('/cabinet/login')
  })

  it('правило 5 (RBAC): authenticated, без нужной роли → /cabinet/403 (forbidden)', async () => {
    authState.roles = ['Some Other Role']
    authState.hasCabinetAccess = true // допустим, имеет какую-то cabinet-роль
    await router.push('/cabinet/orders/new')
    expect(router.currentRoute.value.name).toBe('forbidden')
  })

  it('правило 5: admin (isAdmin=true) обходит RBAC', async () => {
    authState.permissions = { isAdmin: true }
    await router.push('/cabinet/orders/new')
    expect(router.currentRoute.value.name).toBe('orders.new')
  })
})
