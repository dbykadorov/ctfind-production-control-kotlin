<script setup lang="ts">
import type { OrderTrendPoint } from '@/api/types/dashboard'
import {
  CategoryScale,
  Chart,
  type ChartConfiguration,
  Filler,
  Legend,
  LinearScale,
  LineController,
  LineElement,
  PointElement,
  Tooltip,
} from 'chart.js'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const props = withDefaults(defineProps<Props>(), { loading: false })

Chart.register(LineController, LineElement, PointElement, LinearScale, CategoryScale, Filler, Tooltip, Legend)

interface Props {
  points: OrderTrendPoint[]
  loading?: boolean
}
const { t } = useI18n()
const canvasRef = ref<HTMLCanvasElement | null>(null)
let chart: Chart<'line'> | null = null

const isEmpty = computed(() => !props.loading && props.points.every(p => p.count === 0))

const ariaSummary = computed(() => {
  if (props.points.length === 0)
    return ''
  const total = props.points.reduce((acc, p) => acc + p.count, 0)
  return `${total} заказов за ${props.points.length} дней`
})

function reducedMotion(): boolean {
  return typeof window !== 'undefined'
    && window.matchMedia
    && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function readVar(name: string, fallback: string): string {
  if (typeof window === 'undefined')
    return fallback
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return v || fallback
}

function buildConfig(points: OrderTrendPoint[]): ChartConfiguration<'line'> {
  const brand = readVar('--c-brand-500', '#1d8cf8')
  const fg = readVar('--c-fg-muted', '#9a9aaf')
  const grid = readVar('--c-border', '#34374d')
  return {
    type: 'line',
    data: {
      labels: points.map(p => p.date.slice(5)),
      datasets: [{
        label: t('dashboard.trendChart.title'),
        data: points.map(p => p.count),
        fill: true,
        backgroundColor: `${brand}33`,
        borderColor: brand,
        borderWidth: 2,
        tension: 0.35,
        pointRadius: 0,
        pointHoverRadius: 4,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: reducedMotion() ? false : { duration: 400 },
      plugins: {
        legend: { display: false },
        tooltip: {
          mode: 'index',
          intersect: false,
        },
      },
      scales: {
        x: {
          ticks: { color: fg, maxRotation: 0, autoSkip: true, maxTicksLimit: 8 },
          grid: { display: false },
        },
        y: {
          beginAtZero: true,
          ticks: { color: fg, precision: 0 },
          grid: { color: grid },
        },
      },
    },
  }
}

function renderChart(): void {
  if (!canvasRef.value)
    return
  chart?.destroy()
  chart = new Chart(canvasRef.value, buildConfig(props.points))
}

onMounted(() => {
  if (!isEmpty.value)
    renderChart()
})

watch(() => props.points, () => {
  if (isEmpty.value) {
    chart?.destroy()
    chart = null
    return
  }
  if (!chart) {
    renderChart()
    return
  }
  chart.data.labels = props.points.map(p => p.date.slice(5))
  chart.data.datasets[0]!.data = props.points.map(p => p.count)
  chart.update(reducedMotion() ? 'none' : undefined)
}, { deep: true })

onBeforeUnmount(() => {
  chart?.destroy()
  chart = null
})
</script>

<template>
  <div class="relative h-64 w-full" role="img" :aria-label="t('dashboard.trendChart.ariaLabelTemplate', { summary: ariaSummary })">
    <div v-if="loading" class="absolute inset-0 flex items-center justify-center">
      <span class="h-8 w-8 animate-pulse rounded-full bg-border" />
    </div>
    <div v-else-if="isEmpty" class="absolute inset-0 flex items-center justify-center text-sm text-ink-muted">
      {{ t('dashboard.trendChart.empty') }}
    </div>
    <canvas v-show="!loading && !isEmpty" ref="canvasRef" />
  </div>
</template>
