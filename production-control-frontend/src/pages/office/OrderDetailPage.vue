<script setup lang="ts">
import type { CustomerOrder, CustomerOrderItem } from '@/api/types/domain'
import { format, parseISO } from 'date-fns'
import { ru } from 'date-fns/locale'
import { ArrowLeft, Save } from 'lucide-vue-next'
/**
 * Карточка заказа (US1, FR-017–FR-022).
 * Двухколоночный layout: слева — атрибуты + items + actions, справа — timeline.
 * Сохранение через optimistic concurrency: при `TimestampMismatchError` показывается
 * <ConflictDialog> (FR-021).
 */
import { computed, ref, toRef, watch } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { useOrderHistory } from '@/api/composables/use-history'
import { useOrderEditability } from '@/api/composables/use-order-editability'
import { useOrder } from '@/api/composables/use-orders'
import { usePermissions } from '@/api/composables/use-permissions'
import { useOrderTransitions } from '@/api/composables/use-workflow'
import ConflictDialog from '@/components/domain/ConflictDialog.vue'
import CustomerPicker from '@/components/domain/CustomerPicker.vue'
import ProductionTaskCreateForm from '@/components/domain/ProductionTaskCreateForm.vue'
import BomSection from '@/components/domain/orders/BomSection.vue'
import MaterialUsageSection from '@/components/domain/orders/MaterialUsageSection.vue'
import OrderItemsTable from '@/components/domain/OrderItemsTable.vue'
import OrderStatusBadge from '@/components/domain/OrderStatusBadge.vue'
import OrderTimeline from '@/components/domain/OrderTimeline.vue'
import WorkflowActions from '@/components/domain/WorkflowActions.vue'
import { Button, Card, Input, Label, Skeleton, Textarea } from '@/components/ui'

const props = defineProps<{ name: string }>()

const router = useRouter()

const orderName = toRef(props, 'name')
const { data: order, state, error, reload, save } = useOrder(orderName)
const { data: transitions, reload: reloadTransitions, loading: transitionsLoading } = useOrderTransitions(orderName)
const { entries: timelineEntries, loading: timelineLoading, reload: reloadHistory } = useOrderHistory(orderName)

const permissions = usePermissions()
const editability = useOrderEditability(order, permissions)

const draft = ref<{ customer: string, delivery_date: string, notes: string, items: CustomerOrderItem[] } | null>(null)

watch(order, (next) => {
  if (!next) {
    draft.value = null
    return
  }
  draft.value = {
    customer: next.customer ?? '',
    delivery_date: next.delivery_date ?? '',
    notes: next.notes ?? '',
    items: (next.items ?? []).map(it => ({ ...it })),
  }
}, { immediate: true })

const isDirty = computed(() => {
  if (!order.value || !draft.value)
    return false
  if (draft.value.customer !== (order.value.customer ?? ''))
    return true
  if (draft.value.delivery_date !== (order.value.delivery_date ?? ''))
    return true
  if ((draft.value.notes ?? '') !== (order.value.notes ?? ''))
    return true
  const a = draft.value.items
  const b = order.value.items ?? []
  if (a.length !== b.length)
    return true
  for (let i = 0; i < a.length; i += 1) {
    const ai = a[i]
    const bi = b[i]
    if (!ai || !bi)
      return true
    if (ai.item_name !== bi.item_name)
      return true
    if (Number(ai.quantity) !== Number(bi.quantity))
      return true
    if (ai.uom !== bi.uom)
      return true
  }
  return false
})

const conflictOpen = ref(false)
const saving = ref(false)
const orderMaterialsRevision = ref(0)

const formattedModified = computed(() => {
  if (!order.value?.modified)
    return ''
  try {
    return format(parseISO(order.value.modified), 'd MMM yyyy, HH:mm', { locale: ru })
  }
  catch {
    return order.value.modified
  }
})

async function onSave(): Promise<void> {
  if (!order.value || !draft.value)
    return
  saving.value = true
  try {
    const patch: Partial<CustomerOrder> = {
      customer: draft.value.customer,
      delivery_date: draft.value.delivery_date,
      notes: draft.value.notes || undefined,
      items: draft.value.items,
    }
    await save(patch, order.value.modified)
    toast.success('Заказ сохранён')
    await Promise.all([reloadTransitions(), reloadHistory()])
  }
  catch (e) {
    const apiErr = e as { kind?: string, message?: string, field?: string }
    if (apiErr.kind === 'conflict') {
      conflictOpen.value = true
    }
    else if (apiErr.kind === 'permission') {
      toast.error('Недостаточно прав для сохранения')
    }
    else {
      toast.error(apiErr.message || 'Не удалось сохранить заказ')
    }
  }
  finally {
    saving.value = false
  }
}

async function onConflictReload(): Promise<void> {
  await reload()
  await Promise.all([reloadTransitions(), reloadHistory()])
}

async function onTransitionApplied(): Promise<void> {
  await Promise.all([reload(), reloadTransitions(), reloadHistory()])
}

function back(): void {
  router.push({ name: 'orders.list' })
}

function onBomChanged(): void {
  orderMaterialsRevision.value += 1
}

function onMaterialConsumed(): void {
  orderMaterialsRevision.value += 1
}
</script>

