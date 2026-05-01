import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import SidebarPresetPicker from '@/components/ui/SidebarPresetPicker.vue'

function setupI18n() {
  return createI18n<false>({
    legacy: false,
    locale: 'ru',
    messages: {
      ru: {
        ui: {
          sidebarPreset: 'Цвет сайдбара',
          sidebarPresets: {
            none: 'Без градиента',
            ocean: 'Океан',
            sunset: 'Закат',
            forest: 'Лес',
            twilight: 'Сумерки',
            graphite: 'Графит',
          },
        },
      },
    },
  })
}

describe('SidebarPresetPicker', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    window.localStorage.clear()
    document.documentElement.setAttribute('data-theme', 'dark')
    document.documentElement.setAttribute('data-sidebar-preset', 'none')
  })

  it('renders swatches in a fixed 6-column grid to avoid right overflow', () => {
    const wrapper = mount(SidebarPresetPicker, {
      global: { plugins: [setupI18n()] },
    })

    const radioGroup = wrapper.find('div[role="radiogroup"]')
    expect(radioGroup.exists()).toBe(true)
    expect(radioGroup.classes()).toContain('grid')
    expect(radioGroup.classes()).toContain('grid-cols-6')

    const buttons = wrapper.findAll('button[role="radio"]')
    expect(buttons).toHaveLength(6)
    for (const button of buttons) {
      expect(button.classes()).toContain('aspect-square')
      expect(button.classes()).toContain('w-full')
    }
  })
})
