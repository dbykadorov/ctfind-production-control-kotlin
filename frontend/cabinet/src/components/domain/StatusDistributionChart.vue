<script setup lang="ts">
import type { StatusDistributionEntry } from '@/api/types/dashboard'
import type { OrderStatus } from '@/api/types/domain'
import {
  ArcElement,
  Chart,
  type ChartConfiguration,
  DoughnutController,
  Legend,
  Tooltip,
} from 'chart.js'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const props = withDefaults(defineProps<Props>(), { loading: false })

Chart.register(DoughnutController, ArcElement, Tooltip, Legend)

interface Props {
  entries: StatusDistributionEntry[]
  loading?: boolean
}
const { t } = useI18n()
const canvasRef = ref<HTMLCanvasElement | null>(null)
let chart: Chart<'doughnut'> | null = null

const STATUS_LABEL: Record<OrderStatus, string> = {
  'новый': 'Новый',
  'в работе': 'В работе',
  'готов': 'Готов',
  'отгружен': 'Отгружен',
}

const isEmpty = computed(() => !props.loading && props.entries.every(e => e.count === 0))

function readVar(name: string, fallback: string): string {
  if (typeof window === 'undefined')
    return fallback
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return v || fallback
}

function reducedMotion(): boolean {
  return typeof window !== 'undefined'
    && window.matchMedia
    && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function buildConfig(entries: StatusDistributionEntry[]): ChartConfiguration<'doughnut'> {
  return {
    type: 'doughnut',
    data: {
      labels: entries.map(e => STATUS_LABEL[e.status]),
      datasets: [{
        data: entries.map(e => e.count),
        backgroundColor: [
          readVar('--c-status-new', '#1d8cf8'),
          readVar('--c-status-progress', '#ff8d72'),
          readVar('--c-status-ready', '#00f2c3'),
          readVar('--c-status-shipped', '#9a9aaf'),
        ],
        borderColor: readVar('--c-surface', '#27293d'),
        borderWidth: 2,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: reducedMotion() ? false : { duration: 400 },
      cutout: '62%',
      plugins: {
        legend: {
          position: 'bottom',
          labels: {
            color: readVar('--c-fg', '#d2d3da'),
            boxWidth: 12,
            boxHeight: 12,
            padding: 12,
          },
        },
        tooltip: {
          callbacks: {
            label(ctx) {
              const entry = entries[ctx.dataIndex]
              if (!entry)
                return ''
              return `${STATUS_LABEL[entry.status]}: ${entry.count} (${entry.percent}%)`
            },
          },
        },
      },
    },
  }
}

function render(): void {
  if (!canvasRef.value)
    return
  chart?.destroy()
  chart = new Chart(canvasRef.value, buildConfig(props.entries))
}

onMounted(() => {
  if (!isEmpty.value)
    render()
})

watch(() => props.entries, () => {
  if (isEmpty.value) {
    chart?.destroy()
    chart = null
    return
  }
  if (!chart) {
    render()
    return
  }
  chart.data.datasets[0]!.data = props.entries.map(e => e.count)
  chart.update(reducedMotion() ? 'none' : undefined)
}, { deep: true })

onBeforeUnmount(() => {
  chart?.destroy()
  chart = null
})
</script>

<template>
  <div class="relative h-64 w-full" role="img" :aria-label="t('dashboard.statusDistribution.title')">
    <div v-if="loading" class="absolute inset-0 flex items-center justify-center">
      <span class="h-8 w-8 animate-pulse rounded-full bg-border" />
    </div>
    <div v-else-if="isEmpty" class="absolute inset-0 flex items-center justify-center text-sm text-ink-muted">
      {{ t('dashboard.statusDistribution.empty') }}
    </div>
    <canvas v-show="!loading && !isEmpty" ref="canvasRef" />
  </div>
</template>
