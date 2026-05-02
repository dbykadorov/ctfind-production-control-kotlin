/**
 * Unit-тесты SidebarItem.vue (010-cabinet-layout-rework, T022 / US2).
 *
 * Контракт: specs/010-cabinet-layout-rework/contracts/sidebar-tooltip.contract.md §8
 *   - Рендер RouterLink с правильным to
 *   - В развёрнутом состоянии: label виден, tooltip НЕ видим (v-show=false)
 *   - В свёрнутом + не активен: aria-labelledby указывает на tooltip-id
 *   - Активный: aria-current=page, класс cabinet-sidebar-item--active
 *   - mouseenter в свёрнутом → tooltip становится visible
 *   - mouseleave → tooltip скрывается
 *   - focus в свёрнутом → tooltip visible
 *   - blur → tooltip скрывается
 *   - mouseenter в развёрнутом → tooltip НЕ показывается (ST-G1)
 *   - Position через getBoundingClientRect (mock)
 */

import { mount } from '@vue/test-utils'
import { LayoutDashboard } from 'lucide-vue-next'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { createMemoryHistory, createRouter } from 'vue-router'
import SidebarItem from '@/components/layout/SidebarItem.vue'
import { useUiStore } from '@/stores/ui'

function setupRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/cabinet', component: { template: '<div />' } },
      { path: '/cabinet/orders', component: { template: '<div />' } },
    ],
  })
}

function setupI18n() {
  return createI18n<false>({
    legacy: false,
    locale: 'ru',
    messages: {
      ru: {
        nav: { dashboard: 'Обзор', orders: 'Заказы' },
      },
    },
  })
}

async function renderItem(props: {
  to: string
  labelKey: string
  active?: boolean
  collapsed?: boolean
}) {
  setActivePinia(createPinia())
  const ui = useUiStore()
  if (props.collapsed)
    ui.sidebarCollapsed = true
  const router = setupRouter()
  await router.push('/cabinet')
  await router.isReady()
  const wrapper = mount(SidebarItem, {
    props: {
      to: props.to,
      icon: LayoutDashboard,
      labelKey: props.labelKey,
      active: props.active ?? false,
    },
    global: { plugins: [router, setupI18n()] },
    attachTo: document.body,
  })
  return { wrapper, ui }
}

describe('sidebarItem (010 US2)', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
  })

  it('рендерит <a> RouterLink с правильным href', async () => {
    const { wrapper } = await renderItem({ to: '/cabinet/orders', labelKey: 'nav.orders' })
    const a = wrapper.find('a')
    expect(a.exists()).toBe(true)
    expect(a.attributes('href')).toBe('/cabinet/orders')
  })

  it('в развёрнутом состоянии label виден, tooltip визуально скрыт', async () => {
    const { wrapper } = await renderItem({ to: '/cabinet/orders', labelKey: 'nav.orders' })
    const label = wrapper.find('.cabinet-sidebar-item__label')
    expect(label.exists()).toBe(true)
    expect(label.text()).toBe('Заказы')
    expect(label.classes()).not.toContain('cabinet-sidebar-item__label--hidden')

    const tooltip = document.body.querySelector('.cabinet-tooltip') as HTMLElement | null
    if (tooltip) {
      expect(tooltip.style.display).toBe('none')
    }
  })

  it('в свёрнутом состоянии: aria-labelledby указывает на tooltip-id', async () => {
    const { wrapper } = await renderItem({
      to: '/cabinet/orders',
      labelKey: 'nav.orders',
      collapsed: true,
    })
    const a = wrapper.find('a')
    const lbId = a.attributes('aria-labelledby')
    expect(lbId).toBeDefined()
    expect(lbId).toMatch(/^tt-/)
    const tooltip = document.body.querySelector(`#${lbId}`)
    expect(tooltip).not.toBeNull()
  })

  it('активный пункт получает aria-current=page и класс --active', async () => {
    const { wrapper } = await renderItem({
      to: '/cabinet/orders',
      labelKey: 'nav.orders',
      active: true,
    })
    const a = wrapper.find('a')
    expect(a.attributes('aria-current')).toBe('page')
    expect(a.classes()).toContain('cabinet-sidebar-item--active')
  })

  it('mouseenter в свёрнутом → tooltip visible', async () => {
    const { wrapper } = await renderItem({
      to: '/cabinet/orders',
      labelKey: 'nav.orders',
      collapsed: true,
    })
    await wrapper.find('a').trigger('mouseenter')
    const tooltip = document.body.querySelector('.cabinet-tooltip') as HTMLElement
    expect(tooltip).not.toBeNull()
    expect(tooltip.style.display).not.toBe('none')
  })

  it('mouseleave → tooltip скрывается', async () => {
    const { wrapper } = await renderItem({
      to: '/cabinet/orders',
      labelKey: 'nav.orders',
      collapsed: true,
    })
    const a = wrapper.find('a')
    await a.trigger('mouseenter')
    await a.trigger('mouseleave')
    const tooltip = document.body.querySelector('.cabinet-tooltip') as HTMLElement
    expect(tooltip.style.display).toBe('none')
  })

  it('focus в свёрнутом → tooltip visible (keyboard a11y)', async () => {
    const { wrapper } = await renderItem({
      to: '/cabinet/orders',
      labelKey: 'nav.orders',
      collapsed: true,
    })
    await wrapper.find('a').trigger('focus')
    const tooltip = document.body.querySelector('.cabinet-tooltip') as HTMLElement
    expect(tooltip).not.toBeNull()
    expect(tooltip.style.display).not.toBe('none')
  })

  it('blur → tooltip скрывается', async () => {
    const { wrapper } = await renderItem({
      to: '/cabinet/orders',
      labelKey: 'nav.orders',
      collapsed: true,
    })
    const a = wrapper.find('a')
    await a.trigger('focus')
    await a.trigger('blur')
    const tooltip = document.body.querySelector('.cabinet-tooltip') as HTMLElement
    expect(tooltip.style.display).toBe('none')
  })

  it('mouseenter в развёрнутом → tooltip НЕ показывается (ST-G1)', async () => {
    const { wrapper } = await renderItem({ to: '/cabinet/orders', labelKey: 'nav.orders' })
    await wrapper.find('a').trigger('mouseenter')
    const tooltip = document.body.querySelector('.cabinet-tooltip') as HTMLElement | null
    if (tooltip) {
      expect(tooltip.style.display).toBe('none')
    }
  })

  it('position вычисляется на основе getBoundingClientRect (right-aligned, vertical center)', async () => {
    const { wrapper } = await renderItem({
      to: '/cabinet/orders',
      labelKey: 'nav.orders',
      collapsed: true,
    })
    const a = wrapper.find('a').element as HTMLAnchorElement
    a.getBoundingClientRect = () =>
      ({
        top: 100,
        left: 0,
        right: 60,
        bottom: 140,
        width: 60,
        height: 40,
        x: 0,
        y: 100,
        toJSON: () => ({}),
      } as DOMRect)
    await wrapper.find('a').trigger('mouseenter')
    const tooltip = document.body.querySelector('.cabinet-tooltip') as HTMLElement
    expect(tooltip).not.toBeNull()
    expect(tooltip.style.left).toBe('68px')
    expect(tooltip.style.top).toBe('120px')
  })
})
