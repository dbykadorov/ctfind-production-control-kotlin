<script setup lang="ts">
import { Plus, RotateCw, Search, Users } from 'lucide-vue-next'
/**
 * Список клиентов (read-only, MVP-стаб для US1).
 *
 * Полноценное управление клиентами (создание/редактирование, активация/деактивация)
 * вынесено в Phase 4. Здесь — только просмотр и поиск, чтобы Order Manager мог
 * видеть справочник, на который ссылаются заказы.
 */
import { onMounted, ref, watch } from 'vue'
import { useCustomersSearch } from '@/api/composables/use-customers'
import { usePermissions } from '@/api/composables/use-permissions'
import { Button, Input, Skeleton } from '@/components/ui'

const permissions = usePermissions()
const { data: customers, loading, error, search } = useCustomersSearch({ onlyActive: false })

const SEARCH_DEBOUNCE_MS = 300
const query = ref('')
let timer: ReturnType<typeof setTimeout> | null = null

watch(query, (q) => {
  if (timer)
    clearTimeout(timer)
  timer = setTimeout(() => void search(q), SEARCH_DEBOUNCE_MS)
})

onMounted(() => {
  void search('')
})

function reload(): void {
  void search(query.value)
}

const STATUS_LABEL = {
  active: 'Активен',
  inactive: 'Неактивен',
} as const

const deskUrl = (name: string) => `/app/customer/${encodeURIComponent(name)}`
</script>

<template>
  <div class="space-y-6">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold text-ink-strong">
          Клиенты
        </h1>
        <p class="text-sm text-ink-muted">
          {{ customers.length }} {{ customers.length === 1 ? 'клиент' : 'клиентов' }}
        </p>
      </div>
      <div class="flex items-center gap-2">
        <Button variant="ghost" size="md" :loading="loading" @click="reload">
          <RotateCw class="size-4" aria-hidden="true" />
          Обновить
        </Button>
        <Button
          v-if="permissions.canManageCustomers"
          variant="secondary"
          size="md"
          as="a"
          :href="deskUrl('new?customer_name=')"
          target="_blank"
          rel="noopener"
        >
          <Plus class="size-4" aria-hidden="true" />
          Новый клиент (Desk)
        </Button>
      </div>
    </header>

    <section
      class="rounded-lg border border-border bg-surface p-4 shadow-card"
      aria-label="Поиск клиентов"
    >
      <div class="relative">
        <Search class="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-ink-muted" aria-hidden="true" />
        <Input
          v-model="query"
          placeholder="Поиск по наименованию или коду…"
          class="pl-8"
          aria-label="Поиск клиентов"
        />
      </div>
    </section>

    <div v-if="error" class="rounded border border-danger/40 bg-danger/5 p-4 text-sm text-danger">
      Не удалось загрузить клиентов: {{ error.message }}
    </div>

    <div v-if="loading && customers.length === 0" class="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
      <Skeleton v-for="i in 6" :key="i" class="h-24 rounded-lg" />
    </div>

    <div
      v-else-if="customers.length === 0"
      class="flex flex-col items-center justify-center gap-2 rounded border border-dashed border-border bg-surface p-12 text-center"
    >
      <Users class="size-8 text-ink-muted" aria-hidden="true" />
      <p class="text-sm text-ink-muted">
        {{ query ? 'Ничего не найдено по запросу' : 'Клиентов пока нет' }}
      </p>
    </div>

    <ul
      v-else
      class="grid gap-3 md:grid-cols-2 xl:grid-cols-3"
    >
      <li
        v-for="c in customers"
        :key="c.name"
        class="ctfind-card flex flex-col gap-2 p-4"
      >
        <div class="flex items-start justify-between gap-2">
          <div class="min-w-0">
            <h3 class="truncate text-base font-semibold text-ink-strong">
              {{ c.customer_name }}
            </h3>
            <span class="font-mono text-xs text-ink-muted">{{ c.name }}</span>
          </div>
          <span
            class="inline-flex shrink-0 items-center rounded-md px-2 py-0.5 text-xs font-medium" :class="[
              c.status === 'active'
                ? 'bg-status-ready/15 text-status-ready'
                : 'bg-status-shipped/15 text-status-shipped',
            ]"
          >
            {{ STATUS_LABEL[c.status] ?? c.status }}
          </span>
        </div>
        <dl class="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 text-sm text-ink">
          <template v-if="c.contact_person">
            <dt class="text-ink-muted">
              Контакт
            </dt>
            <dd class="truncate">
              {{ c.contact_person }}
            </dd>
          </template>
          <template v-if="c.phone">
            <dt class="text-ink-muted">
              Телефон
            </dt>
            <dd class="truncate font-mono">
              {{ c.phone }}
            </dd>
          </template>
          <template v-if="c.email">
            <dt class="text-ink-muted">
              Email
            </dt>
            <dd class="truncate">
              {{ c.email }}
            </dd>
          </template>
        </dl>
        <div v-if="permissions.canManageCustomers" class="mt-1 flex justify-end">
          <a
            :href="deskUrl(c.name)"
            target="_blank"
            rel="noopener"
            class="text-xs text-ink-muted hover:text-brand-500"
          >
            Открыть в Desk →
          </a>
        </div>
      </li>
    </ul>
  </div>
</template>
