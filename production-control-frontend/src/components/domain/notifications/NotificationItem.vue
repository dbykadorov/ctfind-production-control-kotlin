<script setup lang="ts">
import type { NotificationResponse } from '@/api/types/notifications'
import { AlertTriangle, ArrowRightLeft, UserPlus } from 'lucide-vue-next'
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { notificationRoute } from '@/utils/notification-route'
import { relativeTime } from '@/utils/relative-time'
import { useNotificationStore } from '@/stores/notifications'

const props = defineProps<{
  notification: NotificationResponse
}>()

const emit = defineEmits<{ (e: 'navigated'): void }>()

const router = useRouter()
const store = useNotificationStore()

const iconComponent = computed(() => {
  switch (props.notification.type) {
    case 'TASK_ASSIGNED': return UserPlus
    case 'STATUS_CHANGED': return ArrowRightLeft
    case 'TASK_OVERDUE': return AlertTriangle
    default: return null
  }
})

const iconColor = computed(() => {
  switch (props.notification.type) {
    case 'TASK_ASSIGNED': return 'text-blue-500'
    case 'STATUS_CHANGED': return 'text-amber-500'
    case 'TASK_OVERDUE': return 'text-red-500'
    default: return 'text-ink-muted'
  }
})

const timeAgo = computed(() => relativeTime(props.notification.createdAt))

async function handleClick() {
  if (!props.notification.read) {
    await store.markRead(props.notification.id)
  }
  const route = notificationRoute(props.notification)
  if (route) {
    router.push(route)
    emit('navigated')
  }
}
</script>

<template>
  <button
    type="button"
    class="flex w-full items-start gap-3 rounded px-3 py-2 text-left transition-colors hover:bg-bg"
    :class="{ 'bg-brand-50 dark:bg-brand-950/20': !notification.read }"
    @click="handleClick"
  >
    <component
      :is="iconComponent"
      v-if="iconComponent"
      class="mt-0.5 size-4 shrink-0"
      :class="iconColor"
    />
    <div class="min-w-0 flex-1">
      <p class="truncate text-sm" :class="notification.read ? 'text-ink-muted' : 'text-ink-strong font-medium'">
        {{ notification.title }}
      </p>
      <p class="mt-0.5 text-xs text-ink-muted">
        {{ timeAgo }}
      </p>
    </div>
    <span
      v-if="!notification.read"
      class="mt-1.5 size-2 shrink-0 rounded-full bg-brand-500"
    />
  </button>
</template>
