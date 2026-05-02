/**
 * Unit-тесты AppShell.vue (010-cabinet-layout-rework, T018 / US1).
 *
 * Проверяем floating-card структуру: корневой `.cabinet-shell` с `bg-app-bg`,
 * внутри `<Sidebar>` слева и `.cabinet-card` справа; внутри card — `<TopBar>`
 * сверху и `<main>` с `<RouterView>` снизу. См. spec.md US1, contracts/design-tokens.
 */

import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import { createMemoryHistory, createRouter } from 'vue-router'
import AppShell from '@/components/layout/AppShell.vue'

// Sidebar/TopBar/UnsupportedViewport — мокаем (тестируем только структуру AppShell).
vi.mock('@/components/layout/Sidebar.vue', () => ({
  default: { template: '<aside data-testid="sidebar-mock" />' },
}))
vi.mock('@/components/layout/TopBar.vue', () => ({
  default: { template: '<header data-testid="topbar-mock" />' },
}))
vi.mock('@/components/layout/UnsupportedViewport.vue', () => ({
  default: { template: '<div data-testid="unsupported-mock" />' },
}))

function setupRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/cabinet', component: { template: '<div data-testid="dashboard-page">Dashboard</div>' } },
    ],
  })
}

function setupI18n() {
  return createI18n<false>({
    legacy: false,
    locale: 'ru',
    messages: { ru: {} } as unknown as Record<string, Record<string, string>>,
  })
}

describe('appShell — floating-card layout (010 US1)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  async function renderShell() {
    const router = setupRouter()
    await router.push('/cabinet')
    await router.isReady()
    return mount(AppShell, {
      global: { plugins: [router, setupI18n()] },
    })
  }

  it('корневой элемент имеет класс .cabinet-shell с bg-app-bg', async () => {
    const wrapper = await renderShell()
    const shell = wrapper.find('.cabinet-shell')
    expect(shell.exists()).toBe(true)
    expect(shell.classes()).toContain('bg-app-bg')
  })

  it('содержит floating-card обёртку .cabinet-card с margin', async () => {
    const wrapper = await renderShell()
    const card = wrapper.find('.cabinet-card')
    expect(card.exists()).toBe(true)
    // m-card-margin → Tailwind alias для var(--card-margin) — должен быть в classlist.
    expect(card.classes()).toContain('m-card-margin')
  })

  it('sidebar смонтирован как первый child shell-а', async () => {
    const wrapper = await renderShell()
    expect(wrapper.find('[data-testid="sidebar-mock"]').exists()).toBe(true)
  })

  it('topBar смонтирован внутри .cabinet-card', async () => {
    const wrapper = await renderShell()
    const card = wrapper.find('.cabinet-card')
    const topbar = card.find('[data-testid="topbar-mock"]')
    expect(topbar.exists()).toBe(true)
  })

  it('<main> с overflow-y-auto смонтирован внутри card', async () => {
    const wrapper = await renderShell()
    const main = wrapper.find('main')
    expect(main.exists()).toBe(true)
    expect(main.classes()).toContain('overflow-y-auto')
  })

  it('routerView рендерится внутри main и показывает текущую страницу', async () => {
    const wrapper = await renderShell()
    expect(wrapper.find('[data-testid="dashboard-page"]').exists()).toBe(true)
  })

  it('unsupportedViewport монтируется (фича 006 не сломана)', async () => {
    const wrapper = await renderShell()
    expect(wrapper.find('[data-testid="unsupported-mock"]').exists()).toBe(true)
  })

  it('структурный порядок: Sidebar перед .cabinet-card', async () => {
    const wrapper = await renderShell()
    const html = wrapper.html()
    const sidebarIdx = html.indexOf('data-testid="sidebar-mock"')
    const cardIdx = html.indexOf('cabinet-card')
    expect(sidebarIdx).toBeGreaterThan(-1)
    expect(cardIdx).toBeGreaterThan(-1)
    expect(sidebarIdx).toBeLessThan(cardIdx)
  })
})
