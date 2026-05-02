<script setup lang="ts">
import type { OrderFilters, OrderStatus } from '@/api/types/domain'
import { Plus, RotateCw, Search } from 'lucide-vue-next'
/**
 * Список заказов для офисных ролей (US1).
 * Фильтры: status, customer, диапазон delivery_date, поиск по номеру/customer.
 * Spring API отдаёт серверную пагинацию по 50 и стабильное состояние empty/error.
 * Realtime-обновления через subscribeListUpdate (см. use-orders.ts).
 */
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useOrdersList } from '@/api/composables/use-orders'
import { usePermissions } from '@/api/composables/use-permissions'
import CustomerPicker from '@/components/domain/CustomerPicker.vue'
import DateRangeFilter from '@/components/domain/DateRangeFilter.vue'
import OrderCard from '@/components/domain/OrderCard.vue'
import { Button, Input, Select, Skeleton } from '@/components/ui'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()
const permissions = usePermissions()
const route = useRoute()
const { t } = useI18n()

const SEARCH_DEBOUNCE_MS = 300

// 007: '__active' — виртуальный пункт «Все активные» (всё, кроме отгружен).
const STATUS_OPTIONS: Array<{ value: OrderStatus | '__active' | '', label: string }> = [
  { value: '', label: 'Все статусы' },
  { value: '__active', label: t('dashboard.statusFilters.allActive') },
  { value: 'новый', label: 'Новый' },
  { value: 'в работе', label: 'В работе' },
  { value: 'готов', label: 'Готов' },
  { value: 'отгружен', label: 'Отгружен' },
]

// 007: Deep-link из KPI-карточек дашборда. Query разбираем один раз при mount;
// при отсутствии query восстанавливаем последние фильтры из ui-store.
function parseInitialFilters(): OrderFilters {
  const q = route.query
  const queryStatus = typeof q.status === 'string' ? q.status : undefined
  const queryOverdue = q.overdue === '1' || q.overdue === 'true'
  if (queryStatus || queryOverdue) {
    if (queryStatus === 'active') {
      return { activeOnly: true, overdue: queryOverdue }
    }
    if (queryStatus && ['новый', 'в работе', 'готов', 'отгружен'].includes(queryStatus)) {
      return { status: queryStatus as OrderStatus, overdue: queryOverdue }
    }
    return { activeOnly: queryOverdue, overdue: queryOverdue }
  }
  return {
    status: ui.lastOrdersFilters?.status,
    customer: ui.lastOrdersFilters?.customer,
    dateFrom: ui.lastOrdersFilters?.dateFrom,
    dateTo: ui.lastOrdersFilters?.dateTo,
    search: ui.lastOrdersFilters?.search,
    activeOnly: ui.lastOrdersFilters?.activeOnly,
    overdue: ui.lastOrdersFilters?.overdue,
  }
}

const filters = ref<OrderFilters>(parseInitialFilters())

const searchInput = ref(filters.value.search ?? '')
let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(searchInput, (q) => {
  if (searchTimer)
    clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    filters.value = { ...filters.value, search: q.trim() || undefined }
  }, SEARCH_DEBOUNCE_MS)
})

watch(
  filters,
  (next) => {
    ui.setOrdersFilters({
      status: next.status,
      customer: next.customer,
      dateFrom: next.dateFrom,
      dateTo: next.dateTo,
      search: next.search,
      activeOnly: next.activeOnly,
      overdue: next.overdue,
    })
  },
  { deep: true },
)

const { data: orders, state, error, hasMore, reload, loadMore } = useOrdersList(filters)

const isInitialLoading = computed(() => state.value === 'loading' && orders.value.length === 0)
const isEmpty = computed(() => (state.value === 'loaded' || state.value === 'empty') && orders.value.length === 0)

const dateRange = computed({
  get: () => ({ from: filters.value.dateFrom, to: filters.value.dateTo }),
  set: (v: { from?: string, to?: string }) => {
    filters.value = { ...filters.value, dateFrom: v.from, dateTo: v.to }
  },
})

function setStatus(v: string | null): void {
  if (v === '__active') {
    filters.value = { ...filters.value, status: undefined, activeOnly: true }
    return
  }
  filters.value = {
    ...filters.value,
    status: (v || undefined) as OrderStatus | undefined,
    activeOnly: false,
  }
}

const statusSelectValue = computed(() => {
  if (filters.value.status)
    return filters.value.status
  if (filters.value.activeOnly)
    return '__active'
  return ''
})

function setCustomer(v: string | null): void {
  filters.value = { ...filters.value, customer: v ?? undefined }
}

