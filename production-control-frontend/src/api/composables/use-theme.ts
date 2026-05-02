/**
 * 007-cabinet-dashboard-theme: композабл для управления темой и пресетом сайдбара.
 *
 * Тонкая обёртка над `useUiStore`: реактивные `theme`, `sidebarPreset`, сеттеры,
 * + auto-apply на `<html data-theme/data-sidebar-preset>` через `watchEffect` +
 * наблюдение `prefers-reduced-motion` (для отключения анимаций chart.js и т.п.).
 *
 * См. specs/007-cabinet-dashboard-theme/contracts/ui-preferences.contract.md.
 */

import { storeToRefs } from 'pinia'
import { computed, type ComputedRef, onScopeDispose, ref, type Ref, watchEffect } from 'vue'
import { type CabinetSidebarPreset, type CabinetTheme, useUiStore } from '@/stores/ui'

export interface UseThemeResult {
  theme: Ref<CabinetTheme>
  sidebarPreset: Ref<CabinetSidebarPreset>
  setTheme: (next: CabinetTheme) => void
  setSidebarPreset: (next: CabinetSidebarPreset) => void
  prefersReducedMotion: ComputedRef<boolean>
  applyToDom: () => void
}

function readReducedMotion(): boolean {
  if (typeof window === 'undefined' || !window.matchMedia)
    return false
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

export function useTheme(): UseThemeResult {
  const ui = useUiStore()
  const { theme, sidebarPreset } = storeToRefs(ui)

  const reducedMotionRef = ref(readReducedMotion())
  let mql: MediaQueryList | null = null
  let mqlListener: ((e: MediaQueryListEvent) => void) | null = null

  if (typeof window !== 'undefined' && window.matchMedia) {
    mql = window.matchMedia('(prefers-reduced-motion: reduce)')
    mqlListener = (e: MediaQueryListEvent) => {
      reducedMotionRef.value = e.matches
    }
    if (typeof mql.addEventListener === 'function') {
      mql.addEventListener('change', mqlListener)
    }
    else {
      (mql as MediaQueryList & { addListener: (cb: (e: MediaQueryListEvent) => void) => void })
        .addListener(mqlListener)
    }
  }

  function applyToDom(): void {
    if (typeof document === 'undefined')
      return
    const root = document.documentElement
    root.setAttribute('data-theme', theme.value)
    root.setAttribute('data-sidebar-preset', sidebarPreset.value)
  }

  // Реактивно поддерживаем атрибуты <html> в синхроне со стором.
  watchEffect(applyToDom)

  onScopeDispose(() => {
    if (mql && mqlListener) {
      if (typeof mql.removeEventListener === 'function') {
        mql.removeEventListener('change', mqlListener)
      }
      else {
        (mql as MediaQueryList & { removeListener: (cb: (e: MediaQueryListEvent) => void) => void })
          .removeListener(mqlListener)
      }
    }
  })

  return {
    theme,
    sidebarPreset,
    setTheme: ui.setTheme,
    setSidebarPreset: ui.setSidebarPreset,
    prefersReducedMotion: computed(() => reducedMotionRef.value),
    applyToDom,
  }
}
