<script setup lang="ts">
/**
 * Overlay для FR-026a: показываем при viewport < 1024 px (mobile/tablet).
 * Пользователь может dismiss и продолжить (best-effort), но MVP не гарантирует
 * корректность UI на узких экранах.
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Button } from '@/components/ui'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()
const { t } = useI18n()

const tooNarrow = ref(false)
const MIN_WIDTH = 1024

function check(): void {
  if (typeof window === 'undefined')
    return
  tooNarrow.value = window.innerWidth < MIN_WIDTH
}

onMounted(() => {
  check()
  window.addEventListener('resize', check, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', check)
})
</script>

<template>
  <div
    v-if="tooNarrow && !ui.unsupportedViewportDismissed"
    class="fixed inset-x-0 bottom-0 z-30 border-t border-amber-200 bg-amber-50 px-6 py-4 shadow-elevated"
    role="status"
    aria-live="polite"
  >
    <div class="mx-auto flex max-w-3xl items-center justify-between gap-4">
      <p class="text-sm text-amber-900">
        {{ t('common.minViewport') }}
      </p>
      <Button variant="ghost" size="sm" @click="ui.dismissUnsupportedViewport()">
        {{ t('common.close') }}
      </Button>
    </div>
  </div>
</template>
