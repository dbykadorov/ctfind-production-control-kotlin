<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Loader2 } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import NotificationItem from '@/components/domain/notifications/NotificationItem.vue'
import { useNotifications } from '@/api/composables/use-notifications'
import { useNotificationStore } from '@/stores/notifications'

const { t } = useI18n()
const { data, loading, error, refetch } = useNotifications()
const store = useNotificationStore()

const page = ref(0)
const unreadOnly = ref(false)

function loadPage() {
  refetch({ page: page.value, unreadOnly: unreadOnly.value })
}

watch([page, unreadOnly], () => loadPage())
loadPage()

function prevPage() {
  if (page.value > 0) page.value--
}

function nextPage() {
  if (data.value && page.value < data.value.totalPages - 1) page.value++
}

async function markAllRead() {
  await store.markAllRead()
  loadPage()
}
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-xl font-semibold text-ink-strong">
        {{ t('notifications.title') }}
      </h1>
      <Button variant="secondary" size="sm" @click="markAllRead">
        {{ t('notifications.markAllRead') }}
      </Button>
    </div>

    <div class="flex items-center gap-3">
      <label class="flex items-center gap-2 text-sm text-ink-muted">
        <input
          v-model="unreadOnly"
          type="checkbox"
          class="rounded border-border"
          @change="page = 0"
        >
        {{ t('notifications.unreadOnly') }}
      </label>
    </div>

    <div v-if="loading && !data" class="space-y-3">
      <Skeleton v-for="i in 5" :key="i" class="h-14 w-full rounded" />
    </div>

    <div v-else-if="error" class="rounded border border-danger/30 bg-danger/10 p-4 text-sm text-ink-strong">
      <p>{{ error.message || t('common.retry') }}</p>
      <Button variant="secondary" size="sm" class="mt-2" @click="loadPage">
        {{ t('common.refresh') }}
      </Button>
    </div>

    <div v-else-if="!data || data.items.length === 0" class="py-12 text-center text-sm text-ink-muted">
      {{ t('notifications.empty') }}
    </div>

    <div v-else class="divide-y divide-border rounded border border-border">
      <NotificationItem
        v-for="n in data.items"
        :key="n.id"
        :notification="n"
      />
    </div>

    <div v-if="data && data.totalPages > 1" class="flex items-center justify-between pt-2">
      <Button variant="secondary" size="sm" :disabled="page === 0" @click="prevPage">
        {{ '←' }}
      </Button>
      <span class="text-sm text-ink-muted">
        {{ t('audit.page', { current: page + 1, total: data.totalPages }) }}
      </span>
      <Button variant="secondary" size="sm" :disabled="page >= data.totalPages - 1" @click="nextPage">
        {{ '→' }}
      </Button>
    </div>

    <div v-if="loading && data" class="flex justify-center py-2">
      <Loader2 class="size-5 animate-spin text-ink-muted" />
    </div>
  </div>
</template>
