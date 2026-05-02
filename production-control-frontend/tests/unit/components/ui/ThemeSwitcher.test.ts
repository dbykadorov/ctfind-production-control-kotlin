import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import ThemeSwitcher from '@/components/ui/ThemeSwitcher.vue'

function setupI18n() {
  return createI18n<false>({
    legacy: false,
    locale: 'ru',
    messages: {
      ru: {
        ui: {
          theme: 'Тема',
          themeOptions: { dark: 'Темная', light: 'Светлая' },
        },
      },
    },
  })
}

describe('ThemeSwitcher', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    window.localStorage.clear()
    document.documentElement.setAttribute('data-theme', 'dark')
    document.documentElement.setAttribute('data-sidebar-preset', 'none')
  })

  it('active option in dark theme does not use light brand-50 background', () => {
    const wrapper = mount(ThemeSwitcher, {
      global: { plugins: [setupI18n()] },
    })

    const options = wrapper.findAll('label')
    expect(options).toHaveLength(2)
    const darkOption = options[0]
    expect(darkOption?.classes()).toContain('border-brand-500')
    expect(darkOption?.classes()).toContain('bg-brand-500/15')
    expect(darkOption?.classes()).not.toContain('bg-brand-50')
  })
})
