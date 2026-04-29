<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Loader2 } from 'lucide-vue-next'
import NotificationItem from './NotificationItem.vue'
import { useNotificationStore } from '@/stores/notifications'

const emit = defineEmits<{ (e: 'close'): void }>()
const { t } = useI18n()
const store = useNotificationStore()

async function handleMarkAllRead() {
  await store.markAllRead()
}
</script>

<template>
  <div class="w-80">
    <div class="flex items-center justify-between border-b border-border px-3 py-2">
      <h3 class="text-sm font-semibold text-ink-strong">
        {{ t('notifications.title') }}
      </h3>
      <button
        type="button"
        class="text-xs text-brand-500 hover:text-brand-600"
        @click="handleMarkAllRead"
      >
        {{ t('notifications.markAllRead') }}
      </button>
    </div>

    <div v-if="store.dropdownLoading" class="flex items-center justify-center py-8">
      <Loader2 class="size-5 animate-spin text-ink-muted" />
    </div>

    <div v-else-if="!store.dropdownItems || store.dropdownItems.length === 0" class="px-3 py-6 text-center text-sm text-ink-muted">
      {{ t('notifications.empty') }}
    </div>

    <div v-else class="max-h-80 overflow-y-auto">
      <NotificationItem
        v-for="n in store.dropdownItems"
        :key="n.id"
        :notification="n"
        @navigated="emit('close')"
      />
    </div>

    <div class="border-t border-border px-3 py-2 text-center">
      <RouterLink
        to="/cabinet/notifications"
        class="text-xs text-brand-500 hover:text-brand-600"
        @click="emit('close')"
      >
        {{ t('notifications.allNotifications') }}
      </RouterLink>
    </div>
  </div>
</template>
