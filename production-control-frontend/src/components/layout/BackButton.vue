<script setup lang="ts">
/**
 * BackButton — кнопка «Назад» в TopBar Кабинета (010-cabinet-layout-rework, US3).
 *
 * Контракт: specs/010-cabinet-layout-rework/contracts/topbar-title-back.contract.md
 *
 * Приоритет навигации (см. §4):
 *   1. popPrev() из useNavigationStore (внутренний стек /cabinet/*)
 *   2. route.meta.backPath (статический fallback для deep-link сценариев)
 *   3. '/cabinet' (последний рубеж)
 *
 * Гарантии:
 *   - TBB-G1: НЕ используем router.go(-1) — только наш Pinia-стек.
 *   - TBB-G4: aria-label из i18n + title (browser tooltip).
 *
 * Без props и без emit: компонент полностью самодостаточный.
 */
import { ArrowLeft } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useNavigationStore } from '@/stores/navigation'

const router = useRouter()
const route = useRoute()
const nav = useNavigationStore()
const { t } = useI18n()

function onClick(): void {
  const prev = nav.popPrev()
  if (prev) {
    router.push(prev)
    return
  }
  const backPath = route.meta.backPath as string | undefined
  if (backPath) {
    router.push(backPath)
    return
  }
  router.push('/cabinet')
}
</script>

<template>
  <button
    type="button"
    class="cabinet-back-button"
    :aria-label="t('layout.backAria')"
    :title="t('layout.back')"
    @click="onClick"
  >
    <ArrowLeft class="size-5" aria-hidden="true" />
  </button>
</template>

<style scoped>
.cabinet-back-button {
  width: var(--topbar-icon-size);
  height: var(--topbar-icon-size);
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--c-fg-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  flex-shrink: 0;
  transition:
    background-color var(--duration) var(--ease-out),
    color var(--duration) var(--ease-out);
}

.cabinet-back-button:hover {
  background: var(--c-bg);
  color: var(--c-fg-strong);
}

.cabinet-back-button:focus-visible {
  outline: 2px solid var(--c-brand-500);
  outline-offset: 2px;
}
</style>
