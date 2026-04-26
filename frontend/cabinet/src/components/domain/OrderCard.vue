<script setup lang="ts">
import type { OrderListItem } from '@/api/types/domain'
import { format, isToday, parseISO } from 'date-fns'
import { ru } from 'date-fns/locale'
import { ArrowRight, CalendarDays, Clock } from 'lucide-vue-next'
import { computed } from 'vue'
import { Card } from '@/components/ui'
import OrderStatusBadge from './OrderStatusBadge.vue'

const props = defineProps<{
  order: OrderListItem
}>()

function safeParse(input: string | undefined | null): Date | null {
  if (!input)
    return null
  try {
    return parseISO(input)
  }
  catch {
    return null
  }
}

const deliveryDate = computed(() => safeParse(props.order.delivery_date))
const creationDate = computed(() => safeParse(props.order.creation))

const isOverdue = computed(() => {
  const d = deliveryDate.value
  if (!d)
    return false
  if (props.order.status === 'отгружен')
    return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return d < today
})

const isDueToday = computed(() => {
  const d = deliveryDate.value
  return d ? isToday(d) : false
})

const deliveryLabel = computed(() => {
  const d = deliveryDate.value
  return d ? format(d, 'd MMM yyyy', { locale: ru }) : '—'
})

const creationLabel = computed(() => {
  const d = creationDate.value
  return d ? format(d, 'd MMM yyyy, HH:mm', { locale: ru }) : '—'
})

const customerLabel = computed(() => props.order.customer_name || props.order.customer || '—')
</script>

<template>
  <RouterLink
    :to="`/cabinet/orders/${order.name}`"
    class="group block focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded-lg"
  >
    <Card
      class="p-4 transition-all hover:border-brand-500 hover:shadow-elevated focus-within:border-brand-500"
    >
      <div class="flex items-start justify-between gap-3">
        <div class="flex min-w-0 items-baseline gap-2">
          <span class="shrink-0 font-mono text-sm font-semibold text-ink-strong">
            {{ order.name }}
          </span>
          <span class="shrink-0 text-ink-muted">/</span>
          <h3 class="truncate text-sm font-medium text-ink" :title="customerLabel">
            {{ customerLabel }}
          </h3>
        </div>
        <OrderStatusBadge :status="order.status" />
      </div>

      <div class="mt-3 flex flex-wrap items-center justify-between gap-x-4 gap-y-1 text-sm">
        <span class="inline-flex items-center gap-1.5 text-ink-muted" :title="`Создан: ${creationLabel}`">
          <Clock class="size-4" aria-hidden="true" />
          <span>{{ creationLabel }}</span>
        </span>
        <span
          class="inline-flex items-center gap-1.5" :class="[
            isOverdue ? 'text-danger font-medium' : isDueToday ? 'text-status-progress font-medium' : 'text-ink-muted',
          ]"
          :title="`Срок: ${deliveryLabel}`"
        >
          <CalendarDays class="size-4" aria-hidden="true" />
          <span>{{ deliveryLabel }}</span>
          <span v-if="isOverdue" class="text-xs">просрочено</span>
          <span v-else-if="isDueToday" class="text-xs">сегодня</span>
          <ArrowRight
            class="ml-1 size-4 text-ink-muted opacity-0 transition-all duration-DEFAULT ease-out-expo group-hover:translate-x-1 group-hover:opacity-100"
            aria-hidden="true"
          />
        </span>
      </div>
    </Card>
  </RouterLink>
</template>
