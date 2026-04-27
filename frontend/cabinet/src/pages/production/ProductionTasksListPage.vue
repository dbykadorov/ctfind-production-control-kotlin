<script setup lang="ts">
/**
 * Список производственных задач (Feature 005 US1).
 */
import type { ProductionTaskListFilters, ProductionTaskStatus } from '@/api/types/production-tasks'
import { RotateCw, Search } from 'lucide-vue-next'
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
</script>

<template>
  <section class="space-y-6">
    <div class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 class="text-2xl font-semibold text-slate-900">
          {{ t('meta.title.productionTasks.list') }}
        </h1>
        <p v-if="showExecutorHint" class="text-sm text-slate-500">
          Показаны только задачи, назначенные на вас.
        </p>
      </div>
      <Button type="button" variant="secondary" size="sm" class="shrink-0 gap-2" @click="refetch(filters)">
        <RotateCw class="size-4" aria-hidden="true" />
        {{ t('common.refresh') }}
      </Button>
    </div>

    <div class="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div class="grid gap-3 md:grid-cols-2 lg:grid-cols-4">
        <div class="md:col-span-2">
          <label class="mb-1 block text-xs font-medium text-slate-600">{{ t('common.search') }}</label>
          <div class="relative">
            <Search class="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-slate-400" aria-hidden="true" />
            <Input
              v-model="searchInput"
              class="pl-9"
              :placeholder="`${t('common.search')}…`"
            />
          </div>
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-slate-600">Статус</label>
          <select
            v-model="statusFilter"
            :class="cn(
              'flex h-10 w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-sm',
              'ring-offset-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-400',
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
          <input v-model="assignedToMe" type="checkbox" class="rounded border-slate-300">
          Только мои
        </label>
        <label class="flex cursor-pointer items-center gap-2">
          <input v-model="blockedOnly" type="checkbox" class="rounded border-slate-300">
          Только заблокированные
        </label>
        <label class="flex cursor-pointer items-center gap-2">
          <input v-model="activeOnly" type="checkbox" class="rounded border-slate-300">
          Только не завершённые
        </label>
      </div>
    </div>

    <div v-if="error" class="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
      {{ error.message }}
    </div>

    <div v-if="loading && !data" class="space-y-3">
      <Skeleton class="h-12 w-full" />
      <Skeleton class="h-12 w-full" />
    </div>

    <div v-else-if="data && data.items.length === 0" class="rounded-lg border border-dashed border-slate-200 p-8 text-center text-slate-500">
      {{ t('common.empty') }}
    </div>

    <ul v-else-if="data" class="divide-y divide-slate-200 overflow-hidden rounded-lg border border-slate-200 bg-white">
      <li v-for="row in data.items" :key="row.id">
        <RouterLink
          :to="{ name: 'production-tasks.detail', params: { id: row.id } }"
          class="flex flex-col gap-2 px-4 py-3 transition hover:bg-slate-50 sm:flex-row sm:items-center sm:justify-between"
        >
          <div class="min-w-0">
            <div class="flex flex-wrap items-baseline gap-2">
              <span class="font-mono text-sm font-semibold text-slate-900">{{ row.taskNumber }}</span>
              <span class="text-xs text-slate-500">{{ row.statusLabel }}</span>
            </div>
            <p class="truncate text-sm text-slate-700">
              {{ row.purpose }} · {{ row.order.orderNumber }} · {{ row.order.customerDisplayName }}
            </p>
          </div>
          <div class="shrink-0 text-xs text-slate-500">
            <span v-if="row.executor">{{ row.executor.displayName }}</span>
            <span v-else>—</span>
          </div>
        </RouterLink>
      </li>
    </ul>
  </section>
</template>
