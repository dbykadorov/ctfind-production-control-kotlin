<script setup lang="ts">
/**
 * Доска производственных задач (Feature 006 §M4).
 *
 * Видимость определяется серверным фильтром `GET /api/production-tasks` —
 * никаких client-side role checks здесь и в `useProductionTasksBoard` нет.
 * Колонки рендерятся в фиксированном порядке NOT_STARTED → IN_PROGRESS →
 * BLOCKED → COMPLETED. COMPLETED капается на 30 последних, обновлённых за
 * 7 дней (см. `use-production-tasks-board.ts`).
 */
import type { ProductionTaskStatus } from '@/api/types/production-tasks'
import type { ProductionTasksBoardFilters } from '@/api/composables/use-production-tasks-board'
import { RotateCw, Search } from 'lucide-vue-next'
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRouter } from 'vue-router'
import { useProductionTasksBoard } from '@/api/composables/use-production-tasks-board'
import ProductionTaskAssigneePicker from '@/components/domain/ProductionTaskAssigneePicker.vue'
import ProductionTaskBoardCard from '@/components/domain/ProductionTaskBoardCard.vue'
import { Button, Input, Label, Skeleton } from '@/components/ui'

const { t } = useI18n()
const router = useRouter()

const SEARCH_DEBOUNCE_MS = 300

const searchInput = ref('')
const executorFilter = ref<string | null>(null)
const dueDateFrom = ref('')
const dueDateTo = ref('')
const overdueOnly = ref(false)

const filters = computed<ProductionTasksBoardFilters>(() => ({
  search: searchInput.value.trim() || undefined,
  executorUserId: executorFilter.value ?? undefined,
  dueDateFrom: dueDateFrom.value || undefined,
  dueDateTo: dueDateTo.value || undefined,
  overdueOnly: overdueOnly.value || undefined,
}))

const { data, loading, error, forbidden, refetch } = useProductionTasksBoard()

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(searchInput, () => {
  if (searchTimer)
    clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    void refetch(filters.value)
  }, SEARCH_DEBOUNCE_MS)
})

watch([executorFilter, dueDateFrom, dueDateTo, overdueOnly], () => {
  void refetch(filters.value)
})

onMounted(() => {
  void refetch(filters.value)
})

interface BoardColumn {
  status: ProductionTaskStatus
  label: string
}

const COLUMNS: BoardColumn[] = [
  { status: 'NOT_STARTED', label: 'Не начато' },
  { status: 'IN_PROGRESS', label: 'В работе' },
  { status: 'BLOCKED', label: 'Заблокировано' },
  { status: 'COMPLETED', label: 'Выполнено' },
]
</script>

<template>
  <section class="space-y-6">
    <div class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 class="text-2xl font-semibold text-ink-strong">
          {{ t('meta.title.productionTasks.board') }}
        </h1>
      </div>
      <Button
        type="button"
        variant="secondary"
        size="sm"
        class="shrink-0 gap-2"
        :loading="loading"
        @click="refetch(filters)"
      >
        <RotateCw class="size-4" aria-hidden="true" />
        {{ t('common.refresh') }}
      </Button>
    </div>

    <div class="flex flex-col gap-3 rounded-lg border border-border bg-surface p-4 shadow-card">
      <div class="grid gap-3 md:grid-cols-2 lg:grid-cols-4">
        <div class="md:col-span-2">
          <Label for="board-search">{{ t('common.search') }}</Label>
          <div class="relative mt-1">
            <Search class="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-ink-muted" aria-hidden="true" />
            <Input
              id="board-search"
              v-model="searchInput"
              class="pl-9"
              :placeholder="`${t('common.search')}…`"
            />
          </div>
        </div>
        <div class="md:col-span-2 lg:col-span-2">
          <Label>Исполнитель</Label>
          <div class="mt-1">
            <ProductionTaskAssigneePicker v-model="executorFilter" />
          </div>
        </div>
        <div>
          <Label for="board-due-from">Срок: с</Label>
          <Input id="board-due-from" v-model="dueDateFrom" type="date" class="mt-1" />
        </div>
        <div>
          <Label for="board-due-to">Срок: по</Label>
          <Input id="board-due-to" v-model="dueDateTo" type="date" class="mt-1" />
        </div>
        <div class="flex items-end gap-2 md:col-span-2">
          <label class="flex cursor-pointer items-center gap-2 text-sm">
            <input v-model="overdueOnly" type="checkbox" class="rounded border-border-strong">
            Только просроченные
          </label>
          <Button
            v-if="executorFilter || dueDateFrom || dueDateTo || overdueOnly || searchInput"
            type="button"
            variant="ghost"
            size="sm"
            @click="searchInput = ''; executorFilter = null; dueDateFrom = ''; dueDateTo = ''; overdueOnly = false"
          >
            Сбросить фильтры
          </Button>
        </div>
      </div>
    </div>

    <div v-if="forbidden" class="rounded-lg border border-warning/30 bg-warning/10 p-6 text-ink-strong">
      <p class="font-medium">
        Нет доступа к доске задач.
      </p>
      <Button variant="secondary" size="sm" class="mt-4" @click="router.push({ name: 'production-tasks.list' })">
        К списку задач
      </Button>
    </div>

    <div v-else-if="error" class="rounded-md border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-ink-strong">
      {{ error.message }}
    </div>

    <div
      v-else-if="data?.truncated"
      class="rounded-md border border-warning/30 bg-warning/10 px-4 py-3 text-sm text-ink-strong"
      data-testid="production-task-board-truncation-banner"
    >
      Показаны первые 200 задач — уточните фильтры, чтобы увидеть остальные.
    </div>

    <div
      v-if="loading && !data"
      class="grid gap-3 lg:grid-cols-4"
      data-testid="production-task-board-skeleton"
    >
      <Skeleton v-for="n in 4" :key="n" class="h-72 w-full" />
    </div>

    <div
      v-else-if="data"
      class="flex gap-4 overflow-x-auto pb-2 lg:overflow-visible"
      data-testid="production-task-board-columns"
    >
      <section
        v-for="column in COLUMNS"
        :key="column.status"
        class="flex w-[20rem] min-w-[18rem] shrink-0 flex-col gap-3 lg:w-auto lg:flex-1"
        :data-testid="`production-task-board-column-${column.status}`"
      >
        <header class="flex items-baseline justify-between rounded-md bg-bg/70 px-3 py-2">
          <h2 class="text-sm font-semibold text-ink-strong">
            {{ column.label }}
          </h2>
          <span class="text-xs text-ink-muted">
            {{ data.byStatus[column.status].length }}
          </span>
        </header>
        <div
          v-if="data.byStatus[column.status].length === 0"
          class="rounded-md border border-dashed border-border px-3 py-6 text-center text-xs text-ink-muted"
          :data-testid="`production-task-board-empty-${column.status}`"
        >
          {{ t('common.empty') }}
        </div>
        <RouterLink
          v-for="row in data.byStatus[column.status]"
          :key="row.id"
          :to="{ name: 'production-tasks.detail', params: { id: row.id } }"
          class="rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
        >
          <ProductionTaskBoardCard :row="row" />
        </RouterLink>
      </section>
    </div>
  </section>
</template>
