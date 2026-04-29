<script setup lang="ts">
/**
 * TopBar — верхняя панель Кабинета (фичи 006/007/009/010).
 *
 * 010-cabinet-layout-rework US3:
 *   - Слева: BackButton (если route.meta.showBackButton) + заголовок страницы.
 *   - Источник заголовка (приоритет): slot#title → t(meta.title) → meta.title as-is → null.
 *   - Используется CSS-grid для зарезервированной колонки под BackButton (FR-026):
 *     grid-template-columns: var(--topbar-icon-size) auto.
 *   - Справа: user dropdown фич 007/009 — без изменений.
 *
 * Контракт: contracts/topbar-title-back.contract.md.
 */
import { ChevronDown, LogOut } from 'lucide-vue-next'
import { computed, onBeforeUnmount, onMounted, ref, useSlots } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import BackButton from '@/components/layout/BackButton.vue'
import NotificationBell from '@/components/domain/notifications/NotificationBell.vue'
import SidebarPresetPicker from '@/components/ui/SidebarPresetPicker.vue'
import ThemeSwitcher from '@/components/ui/ThemeSwitcher.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const { t, te } = useI18n()
const slots = useSlots()

const hasTitleSlot = computed(() => Boolean(slots.title))

// Заголовок: приоритет slot → te(key) → литеральная строка → null.
// te(key) === false → meta.title считаем уже готовой строкой (back-compat TBB-G3).
const titleText = computed<string | null>(() => {
  if (hasTitleSlot.value)
    return null
  const key = route.meta.title
  if (typeof key !== 'string' || key.length === 0)
    return null
  return te(key) ? (t(key) as string) : key
})

const showBackButton = computed(() => Boolean(route.meta.showBackButton))

const menuOpen = ref(false)
const menuRef = ref<HTMLElement | null>(null)

function toggleMenu(): void {
  menuOpen.value = !menuOpen.value
}

function closeMenu(): void {
  menuOpen.value = false
}

function onDocumentClick(e: MouseEvent): void {
  if (!menuOpen.value)
    return
  const target = e.target as Node | null
  if (!target || !menuRef.value)
    return
  if (!menuRef.value.contains(target))
    closeMenu()
}

function onKeydown(e: KeyboardEvent): void {
  if (e.key === 'Escape')
    closeMenu()
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
  document.removeEventListener('keydown', onKeydown)
})

async function logout(): Promise<void> {
  closeMenu()
  try {
    await auth.logout()
  }
  catch (err) {
    console.warn('[cabinet] logout request failed; redirecting anyway:', err)
  }
  window.location.assign('/cabinet/login')
}
</script>

<template>
  <header
    class="cabinet-topbar flex h-header items-center justify-between border-b border-border bg-surface px-6"
  >
    <!-- Левая часть: BackButton + title (slot или meta).
         min-w-0 + truncate в .cabinet-topbar__title чтобы длинный заголовок
         не выталкивал правую часть. -->
    <div class="cabinet-topbar__left flex min-w-0 flex-1 items-center gap-3">
      <slot name="brand" />
      <BackButton v-if="showBackButton" />
      <slot name="title">
        <h1 v-if="titleText" class="cabinet-topbar__title truncate text-lg font-semibold text-ink-strong">
          {{ titleText }}
        </h1>
      </slot>
    </div>

    <div ref="menuRef" class="relative flex items-center gap-3">
      <slot name="actions" />
      <NotificationBell />
      <button
        type="button"
        class="flex items-center gap-2 rounded px-2 py-1 text-sm text-ink-strong transition-colors hover:bg-bg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
        :aria-expanded="menuOpen"
        :aria-haspopup="true"
        @click="toggleMenu"
      >
        <span class="flex size-8 items-center justify-center rounded-full bg-brand-500 text-xs font-medium text-white">
          {{ (auth.user || '?').slice(0, 1).toUpperCase() }}
        </span>
        <span class="hidden sm:inline">{{ auth.user || '—' }}</span>
        <ChevronDown class="size-4 text-ink-muted" />
      </button>

      <div
        v-if="menuOpen"
        class="absolute right-0 top-full z-30 mt-2 w-72 rounded border border-border bg-elevated p-3 shadow-elevated"
        role="menu"
      >
        <section class="mb-3 flex flex-col gap-3 border-b border-border pb-3">
          <p class="text-xs font-medium uppercase tracking-wider text-ink-muted">
            {{ t('ui.appearance') }}
          </p>
          <ThemeSwitcher />
          <SidebarPresetPicker />
        </section>

        <button
          type="button"
          class="flex w-full items-center gap-2 rounded-sm px-2 py-1.5 text-left text-sm text-ink hover:bg-bg"
          role="menuitem"
          @click="logout"
        >
          <LogOut class="size-4 text-ink-muted" />
          {{ t('nav.logout') }}
        </button>
      </div>
    </div>
  </header>
</template>
