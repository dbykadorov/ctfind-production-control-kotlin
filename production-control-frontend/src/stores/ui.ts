/**
 * Pinia store: UI-преференсы Кабинета.
 *
 * Два уровня персистентности (см. 007 ui-preferences.contract.md):
 *   • sessionStorage `ctfind.cabinet.ui.v1` — sidebarCollapsed, lastOrdersFilters,
 *     dismiss-флаги (живут только в рамках вкладки/сессии).
 *   • localStorage   `ctfind.cabinet.theme.v1` — theme + sidebarPreset (живут
 *     между сессиями и доступны inline-скрипту в `index.html` для FOUC-prevention).
 *
 * См. specs/006-spa-cabinet-ui/data-model.md §3.2 и
 *     specs/007-cabinet-dashboard-theme/contracts/ui-preferences.contract.md.
 */

import type { OrderFilters } from '@/api/types/domain'
import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

const STORAGE_KEY = 'ctfind.cabinet.ui.v1'
const THEME_SWITCHING_CLASS = 'theme-switching'
/**
 * v2 (010-cabinet-layout-rework, post-MVP): меняем дефолтный sidebar preset с
 * 'ocean' на 'none' (solid var(--bg-app), сливается с body — соответствует PAM-эталону).
 * При наличии старого ключа `theme.v1` — мигрируем в v2:
 *   - если был sidebarPreset='ocean' (старый дефолт, мог не быть осознанным выбором)
 *     → переписываем на 'none';
 *   - остальные пресеты ('sunset','forest','twilight','graphite') считаем
 *     осознанным выбором и сохраняем как есть;
 *   - theme мигрируем как есть.
 * После миграции v1-ключ удаляется. Это гарантирует one-shot apply.
 */
const THEME_STORAGE_KEY = 'ctfind.cabinet.theme.v2'
const LEGACY_THEME_STORAGE_KEY_V1 = 'ctfind.cabinet.theme.v1'

export type CabinetTheme = 'dark' | 'light'
/**
 * 010-cabinet-layout-rework §R-008: добавлен пресет 'none' — solid фон var(--bg-app),
 * без gradient. Это новый default для пользователей без сохранённого значения, чтобы
 * Sidebar по умолчанию сливался с body (FR-002). Существующие пользователи с
 * сохранённым значением 'ocean'/'sunset'/etc. сохраняют свой выбор (override).
 */
export type CabinetSidebarPreset =
  | 'none'
  | 'ocean'
  | 'sunset'
  | 'forest'
  | 'twilight'
  | 'graphite'

const VALID_THEMES: readonly CabinetTheme[] = ['dark', 'light'] as const
const VALID_PRESETS: readonly CabinetSidebarPreset[] = [
  'none',
  'ocean',
  'sunset',
  'forest',
  'twilight',
  'graphite',
] as const

interface PersistedThemeState {
  theme: CabinetTheme
  sidebarPreset: CabinetSidebarPreset
}

/**
 * Парсит сохранённый JSON в PersistedThemeState с фоллбэком на дефолты при
 * битом/невалидном содержимом. Используется как для v2-чтения, так и для
 * v1-миграции (см. `migrateLegacyV1`).
 */
function parseTheme(raw: string | null): PersistedThemeState | null {
  if (!raw)
    return null
  try {
    const parsed = JSON.parse(raw) as Partial<PersistedThemeState>
    return {
      theme: VALID_THEMES.includes(parsed.theme as CabinetTheme)
        ? (parsed.theme as CabinetTheme)
        : 'dark',
      sidebarPreset: VALID_PRESETS.includes(
        parsed.sidebarPreset as CabinetSidebarPreset,
      )
        ? (parsed.sidebarPreset as CabinetSidebarPreset)
        : 'none',
    }
  }
  catch {
    return null
  }
}

/**
 * Одноразовая миграция v1→v2: читаем legacy-ключ, переписываем 'ocean' →
 * 'none' (см. комментарий к THEME_STORAGE_KEY), затем удаляем v1-ключ.
 * Возвращает мигрированное состояние или null если v1-ключа нет.
 */
function migrateLegacyV1(): PersistedThemeState | null {
  if (typeof window === 'undefined')
    return null
  const legacyRaw = window.localStorage.getItem(LEGACY_THEME_STORAGE_KEY_V1)
  if (!legacyRaw)
    return null
  const legacy = parseTheme(legacyRaw)
  // Удаляем legacy-ключ независимо от валидности — больше он нам не нужен.
  try {
    window.localStorage.removeItem(LEGACY_THEME_STORAGE_KEY_V1)
  }
  catch {
    /* storage недоступен — переживём */
  }
  if (!legacy)
    return null
  // ocean был старым дефолтом → перебиваем на новый дефолт 'none'.
  // Остальные пресеты — осознанный выбор пользователя, сохраняем.
  const migrated: PersistedThemeState = {
    theme: legacy.theme,
    sidebarPreset:
      legacy.sidebarPreset === 'ocean' ? 'none' : legacy.sidebarPreset,
  }
  return migrated
}

