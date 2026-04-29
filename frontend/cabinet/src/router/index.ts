/**
 * Vue Router конфигурация Кабинета. Все маршруты под префиксом /cabinet/.
 * Lazy-чанки per-роль (US1=office, US2=auth, etc.).
 *
 * См. specs/006-spa-cabinet-ui/research.md §R-006, plan.md §Project Structure.
 */

import { createRouter, createWebHistory, type NavigationGuardWithThis, type RouteRecordRaw } from 'vue-router'
import { i18n } from '@/i18n'
import { useAuthStore } from '@/stores/auth'
import { useNavigationStore } from '@/stores/navigation'
import { sanitizeFrom } from '@/utils/url'

declare module 'vue-router' {
  /**
   * 010-cabinet-layout-rework — расширение `RouteMeta`:
   *   - `title` теперь содержит i18n-ключ (например `'meta.title.dashboard'`),
   *     а не локализованную строку (см. data-model §3 и contracts/topbar-title-back.md).
   *     Маршруты без `title` показывают пустой заголовок (TopBar fallback).
   *   - `showBackButton` — рендерить ли BackButton в TopBar (US3 / FR-021..FR-026).
   *   - `backPath` — fallback-путь для BackButton, если внутренний navigation-stack
   *     пуст (US3 / FR-023, см. contracts/navigation-store §6).
   */
  interface RouteMeta {
    public?: boolean
    roles?: string[]
    title?: string
    showBackButton?: boolean
    backPath?: string
  }
}

const AppShell = () => import('@/components/layout/AppShell.vue')

