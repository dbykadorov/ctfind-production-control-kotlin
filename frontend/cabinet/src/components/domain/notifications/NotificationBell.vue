<script setup lang="ts">
import { Bell } from 'lucide-vue-next'
import { computed, ref, watch } from 'vue'
import { Popover } from '@/components/ui/popover'
import NotificationDropdown from './NotificationDropdown.vue'
import { useNotificationStore } from '@/stores/notifications'

const store = useNotificationStore()
const isOpen = ref(false)

const badgeText = computed(() => {
  if (store.unreadCount <= 0) return null
  return store.unreadCount > 99 ? '99+' : String(store.unreadCount)
})

watch(isOpen, (open) => {
  if (open) {
    store.fetchDropdown()
  }
})
</script>

<template>
  <Popover v-model:open="isOpen" align="end">
    <template #trigger>
      <button
        type="button"
        class="relative flex items-center justify-center rounded p-1.5 text-ink-muted transition-colors hover:bg-bg hover:text-ink-strong focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
        :aria-label="$t('nav.notifications')"
      >
        <Bell class="size-5" />
        <span
          v-if="badgeText"
          class="absolute -right-1 -top-1 flex min-w-[1.125rem] items-center justify-center rounded-full bg-red-500 px-1 text-[0.625rem] font-bold leading-none text-white"
        >
          {{ badgeText }}
        </span>
      </button>
    </template>
    <NotificationDropdown @close="isOpen = false" />
  </Popover>
</template>
