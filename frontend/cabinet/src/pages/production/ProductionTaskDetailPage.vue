<script setup lang="ts">
/**
 * Карточка производственной задачи (Feature 005 US1).
 */
import { format, parseISO } from 'date-fns'
import { ru } from 'date-fns/locale'
import { computed, toRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useProductionTaskDetail } from '@/api/composables/use-production-task-detail'
import { Badge, Button, Card, Skeleton } from '@/components/ui'

const props = defineProps<{ id: string }>()
const router = useRouter()
const { t } = useI18n()

const taskId = toRef(props, 'id')
const { data, loading, error, forbidden, reload } = useProductionTaskDetail(taskId)

const plannedRange = computed(() => {
  const task = data.value
  if (!task?.plannedStartDate && !task?.plannedFinishDate)
    return null
  const fmt = (d: string) => format(parseISO(d), 'd MMM yyyy', { locale: ru })
  if (task.plannedStartDate && task.plannedFinishDate)
    return `${fmt(task.plannedStartDate)} — ${fmt(task.plannedFinishDate)}`
  return task.plannedStartDate ? fmt(task.plannedStartDate) : (task.plannedFinishDate ? fmt(task.plannedFinishDate) : null)
})
</script>

<template>
  <section class="space-y-6">
    <div v-if="forbidden" class="rounded-lg border border-amber-200 bg-amber-50 p-6 text-amber-900">
      <p class="font-medium">
        Нет доступа к этой задаче.
      </p>
      <Button variant="secondary" size="sm" class="mt-4" @click="router.push({ name: 'production-tasks.list' })">
        К списку задач
      </Button>
    </div>

    <template v-else>
      <div class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 class="text-2xl font-semibold text-slate-900">
            {{ t('meta.title.productionTasks.detail') }}
          </h1>
          <p v-if="data" class="font-mono text-sm text-slate-500">
            {{ data.taskNumber }}
          </p>
        </div>
        <Button type="button" variant="secondary" size="sm" :loading="loading" @click="reload()">
          {{ t('common.refresh') }}
        </Button>
      </div>

      <div v-if="error" class="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
        {{ error.message }}
      </div>

      <div v-if="loading && !data" class="space-y-4">
        <Skeleton class="h-40 w-full" />
        <Skeleton class="h-32 w-full" />
      </div>

      <div v-else-if="data" class="grid gap-6 lg:grid-cols-3">
        <Card class="p-4 lg:col-span-2">
          <h2 class="mb-3 text-sm font-semibold text-slate-900">
            Задача
          </h2>
          <dl class="grid gap-2 text-sm">
            <div class="flex flex-wrap gap-2">
              <dt class="text-slate-500">
                Статус
              </dt>
              <dd>
                <Badge tone="neutral">{{ data.statusLabel }}</Badge>
              </dd>
            </div>
            <div>
              <dt class="text-slate-500">
                Назначение
              </dt>
              <dd class="text-slate-900">
                {{ data.purpose }}
              </dd>
            </div>
            <div>
              <dt class="text-slate-500">
                Заказ
              </dt>
              <dd class="text-slate-900">
                {{ data.order.orderNumber }} · {{ data.order.customerDisplayName }}
              </dd>
            </div>
            <div v-if="data.orderItem">
              <dt class="text-slate-500">
                Позиция
              </dt>
              <dd class="text-slate-900">
                №{{ data.orderItem.lineNo }} {{ data.orderItem.itemName }} — {{ data.orderItem.quantity }} {{ data.orderItem.uom }}
              </dd>
            </div>
            <div>
              <dt class="text-slate-500">
                Количество (задача)
              </dt>
              <dd class="text-slate-900">
                {{ data.quantity }} {{ data.uom }}
              </dd>
            </div>
            <div v-if="data.executor">
              <dt class="text-slate-500">
                Исполнитель
              </dt>
              <dd class="text-slate-900">
                {{ data.executor.displayName }}
              </dd>
            </div>
            <div v-if="plannedRange">
              <dt class="text-slate-500">
                План
              </dt>
              <dd class="text-slate-900">
                {{ plannedRange }}
              </dd>
            </div>
            <div v-if="data.blockedReason">
              <dt class="text-slate-500">
                Причина блокировки
              </dt>
              <dd class="text-slate-900">
                {{ data.blockedReason }}
              </dd>
            </div>
          </dl>
        </Card>

        <Card class="p-4">
          <h2 class="mb-2 text-sm font-semibold text-slate-900">
            Действия
          </h2>
          <p v-if="data.allowedActions.length === 0" class="text-sm text-slate-500">
            Нет доступных действий для вашей роли или задача завершена.
          </p>
          <ul v-else class="flex flex-wrap gap-2">
            <li v-for="a in data.allowedActions" :key="a">
              <Badge>{{ a }}</Badge>
            </li>
          </ul>
        </Card>

        <Card class="p-4 lg:col-span-3">
          <h2 class="mb-3 text-sm font-semibold text-slate-900">
            История
          </h2>
          <ol class="space-y-3 border-l border-slate-200 pl-4">
            <li v-for="(ev, idx) in data.history" :key="idx" class="relative text-sm">
              <span class="absolute -left-[21px] top-1.5 size-2 rounded-full bg-slate-300" />
              <p class="text-slate-900">
                <span class="font-medium">{{ ev.type }}</span>
                · {{ ev.actorDisplayName }}
              </p>
              <p class="text-xs text-slate-500">
                {{ format(parseISO(ev.eventAt), "d MMM yyyy HH:mm", { locale: ru }) }}
              </p>
            </li>
          </ol>
        </Card>
      </div>
    </template>
  </section>
</template>