const routes: RouteRecordRaw[] = [
  {
    path: '/cabinet/login',
    name: 'login',
    component: () => import('@/pages/auth/LoginPage.vue'),
    meta: { public: true, title: 'meta.title.login' },
  },
  {
    path: '/cabinet',
    component: AppShell,
    children: [
      {
        path: '',
        name: 'dashboard',
        component: () => import('@/pages/office/DashboardPage.vue'),
        meta: { title: 'meta.title.dashboard' },
      },
      {
        path: 'orders',
        name: 'orders.list',
        component: () => import('@/pages/office/OrdersListPage.vue'),
        meta: {
          roles: ['Order Manager', 'Shop Supervisor', 'Order Corrector'],
          title: 'meta.title.orders.list',
        },
      },
      {
        path: 'orders/new',
        name: 'orders.new',
        component: () => import('@/pages/office/OrderNewPage.vue'),
        meta: {
          roles: ['Order Manager'],
          title: 'meta.title.orders.new',
          // 010 US3: страница глубже dashboard → показываем BackButton.
          showBackButton: true,
          backPath: '/cabinet/orders',
        },
      },
      {
        path: 'orders/:name',
        name: 'orders.detail',
        component: () => import('@/pages/office/OrderDetailPage.vue'),
        props: true,
        meta: {
          roles: ['Order Manager', 'Shop Supervisor', 'Order Corrector'],
          title: 'meta.title.orders.detail',
          showBackButton: true,
          backPath: '/cabinet/orders',
        },
      },
      {
        path: 'customers',
        name: 'customers.list',
        component: () => import('@/pages/office/CustomersListPage.vue'),
        meta: {
          roles: ['Order Manager'],
          title: 'meta.title.customers.list',
        },
      },
      {
        path: 'production-tasks',
        name: 'production-tasks.list',
        component: () => import('@/pages/production/ProductionTasksListPage.vue'),
        meta: {
          roles: ['Order Manager', 'Shop Supervisor', 'Executor', 'ORDER_MANAGER', 'PRODUCTION_SUPERVISOR', 'PRODUCTION_EXECUTOR'],
          title: 'meta.title.productionTasks.list',
        },
      },
      {
        path: 'production-tasks/board',
        name: 'production-tasks.board',
        component: () => import('@/pages/production/ProductionTasksBoardPage.vue'),
        meta: {
          roles: ['Order Manager', 'Shop Supervisor', 'Executor', 'ORDER_MANAGER', 'PRODUCTION_SUPERVISOR', 'PRODUCTION_EXECUTOR'],
          title: 'meta.title.productionTasks.board',
        },
      },
      {
        path: 'production-tasks/:id',
        name: 'production-tasks.detail',
        component: () => import('@/pages/production/ProductionTaskDetailPage.vue'),
        props: true,
        meta: {
          roles: ['Order Manager', 'Shop Supervisor', 'Executor', 'ORDER_MANAGER', 'PRODUCTION_SUPERVISOR', 'PRODUCTION_EXECUTOR'],
          title: 'meta.title.productionTasks.detail',
          showBackButton: true,
          backPath: '/cabinet/production-tasks',
        },
      },
      {
        path: 'notifications',
        name: 'notifications.list',
        component: () => import('@/pages/notifications/NotificationsPage.vue'),
        meta: { title: 'meta.title.notifications' },
      },
      {
        path: 'audit',
        name: 'audit.list',
        component: () => import('@/pages/audit/AuditLogPage.vue'),
        meta: {
          roles: ['ADMIN'],
          title: 'meta.title.audit',
        },
      },
      {
        path: '403',
        name: 'forbidden',
        component: () => import('@/pages/common/ForbiddenPage.vue'),
        meta: { title: 'meta.title.forbidden' },
      },
      {
        path: 'no-modules',
        name: 'no-modules',
        component: () => import('@/pages/common/NoModulesPage.vue'),
        meta: { title: 'meta.title.noModules' },
      },
      {
        path: ':path(.*)*',
        name: 'not-found',
        component: () => import('@/pages/common/NotFoundPage.vue'),
        meta: { title: 'meta.title.notFound' },
      },
    ],
  },
  {
    path: '/',
    redirect: '/cabinet',
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/cabinet',
  },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

/**
 * Главный navigation-guard Кабинета.
 *
 * Порядок правил (009-cabinet-custom-login, data-model.md §2.3):
 *
 * 1. Если пользователь УЖЕ аутентифицирован и идёт на `/cabinet/login`
 *    (`to.name === 'login'`) → редирект на `sanitizeFrom(to.query.from) ?? '/cabinet'`.
 *    Это защищает от «мерцания» формы у уже-залогиненного юзера (FR-009).
 * 2. Если route публичный (`meta.public === true`) — пропускаем как есть.
 *    Сюда же попадает Guest на `/cabinet/login`.
 * 3. Если не аутентифицирован — редирект на `{name:'login', query:{from}}` (FR-007).
 * 4. Если у пользователя нет ни одной cabinet-роли (и он не админ) — редирект
 *    на `/cabinet/no-modules` (FR-021), кроме случая, когда мы уже на
 *    `no-modules` или `forbidden` (чтобы не зациклить).
 * 5. Если route требует конкретные роли — проверка RBAC как раньше.
 */
const guard: NavigationGuardWithThis<undefined> = (to) => {
  const auth = useAuthStore()

  // (1) уже-залогиненный на login → выкидываем обратно (sanitized).
  if (to.name === 'login' && auth.isAuthenticated) {
    const safe = sanitizeFrom(to.query.from) ?? '/cabinet'
    return safe
  }

  if (to.meta.public)
    return true

  if (!auth.isAuthenticated) {
    auth.rememberRedirect(to.fullPath)
    return { name: 'login', query: { from: to.fullPath } }
  }

  // (4) залогинен, но без cabinet-ролей → no-modules.
  const SAFE_NAMES = new Set(['no-modules', 'forbidden', 'login'])
  if (!auth.hasCabinetAccess && !(typeof to.name === 'string' && SAFE_NAMES.has(to.name)))
    return { name: 'no-modules' }

  const required = to.meta.roles
  if (required && required.length > 0) {
    const allowed = [...required, 'Administrator', 'System Manager', 'ADMIN']
    const hasAccess = auth.permissions.isAdmin
      || allowed.some(role => auth.roles.includes(role))
    if (!hasAccess)
      return { name: 'forbidden' }
  }

  return true
}

router.beforeEach(guard)

router.afterEach((to) => {
  if (typeof document === 'undefined')
    return
  /**
   * 010 §R-007: title теперь хранится как i18n-ключ. Резолвим через i18n.global
   * с fallback (если ключа нет — показываем сам ключ как строку, чтобы не падать).
   * Логика выбора: te(key) → t(key) → key (как есть).
   */
  const titleKey = to.meta.title
  let resolved = ''
  if (titleKey && typeof titleKey === 'string') {
    const t = i18n.global.t
    const te = i18n.global.te
    resolved = te(titleKey) ? (t(titleKey) as string) : titleKey
  }
  document.title = resolved ? `${resolved} · Кабинет CTfind` : 'Кабинет CTfind'
})

/**
 * 010 US3 (T033): после успешной навигации записываем fullPath в навигационный
 * стек Pinia. Порядок важен — ПОСЛЕ document.title-хука, и в afterEach
 * (NS-G1: запись только при успешной навигации, не при отменённой beforeEach).
 * Сам store игнорирует пути вне `/cabinet/*`, `/cabinet/login` и дубликаты —
 * см. contracts/navigation-store §1 и §5.
 */
router.afterEach((to) => {
  useNavigationStore().push(to.fullPath)
})
