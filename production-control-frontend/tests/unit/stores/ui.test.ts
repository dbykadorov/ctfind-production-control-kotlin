/**
 * Unit-тесты для useUiStore (007 / US2, T037):
 *  - дефолты темы (dark, ocean) при пустом storage;
 *  - persistence: setTheme / setSidebarPreset → запись в localStorage под theme.v1;
 *  - validation: невалидные значения сбрасываются на дефолт;
 *  - coexistence: запись в localStorage не затрагивает sessionStorage.
 */

import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { uiStoreInternals, useUiStore } from '@/stores/ui'

describe('useUiStore / theme defaults', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    window.localStorage.clear()
    window.sessionStorage.clear()
  })

  it('default: theme=dark, sidebarPreset=none при пустом storage (010 §R-008)', () => {
    // 010-cabinet-layout-rework §R-008: для новых пользователей default = 'none'
    // (solid var(--bg-app), Sidebar сливается с body / FR-002). Существующие
    // пользователи с сохранённым 'ocean'/'sunset'/etc. не затронуты — см. тест ниже.
    const ui = useUiStore()
    expect(ui.theme).toBe('dark')
    expect(ui.sidebarPreset).toBe('none')
  })

  it('setTheme/setSidebarPreset обновляют значения', () => {
    const ui = useUiStore()
    ui.setTheme('light')
    ui.setSidebarPreset('forest')
    expect(ui.theme).toBe('light')
    expect(ui.sidebarPreset).toBe('forest')
  })
})

describe('useUiStore / theme persistence', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    window.localStorage.clear()
    window.sessionStorage.clear()
  })

  it('после setTheme(light) → localStorage содержит {"theme":"light","sidebarPreset":"none"} (010 §R-008)', async () => {
    const ui = useUiStore()
    ui.setTheme('light')
    // Watcher срабатывает асинхронно — ждём микротаску.
    await Promise.resolve()
    await Promise.resolve()
    const raw = window.localStorage.getItem(uiStoreInternals.THEME_STORAGE_KEY)
    expect(raw).not.toBeNull()
    expect(JSON.parse(raw!)).toEqual({ theme: 'light', sidebarPreset: 'none' })
  })

  it('coexistence: запись theme в localStorage не затрагивает session-key UI', async () => {
    const ui = useUiStore()
    ui.setTheme('light')
    await Promise.resolve()
    await Promise.resolve()

    // theme живёт в localStorage…
    expect(window.localStorage.getItem(uiStoreInternals.THEME_STORAGE_KEY)).not.toBeNull()
    // …а старый session-key — пуст (его пишут только sidebar/filters).
    expect(window.sessionStorage.getItem(uiStoreInternals.STORAGE_KEY)).toBeNull()
  })
})

describe('useUiStore / theme validation', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    window.localStorage.clear()
    window.sessionStorage.clear()
  })

  it('невалидная theme в storage → reset в dark', () => {
    window.localStorage.setItem(
      uiStoreInternals.THEME_STORAGE_KEY,
      JSON.stringify({ theme: 'neon', sidebarPreset: 'ocean' }),
    )
    const ui = useUiStore()
    expect(ui.theme).toBe('dark')
  })

  it('невалидный sidebarPreset в storage → reset в none (010 §R-008)', () => {
    window.localStorage.setItem(
      uiStoreInternals.THEME_STORAGE_KEY,
      JSON.stringify({ theme: 'light', sidebarPreset: 'rainbow' }),
    )
    const ui = useUiStore()
    expect(ui.theme).toBe('light')
    expect(ui.sidebarPreset).toBe('none')
  })

  it('испорченный JSON в storage → дефолты без падения (010 §R-008)', () => {
    window.localStorage.setItem(uiStoreInternals.THEME_STORAGE_KEY, '{not-valid-json')
    const ui = useUiStore()
    expect(ui.theme).toBe('dark')
    expect(ui.sidebarPreset).toBe('none')
  })

  it('setTheme игнорирует невалидное значение', () => {
    const ui = useUiStore()
    ui.setTheme('dark')
    ui.setTheme('garbage' as 'dark')
    expect(ui.theme).toBe('dark')
  })

  it('setSidebarPreset игнорирует невалидное значение', () => {
    const ui = useUiStore()
    ui.setSidebarPreset('ocean')
    ui.setSidebarPreset('rainbow' as 'ocean')
    expect(ui.sidebarPreset).toBe('ocean')
  })

  it('010 §R-008: пресет "none" принимается как валидный', () => {
    const ui = useUiStore()
    ui.setSidebarPreset('none')
    expect(ui.sidebarPreset).toBe('none')
  })
})

