<script setup lang="ts">
import { ArrowDown, ArrowRight, ArrowUp, type LucideIcon } from 'lucide-vue-next'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Card } from '@/components/ui'
import { cn } from '@/lib/utils'

interface Props {
  label: string
  value: number | null
  icon: LucideIcon
  /** Tailwind-классы для фона иконки (например, `bg-status-progress/15 text-status-progress`). */
  iconClass?: string
  /** Дельта в % за прошлый период; null = скрываем индикатор; +N = рост, -N = снижение. */
  deltaPct?: number | null
  loading?: boolean
  to?: string
}

const props = withDefaults(defineProps<Props>(), {
  iconClass: 'bg-brand-100 text-brand-500',
  deltaPct: null,
  loading: false,
  to: undefined,
})

const { t } = useI18n()

const formattedValue = computed(() => {
  if (props.value === null || props.value === undefined)
    return '—'
  if (props.value >= 10_000) {
    return `${(props.value / 1000).toFixed(props.value >= 100_000 ? 0 : 1)}K`
  }
  return new Intl.NumberFormat('ru-RU').format(props.value)
})

const deltaInfo = computed(() => {
  if (props.deltaPct === null || props.deltaPct === undefined)
    return null
  if (props.deltaPct > 0)
    return { icon: ArrowUp, label: t('dashboard.kpi.trendUp'), tone: 'text-success' }
  if (props.deltaPct < 0)
    return { icon: ArrowDown, label: t('dashboard.kpi.trendDown'), tone: 'text-danger' }
  return { icon: ArrowRight, label: t('dashboard.kpi.trendNeutral'), tone: 'text-ink-muted' }
})

const isInteractive = computed(() => Boolean(props.to))
</script>

<template>
  <component
    :is="isInteractive ? 'router-link' : 'div'"
    :to="isInteractive ? to : undefined"
    :class="cn(
      'block focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded',
      isInteractive && 'cursor-pointer',
    )"
    :aria-label="`${label}: ${formattedValue}`"
  >
    <Card
      :class="cn(
        'flex items-start gap-4 p-5',
        isInteractive && 'transition-shadow hover:shadow-elevated',
      )"
    >
      <span :class="cn('flex size-10 items-center justify-center rounded', iconClass)">
        <component :is="icon" class="size-5" aria-hidden="true" />
      </span>
      <div class="flex min-w-0 flex-1 flex-col gap-1">
        <p class="text-xs font-medium uppercase tracking-wider text-ink-muted">
          {{ label }}
        </p>
        <p class="text-2xl font-semibold text-ink-strong tabular-nums">
          <span v-if="loading" class="inline-block h-7 w-16 animate-pulse rounded bg-border" />
          <span v-else>{{ formattedValue }}</span>
        </p>
        <div
          v-if="deltaInfo && !loading"
          :class="cn('mt-0.5 flex items-center gap-1 text-xs font-medium', deltaInfo.tone)"
        >
          <component :is="deltaInfo.icon" class="size-3" aria-hidden="true" />
          <span>{{ Math.abs(deltaPct ?? 0).toFixed(1) }}% · {{ deltaInfo.label }}</span>
        </div>
      </div>
    </Card>
  </component>
</template>
