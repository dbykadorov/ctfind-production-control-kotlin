<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { useRecentActivity } from '@/api/composables/use-recent-activity'
import { Card } from '@/components/ui'

const { t } = useI18n()
const { data: changes, loading, error } = useRecentActivity()

const STATUS_LABEL: Record<string, string> = {
  'новый': 'Новый',
  'в работе': 'В работе',
  'готов': 'Готов',
  'отгружен': 'Отгружен',
}

function formatTime(iso: string): string {
  if (!iso)
    return '—'
  // "YYYY-MM-DD HH:MM:SS" → "DD.MM HH:MM"
  const d = iso.slice(8, 10)
  const m = iso.slice(5, 7)
  const time = iso.slice(11, 16)
  return `${d}.${m} ${time}`
}
</script>

<template>
  <Card class="flex flex-col gap-3 p-5">
    <header class="flex items-center justify-between">
      <h2 class="text-base font-semibold text-ink-strong">
        {{ t('dashboard.recentChanges.title') }}
      </h2>
    </header>

    <div v-if="loading && changes.length === 0" class="space-y-2">
      <div v-for="n in 5" :key="n" class="h-9 animate-pulse rounded bg-border/50" />
    </div>

    <p v-else-if="error" class="py-4 text-center text-sm text-ink-muted">
      {{ t('dashboard.recentChanges.empty') }}
    </p>

    <p v-else-if="changes.length === 0" class="py-4 text-center text-sm text-ink-muted">
      {{ t('dashboard.recentChanges.empty') }}
    </p>

    <ul v-else class="flex flex-col">
      <li
        v-for="change in changes"
        :key="change.name"
        class="border-b border-border last:border-b-0 py-2"
      >
        <RouterLink
          :to="`/cabinet/orders/${change.order}`"
          class="flex flex-col gap-0.5 text-sm transition-colors hover:text-ink-strong"
        >
          <span class="text-xs tabular-nums text-ink-muted">{{ formatTime(change.eventAt) }}</span>
          <span class="text-ink">
            {{ t('dashboard.recentChanges.template', {
              actor: change.actorUser,
              order: change.order,
              from: STATUS_LABEL[change.fromStatus] ?? change.fromStatus,
              to: STATUS_LABEL[change.toStatus] ?? change.toStatus,
            }) }}
          </span>
        </RouterLink>
      </li>
    </ul>
  </Card>
</template>
