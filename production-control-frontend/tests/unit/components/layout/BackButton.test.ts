/**
 * Unit-тесты BackButton.vue (010-cabinet-layout-rework, T031 / US3).
 *
 * Контракт: specs/010-cabinet-layout-rework/contracts/topbar-title-back.contract.md §8
 *   - click → router.push(popPrev), если popPrev !== null
 *   - click → router.push(meta.backPath), если popPrev === null и backPath задан
 *   - click → router.push('/cabinet'), если ни popPrev ни backPath
 *   - НИКОГДА не вызывает router.go(-1) (TBB-G1)
 *   - aria-label из i18n
 */

import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import { createMemoryHistory, createRouter, type RouteMeta } from 'vue-router'
import BackButton from '@/components/layout/BackButton.vue'
import { useNavigationStore } from '@/stores/navigation'

function setupI18n() {
  return createI18n<false>({
    legacy: false,
    locale: 'ru',
    messages: {
      ru: {
        layout: { back: 'Назад', backAria: 'Вернуться на предыдущую страницу' },
      },
    },
  })
}

async function renderButton(currentPath: string, meta?: RouteMeta) {
  setActivePinia(createPinia())
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/cabinet', component: { template: '<div />' } },
      { path: '/cabinet/orders', component: { template: '<div />' } },
      { path: '/cabinet/orders/:id', component: { template: '<div />' }, meta: meta ?? {} },
      { path: '/cabinet/customers', component: { template: '<div />' } },
    ],
  })
  await router.push(currentPath)
  await router.isReady()

  const pushSpy = vi.spyOn(router, 'push')
  const goSpy = vi.spyOn(router, 'go')

  const wrapper = mount(BackButton, {
    global: { plugins: [router, setupI18n()] },
  })
  return { wrapper, router, pushSpy, goSpy, nav: useNavigationStore() }
}

describe('backButton (010 US3)', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
  })

  it('aria-label берётся из i18n', async () => {
    const { wrapper } = await renderButton('/cabinet')
    const btn = wrapper.find('button')
    expect(btn.attributes('aria-label')).toBe('Вернуться на предыдущую страницу')
    expect(btn.attributes('title')).toBe('Назад')
  })

  it('click → router.push(popPrev), если стек ≥ 2', async () => {
    const { wrapper, pushSpy, nav } = await renderButton('/cabinet/orders/123')
    nav.push('/cabinet')
    nav.push('/cabinet/orders')
    nav.push('/cabinet/orders/123')
    await wrapper.find('button').trigger('click')
    expect(pushSpy).toHaveBeenCalledWith('/cabinet/orders')
  })

  it('click → router.push(meta.backPath), если popPrev === null', async () => {
    const { wrapper, pushSpy } = await renderButton('/cabinet/orders/123', {
      backPath: '/cabinet/orders',
    })
    await wrapper.find('button').trigger('click')
    expect(pushSpy).toHaveBeenCalledWith('/cabinet/orders')
  })

  it('click → fallback /cabinet, если ни popPrev ни backPath не дали результата', async () => {
    const { wrapper, pushSpy } = await renderButton('/cabinet/orders')
    await wrapper.find('button').trigger('click')
    expect(pushSpy).toHaveBeenCalledWith('/cabinet')
  })

  it('нИКОГДА не вызывает router.go(-1) (TBB-G1)', async () => {
    const { wrapper, goSpy, nav } = await renderButton('/cabinet/orders/123', {
      backPath: '/cabinet/orders',
    })
    nav.push('/cabinet')
    nav.push('/cabinet/orders/123')
    await wrapper.find('button').trigger('click')
    await wrapper.find('button').trigger('click')
    expect(goSpy).not.toHaveBeenCalled()
  })
})