function loadThemeFromStorage(): PersistedThemeState {
  // 010 §R-008 (post-MVP): default = { dark, none } — sidebar сливается с body
  // (PAM-style единый блок).
  const defaults: PersistedThemeState = {
    theme: 'dark',
    sidebarPreset: 'none',
  }
  if (typeof window === 'undefined')
    return defaults
  // 1. Сначала пробуем v2 (актуальный ключ).
  const v2 = parseTheme(window.localStorage.getItem(THEME_STORAGE_KEY))
  if (v2)
    return v2
  // 2. v2 нет — пробуем мигрировать с v1 (для пользователей, которые
  //    запускали Кабинет с фичами 007/009 до этого обновления).
  const migrated = migrateLegacyV1()
  if (migrated) {
    // Сразу пишем мигрированное состояние в v2, чтобы при следующем запуске
    // не выполнять миграцию снова.
    saveThemeToStorage(migrated)
    return migrated
  }
  return defaults
}

function saveThemeToStorage(state: PersistedThemeState): void {
  if (typeof window === 'undefined')
    return
  try {
    window.localStorage.setItem(THEME_STORAGE_KEY, JSON.stringify(state))
  }
  catch {
    // Безмолвно игнорируем (storage может быть недоступен в private mode).
  }
}

/**
 * 015 (dark-theme-sync): во время смены темы временно отключаем цветовые
 * transition'ы, чтобы избежать "мигания" промежуточных состояний.
 * CSS-правила класса см. в `styles/globals.css` (`.theme-switching`).
 */
function suppressThemeTransitionsForOneTick(): void {
  if (typeof document === 'undefined')
    return
  const root = document.documentElement
  root.classList.add(THEME_SWITCHING_CLASS)
  const clear = () => root.classList.remove(THEME_SWITCHING_CLASS)

  if (typeof window === 'undefined') {
    clear()
    return
  }

  if (typeof window.requestAnimationFrame === 'function') {
    window.requestAnimationFrame(() => {
      window.requestAnimationFrame(clear)
    })
    return
  }

  window.setTimeout(clear, 0)
}

interface PersistedUiState {
  sidebarCollapsed: boolean
  lastOrdersFilters: OrderFilters | null
  unsupportedViewportDismissed: boolean
}

function loadFromStorage(): PersistedUiState {
  if (typeof window === 'undefined') {
    return {
      sidebarCollapsed: false,
      lastOrdersFilters: null,
      unsupportedViewportDismissed: false,
    }
  }
  try {
    const raw = window.sessionStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return {
        sidebarCollapsed: false,
        lastOrdersFilters: null,
        unsupportedViewportDismissed: false,
      }
    }
    const parsed = JSON.parse(raw) as Partial<PersistedUiState>
    return {
      sidebarCollapsed: !!parsed.sidebarCollapsed,
      lastOrdersFilters: parsed.lastOrdersFilters ?? null,
      unsupportedViewportDismissed: !!parsed.unsupportedViewportDismissed,
    }
  }
  catch {
    return {
      sidebarCollapsed: false,
      lastOrdersFilters: null,
      unsupportedViewportDismissed: false,
    }
  }
}

function saveToStorage(state: PersistedUiState): void {
  if (typeof window === 'undefined')
    return
  try {
    window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state))
  }
  catch {
    // Безмолвно игнорируем (storage может быть недоступен в private mode).
  }
}

export const useUiStore = defineStore('ui', () => {
  const initial = loadFromStorage()
  const initialTheme = loadThemeFromStorage()

  const sidebarCollapsed = ref(initial.sidebarCollapsed)
  const lastOrdersFilters = ref<OrderFilters | null>(initial.lastOrdersFilters)
  const unsupportedViewportDismissed = ref(
    initial.unsupportedViewportDismissed,
  )
  const theme = ref<CabinetTheme>(initialTheme.theme)
  const sidebarPreset = ref<CabinetSidebarPreset>(initialTheme.sidebarPreset)

  watch(
    [sidebarCollapsed, lastOrdersFilters, unsupportedViewportDismissed],
    () => {
      saveToStorage({
        sidebarCollapsed: sidebarCollapsed.value,
        lastOrdersFilters: lastOrdersFilters.value,
        unsupportedViewportDismissed: unsupportedViewportDismissed.value,
      })
    },
    { deep: true },
  )

  // Тема и пресет — отдельный watcher на отдельный storage-ключ (localStorage).
  watch([theme, sidebarPreset], () => {
    saveThemeToStorage({
      theme: theme.value,
      sidebarPreset: sidebarPreset.value,
    })
  })

  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setOrdersFilters(filters: OrderFilters): void {
    lastOrdersFilters.value = { ...filters }
  }

  function dismissUnsupportedViewport(): void {
    unsupportedViewportDismissed.value = true
  }

  function setTheme(next: CabinetTheme): void {
    if (!VALID_THEMES.includes(next))
      return
    if (theme.value === next)
      return
    suppressThemeTransitionsForOneTick()
    theme.value = next
  }

  function setSidebarPreset(next: CabinetSidebarPreset): void {
    if (!VALID_PRESETS.includes(next))
      return
    sidebarPreset.value = next
  }

  return {
    sidebarCollapsed,
    lastOrdersFilters,
    unsupportedViewportDismissed,
    theme,
    sidebarPreset,
    toggleSidebar,
    setOrdersFilters,
    dismissUnsupportedViewport,
    setTheme,
    setSidebarPreset,
  }
})

export const uiStoreInternals = {
  STORAGE_KEY,
  THEME_STORAGE_KEY,
  /** v1-ключ — экспортируется для unit-тестов миграции; не использовать в проде. */
  LEGACY_THEME_STORAGE_KEY_V1,
  THEME_SWITCHING_CLASS,
  VALID_THEMES,
  VALID_PRESETS,
  loadThemeFromStorage,
}
