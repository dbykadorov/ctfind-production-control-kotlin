/**
 * Unit-тесты TopBar.vue для US3 (010-cabinet-layout-rework, T030).
 *
 * Контракт: specs/010-cabinet-layout-rework/contracts/topbar-title-back.contract.md §8
 *   - title из slot (приоритет 1)
 *   - title из meta как i18n key (приоритет 2a)
 *   - title back-compat литеральная строка (приоритет 2b — te(key) === false)
 *   - BackButton рендерится при route.meta.showBackButton === true
 *   - BackButton НЕ рендерится при undefined / false
 *   - User dropdown работает (регрессия 007)
 */

import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import { createMemoryHistory, createRouter, type RouteRecordRaw } from 'vue-router'
import TopBar from '@/components/layout/TopBar.vue'

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    user: 'tester@example.com',
    logout: vi.fn(),
  }),
}))
vi.mock('@/components/ui/SidebarPresetPicker.vue', () => ({
  default: { template: '<div />' },
}))
vi.mock('@/components/ui/ThemeSwitcher.vue', () => ({
  default: { template: '<div />' },
}))

function setupI18n() {
  return createI18n<false>({
    legacy: false,
    locale: 'ru',
    messages: {
      ru: {
        nav: { logout: 'Выйти' },
        ui: { appearance: 'Внешний вид' },
        meta: { title: { dashboard: 'Обзор', orders: { list: 'Заказы' } } },
        layout: { back: 'Назад', backAria: 'Вернуться назад' },
      },
    } as unknown as Record<string, Record<string, string>>,
  })
}

interface MountOpts {
  path: string
  routes: RouteRecordRaw[]
  slots?: Record<string, string>
}

async function renderTopBar(opts: MountOpts) {
  setActivePinia(createPinia())
  const router = createRouter({ history: createMemoryHistory(), routes: opts.routes })
  await router.push(opts.path)
  await router.isReady()
  return mount(TopBar, {
    global: { plugins: [router, setupI18n()] },
    slots: opts.slots,
  })
}

describe('topBar US3 — title + back button (010)', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
  })

  it('рендерит titleText из route.meta.title как i18n key (TBB-G2)', async () => {
    const wrapper = await renderTopBar({
      path: '/cabinet',
      routes: [{ path: '/cabinet', component: { template: '<div />' }, meta: { title: 'meta.title.dashboard' } }],
    })
    const h1 = wrapper.find('h1')
    expect(h1.exists()).toBe(true)
    expect(h1.text()).toBe('Обзор')
  })

  it('рендерит titleText as-is, если te(key) === false (back-compat TBB-G3)', async () => {
    const wrapper = await renderTopBar({
      path: '/cabinet',
      routes: [{ path: '/cabinet', component: { template: '<div />' }, meta: { title: 'Legacy literal title' } }],
    })
    const h1 = wrapper.find('h1')
    expect(h1.exists()).toBe(true)
    expect(h1.text()).toBe('Legacy literal title')
  })

  it('использует slot#title если он передан (приоритет 1)', async () => {
    const wrapper = await renderTopBar({
      path: '/cabinet',
      routes: [{ path: '/cabinet', component: { template: '<div />' }, meta: { title: 'meta.title.dashboard' } }],
      slots: { title: '<h1 class="custom-title">Пользовательский заголовок</h1>' },
    })
    expect(wrapper.find('.custom-title').exists()).toBe(true)
    expect(wrapper.find('.custom-title').text()).toBe('Пользовательский заголовок')
    // Дефолтный <h1> с meta-title не рендерится
    const allH1 = wrapper.findAll('h1')
    expect(allH1).toHaveLength(1)
    expect(allH1[0]?.text()).toBe('Пользовательский заголовок')
  })

  it('backButton рендерится при meta.showBackButton === true', async () => {
    const wrapper = await renderTopBar({
      path: '/cabinet/orders/123',
      routes: [{
        path: '/cabinet/orders/:id',
        component: { template: '<div />' },
        meta: { title: 'meta.title.orders.list', showBackButton: true, backPath: '/cabinet/orders' },
      }],
    })
    expect(wrapper.find('.cabinet-back-button').exists()).toBe(true)
  })

  it('backButton НЕ рендерится при undefined / false', async () => {
    const wrapper = await renderTopBar({
      path: '/cabinet',
      routes: [{ path: '/cabinet', component: { template: '<div />' }, meta: { title: 'meta.title.dashboard' } }],
    })
    expect(wrapper.find('.cabinet-back-button').exists()).toBe(false)
  })

  it('регрессия (007/009): user dropdown открывается и закрывается', async () => {
    const wrapper = await renderTopBar({
      path: '/cabinet',
      routes: [{ path: '/cabinet', component: { template: '<div />' } }],
    })
    const trigger = wrapper.find('button[aria-haspopup="true"]')
    expect(trigger.exists()).toBe(true)
    expect(trigger.attributes('aria-expanded')).toBe('false')
    await trigger.trigger('click')
    expect(trigger.attributes('aria-expanded')).toBe('true')
    await trigger.trigger('click')
    expect(trigger.attributes('aria-expanded')).toBe('false')
  })
})
