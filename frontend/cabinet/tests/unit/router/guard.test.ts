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
  }, 10_000)

  it('правило 1: already authenticated + ?from=/cabinet/orders → редирект на /cabinet/orders (sanitized)', async () => {
    await router.push('/cabinet/login?from=/cabinet/orders')
    expect(router.currentRoute.value.path).toBe('/cabinet/orders')
  }, 10_000)

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

  it('правило 5: backend ADMIN role обходит RBAC', async () => {
    authState.roles = ['ADMIN']
    authState.permissions = { isAdmin: false }
    await router.push('/cabinet/orders/new')
    expect(router.currentRoute.value.name).toBe('orders.new')
  })

  // T086: production-task route access for manager / supervisor / executor / admin
  describe('production-tasks routes (T086)', () => {
    it('ADMIN backend role can open the production task list', async () => {
      authState.roles = ['ADMIN']
      authState.permissions = { isAdmin: false }
      await router.push('/cabinet/production-tasks')
      expect(router.currentRoute.value.name).toBe('production-tasks.list')
    })

    it('isAdmin=true bypasses RBAC for production tasks', async () => {
      authState.roles = []
      authState.permissions = { isAdmin: true }
      await router.push('/cabinet/production-tasks')
      expect(router.currentRoute.value.name).toBe('production-tasks.list')
    })

    it('PRODUCTION_SUPERVISOR backend role can open production tasks', async () => {
      authState.roles = ['PRODUCTION_SUPERVISOR']
      authState.permissions = { isAdmin: false }
      await router.push('/cabinet/production-tasks')
      expect(router.currentRoute.value.name).toBe('production-tasks.list')
    })

    it('PRODUCTION_EXECUTOR backend role can open production tasks', async () => {
      authState.roles = ['PRODUCTION_EXECUTOR']
      authState.permissions = { isAdmin: false }
      await router.push('/cabinet/production-tasks')
      expect(router.currentRoute.value.name).toBe('production-tasks.list')
    })

    it('ORDER_MANAGER backend role can open production tasks', async () => {
      authState.roles = ['ORDER_MANAGER']
      authState.permissions = { isAdmin: false }
      await router.push('/cabinet/production-tasks')
      expect(router.currentRoute.value.name).toBe('production-tasks.list')
    })

    it('a role outside the production-tasks allowlist gets sent to /cabinet/403', async () => {
      authState.roles = ['Some Other Role']
      authState.permissions = { isAdmin: false }
      authState.hasCabinetAccess = true
      await router.push('/cabinet/production-tasks')
      expect(router.currentRoute.value.name).toBe('forbidden')
    })

    it('PRODUCTION_EXECUTOR can open the production task detail route', async () => {
      authState.roles = ['PRODUCTION_EXECUTOR']
      authState.permissions = { isAdmin: false }
      await router.push('/cabinet/production-tasks/abc-123')
      expect(router.currentRoute.value.name).toBe('production-tasks.detail')
      expect(router.currentRoute.value.params.id).toBe('abc-123')
    })
  })
})
