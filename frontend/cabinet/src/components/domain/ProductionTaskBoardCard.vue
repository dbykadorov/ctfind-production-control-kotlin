<script setup lang="ts">
/**
 * Карточка задачи на доске (Feature 006 §M4).
 *
 * Read-only — клик по обёртывающему RouterLink ведёт на детальную страницу.
 * Карточка не эмитит события и не содержит role-checks: видимость
 * фильтруется на сервере и в `useProductionTasksBoard`.
 */
import type { ProductionTaskListRowResponse } from '@/api/types/production-tasks'
import { format, parseISO } from 'date-fns'
import { ru } from 'date-fns/locale'
import { CalendarDays, ShieldAlert } from 'lucide-vue-next'
import { computed } from 'vue'
import { Card } from '@/components/ui'

const props = defineProps<{
  row: ProductionTaskListRowResponse
}>()

function safeParse(input?: string | null): Date | null {
  if (!input)
    return null
  try {
    return parseISO(input)
  }
  catch {
    return null
  }
}

const isOverdue = computed<boolean>(() => {
  if (props.row.status === 'COMPLETED')
    return false
  const due = safeParse(props.row.plannedFinishDate)
  if (!due)
    return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return due < today
})

const formattedDueDate = computed<string | null>(() => {
  const due = safeParse(props.row.plannedFinishDate)
  return due ? format(due, 'd MMM yyyy', { locale: ru }) : null
})
</script>

<template>
  <Card
    class="block p-3 text-sm transition hover:border-brand-500 hover:shadow-elevated focus-within:border-brand-500"
    data-testid="production-task-board-card"
  >
    <div class="flex flex-wrap items-baseline justify-between gap-2">
      <span class="font-mono text-sm font-semibold text-slate-900">{{ row.taskNumber }}</span>
      <span class="text-xs text-slate-500">{{ row.statusLabel }}</span>
    </div>

    <p
      v-if="row.status === 'BLOCKED' && row.blockedReason"
      class="mt-1 line-clamp-2 inline-flex items-start gap-1 text-xs text-danger"
      data-testid="production-task-board-card-blocked-reason"
    >
      <ShieldAlert class="mt-0.5 size-3.5 shrink-0" aria-hidden="true" />
      <span>{{ row.blockedReason }}</span>
    </p>

    <p class="mt-2 line-clamp-2 text-sm text-slate-700">
      {{ row.purpose }}
    </p>
    <p class="mt-1 truncate text-xs text-slate-500">
      {{ row.order.orderNumber }} · {{ row.order.customerDisplayName }}
    </p>

    <div class="mt-3 flex flex-wrap items-center justify-between gap-2 text-xs">
      <span class="truncate text-slate-700">
        <template v-if="row.executor">{{ row.executor.displayName }}</template>
        <template v-else>не назначен</template>
      </span>
      <span
        v-if="formattedDueDate"
        class="inline-flex items-center gap-1.5"
        :class="isOverdue ? 'text-danger font-medium' : 'text-slate-500'"
        :title="`Срок: ${formattedDueDate}`"
      >
        <CalendarDays class="size-3.5" aria-hidden="true" />
        <span>{{ formattedDueDate }}</span>
        <span v-if="isOverdue" data-testid="production-task-board-card-overdue">просрочено</span>
      </span>
      <span v-else class="text-slate-400">срок не указан</span>
    </div>
  </Card>
</template>
