import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'

vi.mock('@/pages/common/SessionExpiredOverlay.vue', () => ({
  default: { template: '<div data-testid="session-expired-overlay" />' },
}))

vi.mock('@/api/composables/use-theme', () => ({
  useTheme: vi.fn(),
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn(),
}))

const { default: App } = await import('@/App.vue')

function mountApp() {
  return mount(App, {
    global: {
      stubs: {
        RouterView: { template: '<main data-testid="router-view" />' },
        Toaster: { template: '<div data-testid="toaster" />' },
      },
      plugins: [
        createI18n({
          legacy: false,
          locale: 'ru',
          messages: { ru: {} },
        }),
      ],
    },
  })
}

describe('App', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('does not mount the session-expired relogin overlay', () => {
    const wrapper = mountApp()

    expect(wrapper.find('[data-testid="router-view"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="session-expired-overlay"]').exists()).toBe(false)
  })
})
