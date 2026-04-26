<script setup lang="ts">
/**
 * 007 US2: Picker градиента сайдбара (5 пресетов).
 *
 * Каждая swatch-кнопка показывает превью linear-gradient пресета;
 * активный — выделен ring-ом и галочкой `Check` (lucide).
 * Aria-label берётся из i18n (`ui.sidebarPresets.<id>`).
 */
import { useI18n } from 'vue-i18n'
import { Check } from 'lucide-vue-next'
import { useTheme } from '@/api/composables/use-theme'
import type { CabinetSidebarPreset } from '@/stores/ui'
import { cn } from '@/lib/utils'

const { t } = useI18n()
const { sidebarPreset, setSidebarPreset } = useTheme()

interface PresetSwatch {
  id: CabinetSidebarPreset
  /** CSS gradient — должен совпадать с tokens.css `[data-sidebar-preset='<id>']`. */
  gradient: string
}

const PRESETS: PresetSwatch[] = [
  // 010 §R-008: 'none' = solid фон var(--bg-app), новый default. Превью —
  // сплошной navy/light в зависимости от темы (через CSS-переменную).
  { id: 'none',     gradient: 'var(--bg-app)' },
  { id: 'ocean',    gradient: 'linear-gradient(195deg, #1d8cf8, #3358f4)' },
  { id: 'sunset',   gradient: 'linear-gradient(195deg, #fd5d93, #ff8d72)' },
  { id: 'forest',   gradient: 'linear-gradient(195deg, #00f2c3, #1769b8)' },
  { id: 'twilight', gradient: 'linear-gradient(195deg, #ba54f5, #344675)' },
  { id: 'graphite', gradient: 'linear-gradient(195deg, #344675, #1e1e2f)' },
]
</script>

<template>
  <fieldset class="flex flex-col gap-2">
    <legend class="text-xs font-medium uppercase tracking-wider text-ink-muted">
      {{ t('ui.sidebarPreset') }}
    </legend>
    <div class="flex gap-2" role="radiogroup" :aria-label="t('ui.sidebarPreset')">
      <button
        v-for="p in PRESETS"
        :key="p.id"
        type="button"
        :class="cn(
          'relative flex h-10 w-10 items-center justify-center rounded transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500',
          sidebarPreset === p.id ? 'ring-2 ring-brand-500 ring-offset-2 ring-offset-surface' : 'opacity-80 hover:opacity-100',
        )"
        :style="{ background: p.gradient }"
        :aria-label="t(`ui.sidebarPresets.${p.id}`)"
        :aria-checked="sidebarPreset === p.id"
        role="radio"
        @click="setSidebarPreset(p.id)"
      >
        <Check
          v-if="sidebarPreset === p.id"
          class="size-4 text-white drop-shadow"
          aria-hidden="true"
        />
      </button>
    </div>
  </fieldset>
</template>
