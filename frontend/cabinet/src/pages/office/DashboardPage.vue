<script setup lang="ts">
import {
  AlertTriangle,
  CheckCircle2,
  Clock,
  ListChecks,
} from 'lucide-vue-next'
/**
 * 007: Главная страница «Кабинет CTfind» — дашборд для офисных ролей.
 *
 * Контент собирается из 3 композаблов:
 *   • use-dashboard-stats — KPI + донат статусов
 *   • use-trend-data       — линейный график 30-дневного тренда
 *   • use-recent-activity  — список последних 10 status changes
 *
 * Polling-инвалидация уже встроена в каждый композабл.
 *
 * Order Manager НЕ редиректится в /cabinet/orders — все office-роли
 * приземляются здесь (FR-001).
 */
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useDashboardStats } from '@/api/composables/use-dashboard-stats'
import { usePermissions } from '@/api/composables/use-permissions'
import { useTrendData } from '@/api/composables/use-trend-data'
import DashboardEmptyState from '@/components/domain/DashboardEmptyState.vue'
import KpiCard from '@/components/domain/KpiCard.vue'
import RecentOrdersWidget from '@/components/domain/RecentOrdersWidget.vue'
import RecentStatusChangesWidget from '@/components/domain/RecentStatusChangesWidget.vue'
import StatusDistributionChart from '@/components/domain/StatusDistributionChart.vue'
import TrendChart from '@/components/domain/TrendChart.vue'
import { Card } from '@/components/ui'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const auth = useAuthStore()
const router = useRouter()
const permissions = usePermissions()

onMounted(() => {
  if (!permissions.value.canSeeCabinetWorkArea) {
    router.replace({ name: 'no-modules' })
  }
})

const { kpis, distribution, loading: statsLoading, error: statsError } = useDashboardStats()
const { data: trendSeries, loading: trendLoading } = useTrendData()
// RecentOrdersWidget и RecentStatusChangesWidget сами тянут свои данные;
// здесь композаблы НЕ пере-вызываем, чтобы не дублировать запросы.

// Дашборд считается «совсем пустым» когда система не содержит ни одного заказа.
const totalOrders = computed(() => distribution.value.reduce((acc, e) => acc + e.count, 0))
const isInitialLoading = computed(() => statsLoading.value && kpis.value === null)
const isEmptyState = computed(() => !isInitialLoading.value && totalOrders.value === 0 && !statsError.value)

const trendPoints = computed(() => trendSeries.value?.points ?? [])
const trendDeltaPct = computed(() => trendSeries.value?.delta30vsPrev30Pct ?? null)

const greetingName = computed(() => auth.user || '—')
</script>

<template>
  <div class="mx-auto flex w-full max-w-7xl flex-col gap-6">
    <header class="flex flex-col gap-1">
      <p class="text-xs uppercase tracking-wider text-ink-muted">
        {{ t('app.tagline') }}
      </p>
      <h1 class="text-2xl font-semibold text-ink-strong">
        {{ t('dashboard.title') }} · {{ greetingName }}
      </h1>
      <p class="text-sm text-ink-muted">
        {{ t('dashboard.subtitle') }}
      </p>
    </header>

    <DashboardEmptyState v-if="isEmptyState" />

    <template v-else>
      <section
        class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4"
        :aria-label="t('dashboard.title')"
      >
        <KpiCard
          :label="t('dashboard.kpi.totalActive')"
          :value="kpis?.totalActive ?? null"
          :icon="ListChecks"
          icon-class="bg-status-new/15 text-status-new"
          :delta-pct="trendDeltaPct"
          :loading="isInitialLoading"
          to="/cabinet/orders?status=active"
        />
        <KpiCard
          :label="t('dashboard.kpi.inProgress')"
          :value="kpis?.inProgress ?? null"
          :icon="Clock"
          icon-class="bg-status-progress/15 text-status-progress"
          :loading="isInitialLoading"
          to="/cabinet/orders?status=в%20работе"
        />
        <KpiCard
          :label="t('dashboard.kpi.ready')"
          :value="kpis?.ready ?? null"
          :icon="CheckCircle2"
          icon-class="bg-status-ready/15 text-status-ready"
          :loading="isInitialLoading"
          to="/cabinet/orders?status=готов"
        />
        <KpiCard
          :label="t('dashboard.kpi.overdue')"
          :value="kpis?.overdue ?? null"
          :icon="AlertTriangle"
          icon-class="bg-danger/15 text-danger"
          :loading="isInitialLoading"
          to="/cabinet/orders?status=active&overdue=1"
        />
      </section>

      <section class="grid grid-cols-1 gap-4 xl:grid-cols-3">
        <Card class="p-4 xl:col-span-2">
          <h2 class="mb-3 text-sm font-semibold text-ink-strong">
            {{ t('dashboard.trendChart.title') }}
          </h2>
          <div class="h-64">
            <TrendChart
              :points="trendPoints"
              :loading="trendLoading && trendSeries === null"
            />
          </div>
        </Card>
        <Card class="p-4">
          <h2 class="mb-3 text-sm font-semibold text-ink-strong">
            {{ t('dashboard.statusDistribution.title') }}
          </h2>
          <div class="h-64">
            <StatusDistributionChart
              :entries="distribution"
              :loading="statsLoading && distribution.length === 0"
            />
          </div>
        </Card>
      </section>

      <section class="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <RecentOrdersWidget />
        <RecentStatusChangesWidget />
      </section>
    </template>
  </div>
</template>
