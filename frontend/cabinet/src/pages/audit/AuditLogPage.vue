<script setup lang="ts">
import type { AuditCategory, AuditLogFilters, AuditLogRowResponse } from '@/api/types/audit-log'
import { RotateCw, Search } from 'lucide-vue-next'
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { useAuditLog } from '@/api/composables/use-audit-log'
import AuditActorPicker from '@/components/domain/AuditActorPicker.vue'
import { Skeleton } from '@/components/ui'
import { format, parseISO } from 'date-fns'
import { ru } from 'date-fns/locale'

const { t } = useI18n()
const { data, loading, error, refetch } = useAuditLog()

const DEBOUNCE_MS = 300

// Filter state
const dateFrom = ref('')
const dateTo = ref('')
const categories = ref<Record<AuditCategory, boolean>>({
  AUTH: true,
  ORDER: true,
  PRODUCTION_TASK: true,
  INVENTORY: true,
})
const actorUserId = ref<string | null>(null)
const searchQuery = ref('')
const page = ref(0)

// Debounce timer for search
let debounceTimer: ReturnType<typeof setTimeout> | null = null

// Computed derived state
const isInitialLoading = computed(() => loading.value && data.value === null)
const isEmpty = computed(() => !loading.value && data.value !== null && data.value.items.length === 0)
const isForbidden = computed(() => error.value?.kind === 'forbidden')
const isError = computed(() => error.value !== null && error.value.kind !== 'forbidden')
const totalPages = computed(() => data.value?.totalPages ?? 0)
const hasFilters = computed(() =>
  dateFrom.value !== '' || dateTo.value !== ''
  || !categories.value.AUTH || !categories.value.ORDER || !categories.value.PRODUCTION_TASK || !categories.value.INVENTORY
  || actorUserId.value !== null || searchQuery.value.trim() !== '',
)

function buildFilters(): AuditLogFilters {
  const f: AuditLogFilters = { page: page.value, size: 50 }
  if (dateFrom.value)
    f.from = new Date(dateFrom.value).toISOString()
  if (dateTo.value)
    f.to = new Date(dateTo.value + 'T23:59:59').toISOString()
  const selectedCategories = (Object.entries(categories.value) as [AuditCategory, boolean][])
    .filter(([, v]) => v)
    .map(([k]) => k)
  if (selectedCategories.length < 3)
    f.category = selectedCategories
  if (actorUserId.value)
    f.actorUserId = actorUserId.value
  if (searchQuery.value.trim())
    f.search = searchQuery.value.trim()
  return f
}

function doFetch(): void {
  void refetch(buildFilters())
}

function onFilterChange(): void {
  page.value = 0
  doFetch()
}

function resetFilters(): void {
  dateFrom.value = ''
  dateTo.value = ''
  categories.value = { AUTH: true, ORDER: true, PRODUCTION_TASK: true, INVENTORY: true }
  actorUserId.value = null
  searchQuery.value = ''
  page.value = 0
  doFetch()
}

function nextPage(): void {
  if (page.value < totalPages.value - 1) {
    page.value++
    doFetch()
  }
}

function prevPage(): void {
  if (page.value > 0) {
    page.value--
    doFetch()
  }
}

// Debounce search input
watch(searchQuery, () => {
  if (debounceTimer)
    clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    page.value = 0
    doFetch()
  }, DEBOUNCE_MS)
})

function formatTime(iso: string): string {
  return format(parseISO(iso), 'dd.MM.yyyy HH:mm', { locale: ru })
}

function targetRoute(row: AuditLogRowResponse) {
  if (row.targetType === 'ORDER' && row.targetId)
    return { name: 'orders.detail', params: { id: row.targetId } }
  if (row.targetType === 'PRODUCTION_TASK' && row.targetId)
    return { name: 'production-tasks.detail', params: { id: row.targetId } }
  return null
}

onMounted(() => {
  doFetch()
})
</script>

