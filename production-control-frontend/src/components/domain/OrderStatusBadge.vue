<script setup lang="ts">
import type { OrderStatus } from '@/api/types/domain'
import { computed } from 'vue'
import { cn } from '@/lib/utils'

interface StatusMeta {
  label: string
  bg: string
  text: string
  dot: string
}

const props = withDefaults(
  defineProps<{
    status: OrderStatus
    size?: 'sm' | 'md'
  }>(),
  { size: 'md' },
)

const STATUS_META: Record<OrderStatus, StatusMeta> = {
  'новый': {
    label: 'Новый',
    bg: 'bg-status-new/15',
    text: 'text-status-new',
    dot: 'bg-status-new',
  },
  'в работе': {
    label: 'В работе',
    bg: 'bg-status-progress/15',
    text: 'text-status-progress',
    dot: 'bg-status-progress',
  },
  'готов': {
    label: 'Готов',
    bg: 'bg-status-ready/15',
    text: 'text-status-ready',
    dot: 'bg-status-ready',
  },
  'отгружен': {
    label: 'Отгружен',
    bg: 'bg-status-shipped/15',
    text: 'text-status-shipped',
    dot: 'bg-status-shipped',
  },
}

const meta = computed<StatusMeta>(() => STATUS_META[props.status])

const sizeClass = computed(() =>
  props.size === 'sm'
    ? 'gap-1 px-1.5 py-0.5 text-[10px]'
    : 'gap-1.5 px-2 py-0.5 text-xs',
)

const dotClass = computed(() =>
  props.size === 'sm' ? 'size-1' : 'size-1.5',
)
</script>

<template>
  <span
    :class="cn(
      'inline-flex items-center rounded-md font-medium',
      sizeClass,
      meta.bg,
      meta.text,
    )"
    :aria-label="`Статус: ${meta.label}`"
  >
    <span :class="cn('rounded-full', dotClass, meta.dot)" />
    {{ meta.label }}
  </span>
</template>
