<script setup lang="ts">
import { onErrorCaptured } from 'vue'
import { RouterView } from 'vue-router'
import { Toaster } from 'vue-sonner'
import { useTheme } from '@/api/composables/use-theme'
import { useAuthStore } from '@/stores/auth'

useAuthStore()
// 007 US2: глобально активируем синхронизацию theme/sidebarPreset из стора
// в data-* атрибуты <html>. Inline-script в index.html уже расставил начальные
// атрибуты, но смена темы пользователем должна реактивно применяться без reload.
useTheme()

onErrorCaptured((err) => {
  console.error('[cabinet] uncaught error', err)
  return false
})
</script>

<template>
  <RouterView />
  <Toaster
    position="top-right"
    rich-colors
    :toast-options="{ classes: { toast: 'cabinet-toast' } }"
  />
</template>

<style>
.cabinet-toast {
  font-family: var(--font-sans);
  border-radius: var(--radius);
}
</style>
