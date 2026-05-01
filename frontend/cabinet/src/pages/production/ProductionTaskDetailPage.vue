<script setup lang="ts">
/**
 * Карточка производственной задачи (Feature 005).
 */
import type { ProductionTaskStatus } from '@/api/types/production-tasks'
import { format, parseISO } from 'date-fns'
import { ru } from 'date-fns/locale'
import { computed, ref, toRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { usePermissions } from '@/api/composables/use-permissions'
import {
  postProductionTaskStatus,
  putProductionTaskAssignment,
  useProductionTaskDetail,
} from '@/api/composables/use-production-task-detail'
import ProductionTaskAssigneePicker from '@/components/domain/ProductionTaskAssigneePicker.vue'
import ProductionTaskTimeline from '@/components/domain/ProductionTaskTimeline.vue'
import {
  Badge,
  Button,
  Card,
  Input,
  Label,
  Skeleton,
  Textarea,
} from '@/components/ui'

const props = defineProps<{ id: string }>()
const router = useRouter()
const { t } = useI18n()
const permissions = usePermissions()

const taskId = toRef(props, 'id')
const { data, loading, error, forbidden, reload }
  = useProductionTaskDetail(taskId)

const mutating = ref(false)
const blockReason = ref('')
const blockOpen = ref(false)
const assigneeId = ref<string | null>(null)
const planStart = ref('')
const planFinish = ref('')
const assignNote = ref('')

watch(
  () => data.value,
  (task) => {
    if (!task) {
      assigneeId.value = null
      planStart.value = ''
      planFinish.value = ''
      return
    }
    assigneeId.value = task.executor?.id ?? null
    planStart.value = task.plannedStartDate?.slice(0, 10) ?? ''
    planFinish.value = task.plannedFinishDate?.slice(0, 10) ?? ''
  },
  { immediate: true },
)

const plannedRange = computed(() => {
  const task = data.value
  if (!task?.plannedStartDate && !task?.plannedFinishDate)
    return null
  const fmt = (d: string) => format(parseISO(d), 'd MMM yyyy', { locale: ru })
  if (task.plannedStartDate && task.plannedFinishDate)
    return `${fmt(task.plannedStartDate)} — ${fmt(task.plannedFinishDate)}`
  return task.plannedStartDate
    ? fmt(task.plannedStartDate)
    : task.plannedFinishDate
      ? fmt(task.plannedFinishDate)
      : null
})

const canAssignPanel = computed(
  () =>
    data.value
    && permissions.value.canAssignProductionTasks
    && (data.value.allowedActions.includes('ASSIGN')
      || data.value.allowedActions.includes('PLAN')),
)

async function saveAssignment(): Promise<void> {
  const task = data.value
  if (!task || !assigneeId.value) {
    toast.error('Выберите исполнителя')
    return
  }
  mutating.value = true
  try {
    await putProductionTaskAssignment(task.id, {
      expectedVersion: task.version,
      executorUserId: assigneeId.value,
      plannedStartDate: planStart.value || undefined,
      plannedFinishDate: planFinish.value || undefined,
      note: assignNote.value.trim() || undefined,
    })
    toast.success('Назначение сохранено')
    assignNote.value = ''
    await reload()
  }
  catch (e) {
    const st = (
      e as {
        response?: {
          status?: number
          data?: { code?: string, message?: string }
        }
      }
    ).response?.status
    const msg = (e as { response?: { data?: { message?: string } } }).response?.data?.message
    if (st === 409)
      toast.error('Данные устарели. Обновите страницу.')
    else toast.error(msg ?? 'Не удалось сохранить')
  }
  finally {
    mutating.value = false
  }
}

function openBlock(): void {
  blockReason.value = ''
  blockOpen.value = true
}

async function confirmBlock(): Promise<void> {
  const task = data.value
  if (!task)
    return
  const r = blockReason.value.trim()
  if (!r) {
    toast.error('Укажите причину блокировки')
    return
  }
  blockOpen.value = false
  mutating.value = true
  try {
    await postProductionTaskStatus(task.id, {
      expectedVersion: task.version,
      toStatus: 'BLOCKED',
      reason: r,
    })
    toast.success('Задача заблокирована')
    await reload()
  }
  catch (e) {
    const st = (e as { response?: { status?: number } }).response?.status
    if (st === 409)
      toast.error('Данные устарели. Обновите страницу.')
    else toast.error('Не удалось заблокировать')
  }
  finally {
    mutating.value = false
  }
}

async function runStatus(
  to: ProductionTaskStatus,
  note?: string,
): Promise<void> {
  const task = data.value
  if (!task)
    return
  mutating.value = true
  try {
    await postProductionTaskStatus(task.id, {
      expectedVersion: task.version,
      toStatus: to,
      note: note?.trim() || undefined,
    })
    await reload()
    toast.success('Статус обновлён')
  }
  catch (e) {
    const st = (e as { response?: { status?: number } }).response?.status
    if (st === 409)
      toast.error('Данные устарели. Обновите страницу.')
    else if (st === 422)
      toast.error('Переход запрещён')
    else toast.error('Не удалось обновить статус')
  }
  finally {
    mutating.value = false
  }
}

async function onUnblock(): Promise<void> {
  const task = data.value
  if (!task?.previousActiveStatus) {
    toast.error('Нет данных для разблокировки')
    return
  }
  await runStatus(task.previousActiveStatus, 'Разблокировано')
}
</script>

<template>
  <section class="space-y-6">
    <div
      v-if="forbidden"
      class="rounded-lg border border-warning/30 bg-warning/10 p-6 text-ink-strong"
    >
      <p class="font-medium">
        Нет доступа к этой задаче.
      </p>
      <Button
        variant="secondary"
        size="sm"
        class="mt-4"
        @click="router.push({ name: 'production-tasks.list' })"
      >
        К списку задач
      </Button>
    </div>

    <template v-else>
      <div
        class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between"
      >
        <div>
          <h1 class="text-2xl font-semibold text-ink-strong">
            {{ t("meta.title.productionTasks.detail") }}
          </h1>
          <p v-if="data" class="font-mono text-sm text-ink-muted">
            {{ data.taskNumber }}
          </p>
        </div>
        <Button
          type="button"
          variant="secondary"
          size="sm"
          :loading="loading"
          @click="reload()"
        >
          {{ t("common.refresh") }}
        </Button>
      </div>

      <div
        v-if="error"
        class="rounded-md border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-ink-strong"
      >
        {{ error.message }}
      </div>

      <div v-if="loading && !data" class="space-y-4">
        <Skeleton class="h-40 w-full" />
        <Skeleton class="h-32 w-full" />
      </div>

      <div v-else-if="data" class="grid gap-6 lg:grid-cols-3">
        <Card class="p-4 lg:col-span-2">
          <h2 class="mb-3 text-sm font-semibold text-ink-strong">
            Задача
          </h2>
          <dl class="grid gap-2 text-sm">
            <div class="flex flex-wrap gap-2">
              <dt class="text-ink-muted">
                Статус
              </dt>
              <dd>
                <Badge tone="neutral">
                  {{ data.statusLabel }}
                </Badge>
              </dd>
            </div>
            <div>
              <dt class="text-ink-muted">
                Назначение
              </dt>
              <dd class="text-ink-strong">
                {{ data.purpose }}
              </dd>
            </div>
            <div>
              <dt class="text-ink-muted">
                Заказ
              </dt>
              <dd class="text-ink-strong">
                {{ data.order.orderNumber }} ·
                {{ data.order.customerDisplayName }}
              </dd>
            </div>
            <div v-if="data.orderItem">
              <dt class="text-ink-muted">
                Позиция
              </dt>
              <dd class="text-ink-strong">
                №{{ data.orderItem.lineNo }} {{ data.orderItem.itemName }} —
                {{ data.orderItem.quantity }} {{ data.orderItem.uom }}
              </dd>
            </div>
            <div>
              <dt class="text-ink-muted">
                Количество (задача)
              </dt>
              <dd class="text-ink-strong">
                {{ data.quantity }} {{ data.uom }}
              </dd>
            </div>
            <div v-if="data.executor">
              <dt class="text-ink-muted">
                Исполнитель
              </dt>
              <dd class="text-ink-strong">
                {{ data.executor.displayName }}
              </dd>
            </div>
            <div v-if="plannedRange">
              <dt class="text-ink-muted">
                План
              </dt>
              <dd class="text-ink-strong">
                {{ plannedRange }}
              </dd>
            </div>
            <div v-if="data.blockedReason">
              <dt class="text-ink-muted">
                Причина блокировки
              </dt>
              <dd class="text-ink-strong">
                {{ data.blockedReason }}
              </dd>
            </div>
          </dl>
        </Card>

        <Card class="p-4">
          <h2 class="mb-2 text-sm font-semibold text-ink-strong">
            Действия
          </h2>
          <p
            v-if="data.allowedActions.length === 0"
            class="text-sm text-ink-muted"
          >
            Нет доступных действий для вашей роли или задача завершена.
          </p>
          <div v-else class="flex flex-col gap-2">
            <Button
              v-if="data.allowedActions.includes('START')"
              type="button"
              variant="primary"
              size="sm"
              :loading="mutating"
              @click="runStatus('IN_PROGRESS')"
            >
              В работу
            </Button>
            <Button
              v-if="data.allowedActions.includes('COMPLETE')"
              type="button"
              variant="primary"
              size="sm"
              :loading="mutating"
              @click="runStatus('COMPLETED')"
            >
              Завершить
            </Button>
            <Button
              v-if="data.allowedActions.includes('BLOCK')"
              type="button"
              variant="secondary"
              size="sm"
              :disabled="mutating"
              @click="openBlock"
            >
              Заблокировать
            </Button>
            <Button
              v-if="data.allowedActions.includes('UNBLOCK')"
              type="button"
              variant="secondary"
              size="sm"
              :loading="mutating"
              @click="onUnblock"
            >
              Разблокировать
            </Button>
          </div>
        </Card>

        <Card v-if="canAssignPanel" class="p-4 lg:col-span-3">
          <h2 class="mb-3 text-sm font-semibold text-ink-strong">
            Назначение и план
          </h2>
          <div class="grid gap-4 lg:grid-cols-2">
            <ProductionTaskAssigneePicker
              v-model="assigneeId"
              :disabled="mutating"
            />
            <div class="grid gap-3 sm:grid-cols-2">
              <div class="space-y-1.5">
                <Label for="pa-start">План: начало</Label>
                <Input
                  id="pa-start"
                  v-model="planStart"
                  type="date"
                  :disabled="mutating"
                />
              </div>
              <div class="space-y-1.5">
                <Label for="pa-end">План: окончание</Label>
                <Input
                  id="pa-end"
                  v-model="planFinish"
                  type="date"
                  :disabled="mutating"
                />
              </div>
            </div>
          </div>
          <div class="mt-3 space-y-1.5">
            <Label for="pa-note">Комментарий (необязательно)</Label>
            <Textarea
              id="pa-note"
              v-model="assignNote"
              :rows="2"
              :disabled="mutating"
            />
          </div>
          <Button
            class="mt-3"
            type="button"
            variant="primary"
            size="sm"
            :loading="mutating"
            @click="saveAssignment"
          >
            Сохранить
          </Button>
        </Card>

        <Card class="p-4 lg:col-span-3">
          <h2 class="mb-3 text-sm font-semibold text-ink-strong">
            История
          </h2>
          <ProductionTaskTimeline :history="data.history" />
        </Card>
      </div>
    </template>

    <div
      v-if="blockOpen"
      class="fixed inset-0 z-50 flex items-center justify-center bg-overlay p-4"
      role="dialog"
      aria-modal="true"
    >
      <div
        class="w-full max-w-md rounded-lg border border-border bg-elevated p-4 shadow-elevated"
      >
        <h3 class="text-sm font-semibold text-ink-strong">
          Причина блокировки
        </h3>
        <Textarea
          v-model="blockReason"
          class="mt-2"
          :rows="3"
          placeholder="Обязательно"
        />
        <div class="mt-3 flex justify-end gap-2">
          <Button
            type="button"
            variant="secondary"
            size="sm"
            @click="blockOpen = false"
          >
            Отмена
          </Button>
          <Button
            type="button"
            variant="primary"
            size="sm"
            :loading="mutating"
            @click="confirmBlock"
          >
            Заблокировать
          </Button>
        </div>
      </div>
    </div>
  </section>
</template>