<template>
  <div class="space-y-6">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <div class="flex min-w-0 items-center gap-3">
        <Button variant="ghost" size="sm" @click="back">
          <ArrowLeft class="size-4" aria-hidden="true" />
          К списку
        </Button>
        <div class="min-w-0">
          <p class="font-mono text-xs text-ink-muted">
            {{ orderName }}
          </p>
          <h1 class="truncate text-2xl font-semibold text-ink-strong">
            <span v-if="order">Заказ {{ orderName }}</span>
            <span v-else>Заказ</span>
          </h1>
        </div>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <OrderStatusBadge v-if="order" :status="order.status" />
        <Button
          v-if="editability.canEdit && !editability.readonly"
          variant="primary"
          :disabled="!isDirty"
          :loading="saving"
          @click="onSave"
        >
          <Save class="size-4" aria-hidden="true" />
          Сохранить
        </Button>
      </div>
    </header>

    <div v-if="state === 'loading' && !order" class="grid gap-6 lg:grid-cols-[2fr_1fr]">
      <Skeleton class="h-96 rounded-lg" />
      <Skeleton class="h-96 rounded-lg" />
    </div>

    <div
      v-else-if="state === 'error' && !order"
      role="alert"
      class="rounded border border-danger/40 bg-danger/5 p-4 text-sm text-danger"
    >
      <p class="font-medium">
        Не удалось загрузить заказ
      </p>
      <p class="mt-1">
        {{ error?.message ?? 'Попробуйте обновить страницу.' }}
      </p>
      <Button class="mt-3" variant="secondary" size="sm" @click="reload">
        Повторить
      </Button>
    </div>

    <div v-else-if="order && draft" class="grid gap-6 lg:grid-cols-[2fr_1fr]">
      <div class="space-y-6">
        <Card class="space-y-5 p-5">
          <div v-if="editability.hint" class="rounded border border-warning/30 bg-warning/5 p-3 text-xs text-ink">
            {{ editability.hint }}
          </div>

          <div class="grid gap-4 md:grid-cols-2">
            <div class="space-y-1.5">
              <Label for="d-customer">Клиент</Label>
              <CustomerPicker
                :model-value="draft.customer || null"
                :disabled="editability.readonly || editability.frozen.includes('customer') || editability.frozen.includes('*')"
                @update:model-value="(v: string | null) => (draft!.customer = v ?? '')"
              />
            </div>
            <div class="space-y-1.5">
              <Label for="d-delivery-date">Срок исполнения</Label>
              <Input
                id="d-delivery-date"
                v-model="draft.delivery_date"
                type="date"
                :disabled="editability.readonly || editability.frozen.includes('delivery_date') || editability.frozen.includes('*')"
              />
              <p
                v-if="editability.frozen.includes('delivery_date') && !editability.readonly"
                class="text-xs text-ink-muted"
              >
                Поле заморожено в текущем статусе. Доступно роли Order Corrector.
              </p>
            </div>
          </div>

          <div class="space-y-2">
            <Label>Состав заказа</Label>
            <OrderItemsTable
              v-model="draft.items"
              :editability="editability"
            />
          </div>

          <Card v-if="permissions.canCreateProductionTasks" class="border-dashed">
            <div class="space-y-3 p-4">
              <h3 class="text-sm font-semibold text-ink-strong">
                Производственные задачи
              </h3>
              <p class="text-xs text-ink-muted">
                Создайте задачу для выбранной позиции заказа. Для одной позиции можно добавить несколько задач с разным назначением.
              </p>
              <ProductionTaskCreateForm
                v-if="order"
                :order-id="order.name"
                :items="draft.items"
              />
            </div>
          </Card>

          <div class="space-y-1.5">
            <Label for="d-notes">Комментарий</Label>
            <Textarea
              id="d-notes"
              v-model="draft.notes"
              :rows="4"
              :disabled="editability.readonly"
            />
          </div>

          <p v-if="formattedModified" class="text-xs text-ink-muted">
            Изменён: {{ formattedModified }}
          </p>
        </Card>

        <template v-if="order && permissions.canViewOrderBom">
          <BomSection
            :order-id="order.name"
            :order-shipped="order.status === 'отгружен'"
            :refresh-key="orderMaterialsRevision"
            @changed="onBomChanged"
          />
          <MaterialUsageSection
            :order-id="order.name"
            :order-shipped="order.status === 'отгружен'"
            :refresh-key="orderMaterialsRevision"
            @consumed="onMaterialConsumed"
          />
        </template>

        <Card class="space-y-3 p-5">
          <div class="flex items-center justify-between">
            <h2 class="text-base font-semibold text-ink-strong">
              Доступные действия
            </h2>
            <Button variant="ghost" size="sm" :loading="transitionsLoading" @click="reloadTransitions">
              Обновить
            </Button>
          </div>
          <WorkflowActions
            :order-name="orderName"
            :transitions="transitions"
            :loading="transitionsLoading"
            :disabled="permissions.isShopSupervisor && !permissions.isAdmin"
            @applied="onTransitionApplied"
          />
        </Card>
      </div>

      <Card class="space-y-3 p-5">
        <div class="flex items-center justify-between">
          <h2 class="text-base font-semibold text-ink-strong">
            История
          </h2>
          <Button variant="ghost" size="sm" :loading="timelineLoading" @click="reloadHistory">
            Обновить
          </Button>
        </div>
        <OrderTimeline :entries="timelineEntries" :loading="timelineLoading" />
      </Card>
    </div>

    <ConflictDialog
      :open="conflictOpen"
      :order-name="orderName"
      @update:open="conflictOpen = $event"
      @reload="onConflictReload"
    />
  </div>
</template>
