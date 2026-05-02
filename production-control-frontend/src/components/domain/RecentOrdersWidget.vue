<script setup lang="ts">
import type { OrderFilters } from '@/api/types/domain'
import { ArrowRight } from 'lucide-vue-next'
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { useOrdersList } from '@/api/composables/use-orders'
import OrderStatusBadge from '@/components/domain/OrderStatusBadge.vue'
import { Card } from '@/components/ui'

const { t } = useI18n()
const filters = ref<OrderFilters>({})
const { data: orders, state, error } = useOrdersList(filters, {
  pageLength: 10,
  orderBy: { field: 'creation', order: 'desc' },
})

function formatDate(iso: string): string {
  if (!iso)
    return '—'
  return iso.slice(0, 10)
}
</script>

<template>
  <Card class="flex flex-col gap-3 p-5">
    <header class="flex items-center justify-between">
      <h2 class="text-base font-semibold text-ink-strong">
        {{ t('dashboard.recentOrders.title') }}
      </h2>
      <RouterLink
        to="/cabinet/orders"
        class="flex items-center gap-1 text-xs font-medium text-brand-500 hover:text-brand-600"
      >
        {{ t('dashboard.recentOrders.seeAll') }}
        <ArrowRight class="size-3" aria-hidden="true" />
      </RouterLink>
    </header>

    <div v-if="state === 'loading' && orders.length === 0" class="space-y-2">
      <div v-for="n in 5" :key="n" class="h-10 animate-pulse rounded bg-border/50" />
    </div>

    <p v-else-if="state === 'error'" class="text-sm text-danger">
      {{ error?.message || 'Не удалось загрузить заказы' }}
    </p>

    <p v-else-if="orders.length === 0" class="py-4 text-center text-sm text-ink-muted">
      {{ t('dashboard.recentOrders.empty') }}
    </p>

    <ul v-else class="flex flex-col">
      <li
        v-for="order in orders"
        :key="order.name"
        class="border-b border-border last:border-b-0"
      >
        <RouterLink
          :to="`/cabinet/orders/${order.name}`"
          class="flex items-center gap-3 py-2.5 text-sm transition-colors hover:bg-bg/40"
          :data-testid="`recent-order-${order.name}`"
        >
          <span class="font-mono text-xs text-ink-muted">{{ order.name }}</span>
          <span class="min-w-0 flex-1 truncate text-ink">
            {{ order.customer_name || order.customer || '—' }}
          </span>
          <OrderStatusBadge :status="order.status" size="sm" />
          <span class="hidden text-xs tabular-nums text-ink-muted sm:inline">
            {{ formatDate(order.delivery_date) }}
          </span>
        </RouterLink>
      </li>
    </ul>
  </Card>
</template>
