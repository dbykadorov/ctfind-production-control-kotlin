<script setup lang="ts">
/**
 * Список производственных задач (Feature 005 US1).
 */
import type { ProductionTaskListFilters, ProductionTaskListRowResponse, ProductionTaskStatus } from '@/api/types/production-tasks'
import { format, parseISO } from 'date-fns'
import { ru } from 'date-fns/locale'
import { CalendarDays, RotateCw, Search } from 'lucide-vue-next'
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { useProductionTasksList } from '@/api/composables/use-production-tasks'
import { usePermissions } from '@/api/composables/use-permissions'
import { Button, Input, Skeleton } from '@/components/ui'
import { cn } from '@/lib/utils'

const { t } = useI18n()
const permissions = usePermissions()

const SEARCH_DEBOUNCE_MS = 300

const searchInput = ref('')
const statusFilter = ref<ProductionTaskStatus | ''>('')
const assignedToMe = ref(false)
const blockedOnly = ref(false)
const activeOnly = ref(false)

const filters = computed<ProductionTaskListFilters>(() => ({
  search: searchInput.value.trim() || undefined,
  status: statusFilter.value || undefined,
  assignedToMe: assignedToMe.value || undefined,
  blockedOnly: blockedOnly.value || undefined,
  activeOnly: activeOnly.value || undefined,
  page: 0,
  size: 20,
}))

const { data, loading, error, refetch } = useProductionTasksList()

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(searchInput, () => {
  if (searchTimer)
    clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    void refetch(filters.value)
  }, SEARCH_DEBOUNCE_MS)
})

watch([statusFilter, assignedToMe, blockedOnly, activeOnly], () => {
  void refetch(filters.value)
})

onMounted(() => {
  void refetch(filters.value)
})

const statusOptions: Array<{ value: ProductionTaskStatus | '', label: string }> = [
  { value: '', label: 'Все статусы' },
  { value: 'NOT_STARTED', label: 'Не начато' },
  { value: 'IN_PROGRESS', label: 'В работе' },
  { value: 'BLOCKED', label: 'Заблокировано' },
  { value: 'COMPLETED', label: 'Выполнено' },
]

const showExecutorHint = computed(
  () => permissions.value.canWorkAssignedProductionTasks && !permissions.value.canViewAllProductionTasks,
)

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

function isOverdue(row: ProductionTaskListRowResponse): boolean {
  if (row.status === 'COMPLETED')
    return false
  const due = safeParse(row.plannedFinishDate)
  if (!due)
    return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return due < today
}

function formatPlannedFinish(input?: string | null): string | null {
  const d = safeParse(input)
  return d ? format(d, 'd MMM yyyy', { locale: ru }) : null
}
</script>

<template>
  <section class="space-y-6">
    <div class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 class="text-2xl font-semibold text-ink-strong">
          {{ t('meta.title.productionTasks.list') }}
        </h1>
        <p v-if="showExecutorHint" class="text-sm text-ink-muted">
          Показаны только задачи, назначенные на вас.
        </p>
      </div>
      <Button type="button" variant="secondary" size="sm" class="shrink-0 gap-2" @click="refetch(filters)">
        <RotateCw class="size-4" aria-hidden="true" />
        {{ t('common.refresh') }}
      </Button>
    </div>

    <div class="flex flex-col gap-3 rounded-lg border border-border bg-surface p-4 shadow-card">
      <div class="grid gap-3 md:grid-cols-2 lg:grid-cols-4">
        <div class="md:col-span-2">
          <label class="mb-1 block text-xs font-medium text-ink-muted">{{ t('common.search') }}</label>
          <div class="relative">
            <Search class="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-ink-muted" aria-hidden="true" />
            <Input
              v-model="searchInput"
              class="pl-9"
              :placeholder="`${t('common.search')}…`"
            />
          </div>
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-ink-muted">Статус</label>
          <select
            v-model="statusFilter"
            :class="cn(
              'flex h-10 w-full rounded-md border border-border bg-surface px-3 py-2 text-sm text-ink-strong',
              'ring-offset-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500',
            )"
          >
            <option v-for="o in statusOptions" :key="o.value || 'all'" :value="o.value">
              {{ o.label }}
            </option>
          </select>
        </div>
      </div>
      <div class="flex flex-wrap gap-4 text-sm">
        <label class="flex cursor-pointer items-center gap-2">
          <input v-model="assignedToMe" type="checkbox" class="rounded border-border-strong">
          Только мои
        </label>
        <label class="flex cursor-pointer items-center gap-2">
          <input v-model="blockedOnly" type="checkbox" class="rounded border-border-strong">
          Только заблокированные
        </label>
        <label class="flex cursor-pointer items-center gap-2">
          <input v-model="activeOnly" type="checkbox" class="rounded border-border-strong">
          Только не завершённые
        </label>
      </div>
    </div>

    <div v-if="error" class="rounded-md border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-ink-strong">
      {{ error.message }}
    </div>

    <div v-if="loading && !data" class="space-y-3">
      <Skeleton class="h-12 w-full" />
      <Skeleton class="h-12 w-full" />
    </div>

    <div v-else-if="data && data.items.length === 0" class="rounded-lg border border-dashed border-border p-8 text-center text-ink-muted">
      {{ t('common.empty') }}
    </div>

    <ul v-else-if="data" class="divide-y divide-border overflow-hidden rounded-lg border border-border bg-surface">
      <li v-for="row in data.items" :key="row.id">
        <RouterLink
          :to="{ name: 'production-tasks.detail', params: { id: row.id } }"
          class="flex flex-col gap-2 px-4 py-3 transition hover:bg-bg/70 sm:flex-row sm:items-center sm:justify-between"
        >
          <div class="min-w-0">
            <div class="flex flex-wrap items-baseline gap-2">
              <span class="font-mono text-sm font-semibold text-ink-strong">{{ row.taskNumber }}</span>
              <span class="text-xs text-ink-muted">{{ row.statusLabel }}</span>
            </div>
            <p class="truncate text-sm text-ink">
              {{ row.purpose }} · {{ row.order.orderNumber }} · {{ row.order.customerDisplayName }}
            </p>
          </div>
          <div class="flex shrink-0 flex-col gap-0.5 text-xs sm:items-end">
            <span
              v-if="row.plannedFinishDate"
              class="inline-flex items-center gap-1.5"
              :class="isOverdue(row) ? 'text-danger font-medium' : 'text-ink-muted'"
              :title="`Срок: ${formatPlannedFinish(row.plannedFinishDate)}`"
            >
              <CalendarDays class="size-3.5" aria-hidden="true" />
              <span>{{ formatPlannedFinish(row.plannedFinishDate) }}</span>
              <span v-if="isOverdue(row)">просрочено</span>
            </span>
            <span class="text-ink-muted">
              <span v-if="row.executor">{{ row.executor.displayName }}</span>
              <span v-else>—</span>
            </span>
          </div>
        </RouterLink>
      </li>
    </ul>
  </section>
</template>
