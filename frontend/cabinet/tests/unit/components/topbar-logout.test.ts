/**
 * Unit-тест logout-кнопки в TopBar.vue (009-cabinet-custom-login, T028).
 *
 * После клика по «Выход»:
 *   1) вызывается auth.logout() (мок Pinia store);
 *   2) выполняется window.location.assign('/cabinet/login') — НЕ '/login';
 *   3) даже если auth.logout() падает с ошибкой — редирект всё равно происходит
 *      (try/finally pattern, см. T030).
 */

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import { createMemoryHistory, createRouter } from 'vue-router'
import TopBar from '@/components/layout/TopBar.vue'

import { ru } from '@/i18n/ru'

const logoutMock = vi.fn()
const authState = {
  user: 'tester@example.com',
  logout: logoutMock,
}

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => authState,
}))

vi.mock('@/components/ui/SidebarPresetPicker.vue', () => ({
  default: { template: '<div />' },
}))
vi.mock('@/components/ui/ThemeSwitcher.vue', () => ({
  default: { template: '<div />' },
}))

function setupI18n() {
  // Минимально — нам нужен namespace `nav` для подписей.
  return createI18n<false>({
    legacy: false,
    locale: 'ru',
    fallbackLocale: 'ru',
    messages: {
      ru: {
        nav: { logout: 'Выйти', dashboard: '', orders: '', customers: '' },
        ui: ru.ui,
      },
    } as unknown as Record<string, Record<string, string>>,
  })
}

describe('topBar logout (009)', () => {
  let assignSpy: ReturnType<typeof vi.fn>

  beforeEach(() => {
    setActivePinia(createPinia())
    logoutMock.mockReset()
    assignSpy = vi.fn()
    vi.stubGlobal('location', { assign: assignSpy, href: '/cabinet', pathname: '/cabinet', search: '' })
  })

  async function clickLogout(wrapper: ReturnType<typeof mount>) {
    // Раскрываем меню — кнопка с aria-haspopup, потом ищем button с текстом «Выйти».
    await wrapper.find('button[aria-haspopup="true"]').trigger('click')
    const items = wrapper.findAll('button')
    const logoutBtn = items.find(b => b.text().includes('Выйти'))
    expect(logoutBtn).toBeTruthy()
    await logoutBtn!.trigger('click')
    await flushPromises()
  }

  // 010 US3: TopBar теперь использует useRoute() для title/back-button логики,
  // поэтому даже legacy-тест logout должен подключать router.
  function setupRouter() {
    return createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/cabinet', component: { template: '<div />' } }],
    })
  }

  it('успешный logout → window.location.assign("/cabinet/login")', async () => {
    logoutMock.mockResolvedValueOnce(undefined)

    const router = setupRouter()
    await router.push('/cabinet')
    await router.isReady()
    const wrapper = mount(TopBar, {
      global: { plugins: [setupI18n(), router] },
    })

    await clickLogout(wrapper)

    expect(logoutMock).toHaveBeenCalledTimes(1)
    expect(assignSpy).toHaveBeenCalledWith('/cabinet/login')
  })

  it('logout с ошибкой → редирект всё равно выполняется (try/finally)', async () => {
    logoutMock.mockRejectedValueOnce(new Error('Network'))

    const router = setupRouter()
    await router.push('/cabinet')
    await router.isReady()
    const wrapper = mount(TopBar, {
      global: { plugins: [setupI18n(), router] },
    })

    await clickLogout(wrapper)

    expect(assignSpy).toHaveBeenCalledWith('/cabinet/login')
  })
})
