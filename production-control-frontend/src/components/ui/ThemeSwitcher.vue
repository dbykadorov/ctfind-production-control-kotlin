<script setup lang="ts">
/**
 * 007 US2: Переключатель темы (тёмная / светлая) — radio-group с иконками.
 *
 * Привязан к `useTheme().theme` через `useUiStore.setTheme`.
 * Управление с клавиатуры — стандартное (label + input radio).
 */
import { useI18n } from 'vue-i18n'
import { Moon, Sun } from 'lucide-vue-next'
import { useTheme } from '@/api/composables/use-theme'
import type { CabinetTheme } from '@/stores/ui'

const { t } = useI18n()
const { theme, setTheme } = useTheme()

const OPTIONS: Array<{ value: CabinetTheme, label: string, icon: typeof Moon }> = [
  { value: 'dark', label: 'themeOptions.dark', icon: Moon },
  { value: 'light', label: 'themeOptions.light', icon: Sun },
]

function onChange(value: CabinetTheme): void {
  setTheme(value)
}
</script>

<template>
  <fieldset class="flex flex-col gap-2">
    <legend class="text-xs font-medium uppercase tracking-wider text-ink-muted">
      {{ t('ui.theme') }}
    </legend>
    <div class="flex gap-2" role="radiogroup" :aria-label="t('ui.theme')">
      <label
        v-for="opt in OPTIONS"
        :key="opt.value"
        :class="[
          'flex flex-1 cursor-pointer items-center gap-2 rounded border px-3 py-2 text-sm transition-colors',
          theme === opt.value
            ? 'border-brand-500 bg-brand-500/15 text-ink-strong'
            : 'border-border bg-surface text-ink-muted hover:border-brand-500/50',
        ]"
      >
        <input
          type="radio"
          name="cabinet-theme"
          class="sr-only"
          :value="opt.value"
          :checked="theme === opt.value"
          @change="onChange(opt.value)"
        />
        <component :is="opt.icon" class="size-4" aria-hidden="true" />
        <span>{{ t(`ui.${opt.label}`) }}</span>
      </label>
    </div>
  </fieldset>
</template>