function toggleOverdue(): void {
  filters.value = { ...filters.value, overdue: !filters.value.overdue }
}

function clearAllFilters(): void {
  searchInput.value = ''
  filters.value = {
    status: undefined,
    customer: undefined,
    dateFrom: undefined,
    dateTo: undefined,
    search: undefined,
    activeOnly: false,
    overdue: false,
  }
}

const hasAnyFilter = computed(() =>
  Boolean(
    filters.value.status
    || filters.value.customer
    || filters.value.dateFrom
    || filters.value.dateTo
    || filters.value.search
    || filters.value.activeOnly
    || filters.value.overdue,
  ),
)

onMounted(() => {
  if (!filters.value.status && !filters.value.customer) {
    void reload()
  }
})
</script>

<template>
  <div class="space-y-6">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold text-ink-strong">
          Заказы
        </h1>
        <p class="text-sm text-ink-muted">
          {{ orders.length }} {{ orders.length === 1 ? 'заказ' : 'заказов' }}<span v-if="hasMore"> · загрузить ещё</span>
        </p>
      </div>
      <div class="flex items-center gap-2">
        <Button variant="ghost" size="md" :loading="state === 'loading' && orders.length > 0" @click="reload">
          <RotateCw class="size-4" aria-hidden="true" />
          Обновить
        </Button>
        <Button
          v-if="permissions.canManageOrders"
          variant="primary"
          size="md"
          as="router-link"
          :to="{ name: 'orders.new' }"
        >
          <Plus class="size-4" aria-hidden="true" />
          Новый заказ
        </Button>
      </div>
    </header>

    <section
      class="grid gap-3 rounded-lg border border-border bg-surface p-4 shadow-card md:grid-cols-[minmax(0,1fr)_minmax(180px,220px)_minmax(180px,220px)_minmax(180px,220px)]"
      aria-label="Фильтры списка заказов"
    >
      <div class="relative">
        <Search class="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-ink-muted" aria-hidden="true" />
        <Input
          v-model="searchInput"
          placeholder="Поиск по номеру или клиенту…"
          class="pl-8"
          aria-label="Поиск заказов"
        />
      </div>
      <Select
        :model-value="statusSelectValue"
        :options="STATUS_OPTIONS"
        placeholder="Все статусы"
        @update:model-value="setStatus"
      />
      <CustomerPicker
        :model-value="filters.customer ?? null"
        placeholder="Все клиенты"
        @update:model-value="setCustomer"
      />
      <DateRangeFilter v-model="dateRange" />
    </section>

    <div class="flex flex-wrap items-center justify-between gap-2">
      <Button
        variant="ghost"
        size="sm"
        :class="filters.overdue ? 'bg-danger/15 text-danger hover:bg-danger/20' : ''"
        @click="toggleOverdue"
      >
        {{ filters.overdue ? '✓ ' : '' }}{{ t('dashboard.overdueToggle') }}
      </Button>
      <Button v-if="hasAnyFilter" variant="ghost" size="sm" @click="clearAllFilters">
        Сбросить фильтры
      </Button>
    </div>

    <div v-if="isInitialLoading" class="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
      <Skeleton v-for="i in 6" :key="i" class="h-32 rounded-lg" />
    </div>

    <div
      v-else-if="state === 'error'"
      role="alert"
      class="rounded border border-danger/40 bg-danger/5 p-4 text-sm text-danger"
    >
      <p class="font-medium">
        Не удалось загрузить заказы
      </p>
      <p class="mt-1">
        {{ error?.message ?? 'Попробуйте обновить страницу.' }}
      </p>
      <Button class="mt-3" variant="secondary" size="sm" @click="reload">
        Повторить
      </Button>
    </div>

    <div
      v-else-if="isEmpty"
      class="rounded border border-dashed border-border bg-surface p-10 text-center"
    >
      <p class="text-base font-medium text-ink-strong">
        Заказов не найдено
      </p>
      <p class="mt-1 text-sm text-ink-muted">
        <span v-if="hasAnyFilter">Попробуйте изменить фильтры.</span>
        <span v-else>Создайте первый заказ, чтобы начать работу.</span>
      </p>
    </div>

    <ul v-else class="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
      <li v-for="o in orders" :key="o.name">
        <OrderCard :order="o" />
      </li>
    </ul>

    <div v-if="hasMore && state !== 'loading'" class="flex justify-center">
      <Button variant="secondary" @click="loadMore">
        Загрузить ещё
      </Button>
    </div>
  </div>
</template>