describe('useUiStore / v1→v2 migration (010 post-MVP)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    window.localStorage.clear()
    window.sessionStorage.clear()
  })

  it('legacy v1 с preset="ocean" → мигрирует на "none" и удаляет v1-ключ', () => {
    // Эмулируем пользователя фичи 007/009: в localStorage v1 лежит
    // дефолтный 'ocean' (мог быть не осознанным выбором, поэтому переписываем).
    window.localStorage.setItem(
      uiStoreInternals.LEGACY_THEME_STORAGE_KEY_V1,
      JSON.stringify({ theme: 'dark', sidebarPreset: 'ocean' }),
    )

    const ui = useUiStore()

    expect(ui.sidebarPreset).toBe('none')
    expect(ui.theme).toBe('dark')
    // v1-ключ должен быть удалён, чтобы не мигрировать второй раз.
    expect(window.localStorage.getItem(uiStoreInternals.LEGACY_THEME_STORAGE_KEY_V1)).toBeNull()
    // v2-ключ должен быть записан с мигрированным состоянием.
    const v2 = window.localStorage.getItem(uiStoreInternals.THEME_STORAGE_KEY)
    expect(v2).not.toBeNull()
    expect(JSON.parse(v2!)).toEqual({ theme: 'dark', sidebarPreset: 'none' })
  })

  it('legacy v1 с preset="sunset" → выбор сохраняется (это осознанный override)', () => {
    window.localStorage.setItem(
      uiStoreInternals.LEGACY_THEME_STORAGE_KEY_V1,
      JSON.stringify({ theme: 'light', sidebarPreset: 'sunset' }),
    )

    const ui = useUiStore()

    expect(ui.sidebarPreset).toBe('sunset')
    expect(ui.theme).toBe('light')
    expect(window.localStorage.getItem(uiStoreInternals.LEGACY_THEME_STORAGE_KEY_V1)).toBeNull()
  })

  it('v2 имеет приоритет над v1 (если оба ключа существуют, v1 не трогаем)', () => {
    window.localStorage.setItem(
      uiStoreInternals.THEME_STORAGE_KEY,
      JSON.stringify({ theme: 'light', sidebarPreset: 'forest' }),
    )
    window.localStorage.setItem(
      uiStoreInternals.LEGACY_THEME_STORAGE_KEY_V1,
      JSON.stringify({ theme: 'dark', sidebarPreset: 'ocean' }),
    )

    const ui = useUiStore()

    expect(ui.sidebarPreset).toBe('forest')
    expect(ui.theme).toBe('light')
    // v1 не должен мигрироваться, если есть актуальный v2.
    expect(window.localStorage.getItem(uiStoreInternals.LEGACY_THEME_STORAGE_KEY_V1)).not.toBeNull()
  })

  it('battered v1 (битый JSON) → дефолты + v1 удалён', () => {
    window.localStorage.setItem(uiStoreInternals.LEGACY_THEME_STORAGE_KEY_V1, '{not-json')

    const ui = useUiStore()

    expect(ui.theme).toBe('dark')
    expect(ui.sidebarPreset).toBe('none')
    expect(window.localStorage.getItem(uiStoreInternals.LEGACY_THEME_STORAGE_KEY_V1)).toBeNull()
  })
})

describe('useUiStore / existing UI fields backward-compat', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    window.localStorage.clear()
    window.sessionStorage.clear()
  })

  it('toggleSidebar и dismissUnsupportedViewport продолжают работать', () => {
    const ui = useUiStore()
    expect(ui.sidebarCollapsed).toBe(false)
    ui.toggleSidebar()
    expect(ui.sidebarCollapsed).toBe(true)

    expect(ui.unsupportedViewportDismissed).toBe(false)
    ui.dismissUnsupportedViewport()
    expect(ui.unsupportedViewportDismissed).toBe(true)
  })

  it('setOrdersFilters клонирует и сохраняет фильтры', () => {
    const ui = useUiStore()
    ui.setOrdersFilters({ status: 'новый', activeOnly: true })
    expect(ui.lastOrdersFilters).toEqual({ status: 'новый', activeOnly: true })
  })
})
