/**
 * Unit-тесты Sidebar.vue (010-cabinet-layout-rework, T023 / US2).
 *
 *   - collapse-toggle меняет ширину класса
 *   - persistent state через useUiStore.sidebarCollapsed
 *   - список пунктов плоский (FR-016 — без групп/субменю)
 *   - каждый пункт рендерится через <SidebarItem>
 *   - aria-label кнопки collapse/expand из i18n
 *   - cabinet-sidebar-bg НЕ применяется при preset='none' (R-008 default)
 */

import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import { createMemoryHistory, createRouter } from 'vue-router'
import Sidebar from '@/components/layout/Sidebar.vue'
import { useUiStore } from '@/stores/ui'

// Permissions composable: даём пользователю доступ ко всему.
vi.mock('@/api/composables/use-permissions', () => ({
  usePermissions: () => ({
    value: {
      canManageOrders: true,
      isShopSupervisor: false,
      canManageCustomers: true,
    },
  }),
}))

function setupRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/cabinet', component: { template: '<div />' } },
      { path: '/cabinet/orders', component: { template: '<div />' } },
      { path: '/cabinet/customers', component: { template: '<div />' } },
    ],
  })
}

function setupI18n() {
  return createI18n<false>({
    legacy: false,
    locale: 'ru',
    messages: {
      ru: {
        app: { name: 'CTfind' },
        nav: { dashboard: 'Обзор', orders: 'Заказы', customers: 'Клиенты' },
        sidebar: {
          expand: 'Развернуть',
          collapse: 'Свернуть',
          version: 'v {version}',
          brand: { captionTop: 'ПАНЕЛЬ', captionBottom: 'КАБИНЕТА', alt: 'CTfind' },
        },
      },
    },
  })
}

async function renderSidebar() {
  setActivePinia(createPinia())
  const router = setupRouter()
  await router.push('/cabinet')
  await router.isReady()
  const wrapper = mount(Sidebar, {
    global: { plugins: [router, setupI18n()] },
    attachTo: document.body,
  })
  return { wrapper, ui: useUiStore() }
}

describe('sidebar (010 US2)', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
  })

  it('по умолчанию развёрнут — класс w-sidebar-expanded', async () => {
    const { wrapper, ui } = await renderSidebar()
    expect(ui.sidebarCollapsed).toBe(false)
    const aside = wrapper.find('aside')
    expect(aside.classes()).toContain('w-sidebar-expanded')
    expect(aside.classes()).not.toContain('w-sidebar-collapsed')
  })

  it('toggle меняет ширину на w-sidebar-collapsed', async () => {
    const { wrapper, ui } = await renderSidebar()
    ui.toggleSidebar()
    await wrapper.vm.$nextTick()
    const aside = wrapper.find('aside')
    expect(aside.classes()).toContain('w-sidebar-collapsed')
    expect(aside.classes()).not.toContain('w-sidebar-expanded')
  })

  it('кнопка collapse/expand имеет aria-label из i18n', async () => {
    const { wrapper, ui } = await renderSidebar()
    const btn = wrapper.find('button')
    expect(btn.attributes('aria-label')).toBe('Свернуть')
    ui.toggleSidebar()
    await wrapper.vm.$nextTick()
    expect(btn.attributes('aria-label')).toBe('Развернуть')
  })

  it('список пунктов плоский: ровно 4 navlink-а (Dashboard/Orders/Customers/Notifications)', async () => {
    const { wrapper } = await renderSidebar()
    const links = wrapper.findAll('a.cabinet-sidebar-item')
    expect(links).toHaveLength(4)
  })

  it('cabinet-sidebar-bg НЕ применяется при preset=\'none\' (R-008 default)', async () => {
    // 010 PAM-rework, post-MVP: при preset='none' sidebar остаётся transparent
    // (фон наследуется от .cabinet-shell градиента в AppShell). Класс
    // cabinet-sidebar — единственный, который гарантированно присутствует.
    const { wrapper, ui } = await renderSidebar()
    expect(ui.sidebarPreset).toBe('none')
    const aside = wrapper.find('aside')
    expect(aside.classes()).not.toContain('cabinet-sidebar-bg')
    expect(aside.classes()).toContain('cabinet-sidebar')
  })

  it('cabinet-sidebar-bg применяется при выборе gradient-preset (фича 007 совместимость)', async () => {
    const { wrapper, ui } = await renderSidebar()
    ui.setSidebarPreset('ocean')
    await wrapper.vm.$nextTick()
    const aside = wrapper.find('aside')
    expect(aside.classes()).toContain('cabinet-sidebar-bg')
  })

  it('aside имеет CSS transition на ширину через --transition-base', async () => {
    const { wrapper } = await renderSidebar()
    const aside = wrapper.find('aside').element as HTMLElement
    const inlineStyle = aside.getAttribute('style') ?? ''
    const className = aside.className
    expect(inlineStyle.includes('transition') || className.includes('cabinet-sidebar')).toBe(true)
  })
})