<template>
  <div class="space-y-6 p-6">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <h1 class="text-2xl font-semibold text-ink-strong">
        {{ t('meta.title.audit') }}
      </h1>
      <button
        class="inline-flex items-center gap-1.5 rounded-md border border-border bg-surface px-3 py-1.5 text-sm font-medium text-ink-strong hover:bg-bg"
        @click="doFetch()"
      >
        <RotateCw class="size-4" aria-hidden="true" />
        {{ t('audit.refresh') }}
      </button>
    </header>

    <!-- Forbidden state -->
    <div v-if="isForbidden" class="rounded border border-dashed border-border bg-surface p-10 text-center">
      <p class="text-base font-medium text-ink-strong">
        {{ t('audit.forbidden') }}
      </p>
    </div>

    <template v-else>
      <!-- Filter panel -->
      <section
        class="grid gap-3 rounded-lg border border-border bg-surface p-4 shadow-card lg:grid-cols-[1fr_1fr_1fr_1fr]"
        aria-label="Фильтры журнала"
      >
        <div class="space-y-1">
          <label class="text-xs font-medium text-ink-muted">{{ t('audit.filters.dateFrom') }}</label>
          <input
            v-model="dateFrom"
            type="date"
            class="w-full rounded border border-border bg-bg px-2 py-1.5 text-sm"
            @change="onFilterChange()"
          >
        </div>
        <div class="space-y-1">
          <label class="text-xs font-medium text-ink-muted">{{ t('audit.filters.dateTo') }}</label>
          <input
            v-model="dateTo"
            type="date"
            class="w-full rounded border border-border bg-bg px-2 py-1.5 text-sm"
            @change="onFilterChange()"
          >
        </div>
        <div class="space-y-1">
          <label class="text-xs font-medium text-ink-muted">{{ t('audit.filters.category') }}</label>
          <div class="flex flex-wrap gap-3 pt-1">
            <label v-for="cat in (['AUTH', 'ORDER', 'PRODUCTION_TASK', 'INVENTORY'] as AuditCategory[])" :key="cat" class="inline-flex items-center gap-1 text-sm">
              <input
                v-model="categories[cat]"
                type="checkbox"
                @change="onFilterChange()"
              >
              {{ t(`audit.category.${cat}`) }}
            </label>
          </div>
        </div>
        <div class="space-y-1">
          <label class="text-xs font-medium text-ink-muted">{{ t('audit.filters.actor') }}</label>
          <AuditActorPicker
            :model-value="actorUserId"
            @update:model-value="(v: string | null) => { actorUserId = v; onFilterChange() }"
          />
        </div>
        <div class="relative lg:col-span-3">
          <Search class="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-ink-muted" aria-hidden="true" />
          <input
            v-model="searchQuery"
            type="text"
            :placeholder="t('audit.filters.search')"
            class="w-full rounded border border-border bg-bg py-1.5 pl-8 pr-3 text-sm"
          >
        </div>
        <div class="flex items-end">
          <button
            v-if="hasFilters"
            class="rounded border border-border px-3 py-1.5 text-sm text-ink-muted hover:bg-bg"
            @click="resetFilters()"
          >
            {{ t('audit.resetFilters') }}
          </button>
        </div>
      </section>

      <!-- Loading skeleton -->
      <div v-if="isInitialLoading" class="space-y-2">
        <Skeleton v-for="i in 8" :key="i" class="h-10 rounded" />
      </div>

      <!-- Error banner -->
      <div
        v-else-if="isError"
        role="alert"
        class="rounded border border-danger/40 bg-danger/5 p-4 text-sm text-danger"
      >
        <p class="font-medium">
          {{ t('audit.errorLoading') }}
        </p>
        <p v-if="error?.message" class="mt-1">
          {{ error.message }}
        </p>
        <button
          class="mt-3 rounded-md border border-border bg-surface px-3 py-1.5 text-sm font-medium text-ink-strong hover:bg-bg"
          @click="refetch()"
        >
          {{ t('audit.refresh') }}
        </button>
      </div>

      <!-- Empty state -->
      <div v-else-if="isEmpty" class="rounded border border-dashed border-border bg-surface p-10 text-center">
        <p class="text-base font-medium text-ink-strong">
          {{ hasFilters ? t('audit.emptyFiltered') : t('audit.empty') }}
        </p>
      </div>

      <!-- Data table -->
      <div v-else-if="data && data.items.length > 0" class="overflow-x-auto">
        <table class="w-full min-w-[800px] text-sm">
          <thead>
            <tr class="border-b border-border text-left text-xs font-medium uppercase text-ink-muted">
              <th class="px-3 py-2">{{ t('audit.columns.time') }}</th>
              <th class="px-3 py-2">{{ t('audit.columns.category') }}</th>
              <th class="px-3 py-2">{{ t('audit.columns.eventType') }}</th>
              <th class="px-3 py-2">{{ t('audit.columns.actor') }}</th>
              <th class="px-3 py-2">{{ t('audit.columns.summary') }}</th>
              <th class="px-3 py-2" />
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in data.items"
              :key="row.id"
              class="border-b border-border last:border-0 hover:bg-bg/50"
            >
              <td class="whitespace-nowrap px-3 py-2 text-ink-muted">
                {{ formatTime(row.occurredAt) }}
              </td>
              <td class="px-3 py-2">
                <span
                  class="inline-block rounded-full px-2 py-0.5 text-xs font-medium"
                  :class="{
                    'bg-blue-100 text-blue-800': row.category === 'AUTH',
                    'bg-green-100 text-green-800': row.category === 'ORDER',
                    'bg-purple-100 text-purple-800': row.category === 'PRODUCTION_TASK',
                  }"
                >
                  {{ t(`audit.category.${row.category}`) }}
                </span>
              </td>
              <td class="px-3 py-2 text-ink-strong">{{ row.eventType }}</td>
              <td class="px-3 py-2">{{ row.actorDisplayName }}</td>
              <td class="max-w-xs truncate px-3 py-2 text-ink-muted">{{ row.summary }}</td>
              <td class="px-3 py-2">
                <RouterLink
                  v-if="targetRoute(row)"
                  :to="targetRoute(row)!"
                  class="text-accent hover:underline"
                >
                  →
                </RouterLink>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="mt-4 flex items-center justify-center gap-4 text-sm">
          <button
            class="rounded border border-border px-3 py-1 text-sm disabled:opacity-50"
            :disabled="page === 0"
            @click="prevPage()"
          >
            ← {{ t('audit.page', { current: '', total: '' }).includes('Стр') ? 'Назад' : 'Prev' }}
          </button>
          <span class="text-ink-muted">
            {{ t('audit.page', { current: page + 1, total: totalPages }) }}
          </span>
          <button
            class="rounded border border-border px-3 py-1 text-sm disabled:opacity-50"
            :disabled="page >= totalPages - 1"
            @click="nextPage()"
          >
            {{ t('audit.page', { current: '', total: '' }).includes('Стр') ? 'Вперёд' : 'Next' }} →
          </button>
        </div>
        <div class="mt-2 text-center text-xs text-ink-muted">
          {{ t('audit.totalItems', { count: data.totalItems }) }}
        </div>
      </div>
    </template>
  </div>
</template>
