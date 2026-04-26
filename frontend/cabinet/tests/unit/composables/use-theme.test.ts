/**
 * Unit-тесты для use-theme.ts (007 / US2, T036):
 *  - applyToDom выставляет атрибуты data-theme и data-sidebar-preset на <html>;
 *  - persistence: смена темы сохраняется в localStorage под ключом theme.v1;
 *  - prefersReducedMotion отражает значение window.matchMedia.
 */

import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope, nextTick } from 'vue'
import { useTheme } from '@/api/composables/use-theme'
import { uiStoreInternals } from '@/stores/ui'

describe('use-theme / DOM apply', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    document.documentElement.removeAttribute('data-theme')
    document.documentElement.removeAttribute('data-sidebar-preset')
    window.localStorage.clear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('initial: устанавливает дефолтную тёмную тему и пресет none на <html> (010 §R-008)', async () => {
    // 010-cabinet-layout-rework §R-008: новый default 'none' (solid var(--bg-app)).
    const scope = effectScope()
    scope.run(() => useTheme())
    await nextTick()
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
    expect(document.documentElement.getAttribute('data-sidebar-preset')).toBe('none')
    scope.stop()
  })

  it('setTheme(\'light\') обновляет атрибут <html data-theme>', async () => {
    const scope = effectScope()
    const { setTheme } = scope.run(() => useTheme())!
    setTheme('light')
    await nextTick()
    expect(document.documentElement.getAttribute('data-theme')).toBe('light')
    scope.stop()
  })

  it('setSidebarPreset(\'sunset\') обновляет атрибут <html data-sidebar-preset>', async () => {
    const scope = effectScope()
    const { setSidebarPreset } = scope.run(() => useTheme())!
    setSidebarPreset('sunset')
    await nextTick()
    expect(document.documentElement.getAttribute('data-sidebar-preset')).toBe('sunset')
    scope.stop()
  })

  it('persistence: смена темы → запись в localStorage под ключом theme.v1', async () => {
    const scope = effectScope()
    const { setTheme, setSidebarPreset } = scope.run(() => useTheme())!
    setTheme('light')
    setSidebarPreset('forest')
    await nextTick()
    const raw = window.localStorage.getItem(uiStoreInternals.THEME_STORAGE_KEY)
    expect(raw).not.toBeNull()
    const parsed = JSON.parse(raw!)
    expect(parsed).toEqual({ theme: 'light', sidebarPreset: 'forest' })
    scope.stop()
  })

  it('valid theme values: только dark / light, прочее игнорируется', async () => {
    const scope = effectScope()
    const { theme, setTheme } = scope.run(() => useTheme())!
    setTheme('dark')
    await nextTick()
    // Невалидное значение не должно менять текущую тему.
    setTheme('neon' as 'dark')
    await nextTick()
    expect(theme.value).toBe('dark')
    scope.stop()
  })
})

describe('use-theme / prefers-reduced-motion', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('prefersReducedMotion=true когда matchMedia говорит matches=true', () => {
    vi.stubGlobal('matchMedia', vi.fn().mockReturnValue({
      matches: true,
      media: '(prefers-reduced-motion: reduce)',
      onchange: null,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }))

    const scope = effectScope()
    const { prefersReducedMotion } = scope.run(() => useTheme())!
    expect(prefersReducedMotion.value).toBe(true)
    scope.stop()
  })

  it('prefersReducedMotion=false когда matchMedia говорит matches=false', () => {
    vi.stubGlobal('matchMedia', vi.fn().mockReturnValue({
      matches: false,
      media: '(prefers-reduced-motion: reduce)',
      onchange: null,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }))

    const scope = effectScope()
    const { prefersReducedMotion } = scope.run(() => useTheme())!
    expect(prefersReducedMotion.value).toBe(false)
    scope.stop()
  })
})
