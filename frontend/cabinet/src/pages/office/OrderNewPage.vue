<script setup lang="ts">
import type { CustomerOrder, CustomerOrderItem } from '@/api/types/domain'
import { toTypedSchema } from '@vee-validate/zod'
import { ArrowLeft, Save } from 'lucide-vue-next'
import { useForm } from 'vee-validate'
/**
 * Создание нового заказа (US1, FR-016).
 * Vee-Validate + Zod (OrderCreateSchema). После успешного создания заказа
 * редирект на /cabinet/orders/<name>. Серверные ошибки `validation` маппятся
 * на `setErrors`; `permission` — toast и блокировка формы; прочие — toast.
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { createOrder } from '@/api/composables/use-orders'
import CustomerPicker from '@/components/domain/CustomerPicker.vue'
import OrderItemsTable from '@/components/domain/OrderItemsTable.vue'
import { Button, Card, Input, Label, Textarea } from '@/components/ui'
import { OrderCreateSchema } from '@/utils/validators'

const router = useRouter()

const submitting = ref(false)

const { handleSubmit, errors, defineField, setErrors, values, setFieldValue } = useForm({
  validationSchema: toTypedSchema(OrderCreateSchema),
  initialValues: {
    customer: '',
    delivery_date: '',
    notes: '',
    items: [
      { item_name: '', quantity: 1, uom: 'шт' },
    ],
  },
})

const [customer, customerAttrs] = defineField('customer')
const [deliveryDate, deliveryDateAttrs] = defineField('delivery_date')
const [notes, notesAttrs] = defineField('notes')

function onItemsUpdate(next: CustomerOrderItem[]): void {
  setFieldValue(
    'items',
    next.map(it => ({
      item_name: it.item_name,
      quantity: typeof it.quantity === 'number' ? it.quantity : 0,
      uom: it.uom,
    })),
  )
}

const onSubmit = handleSubmit(async (form) => {
  submitting.value = true
  try {
    const payload: Partial<CustomerOrder> = {
      customer: form.customer,
      delivery_date: form.delivery_date,
      notes: form.notes || undefined,
      items: form.items as CustomerOrderItem[],
    }
    const created = await createOrder(payload)
    toast.success(`Заказ ${created.name} создан`)
    await router.replace({ name: 'orders.detail', params: { name: created.name } })
  }
  catch (e) {
    const apiErr = e as { kind?: string, message?: string, field?: string }
    if (apiErr.kind === 'validation' && apiErr.field) {
      setErrors({ [apiErr.field]: apiErr.message ?? 'Проверьте поле' })
    }
    else if (apiErr.kind === 'permission') {
      toast.error('Недостаточно прав для создания заказа')
    }
    else {
      toast.error(apiErr.message || 'Не удалось сохранить заказ')
    }
  }
  finally {
    submitting.value = false
  }
})
</script>

<template>
  <form class="space-y-6" novalidate @submit.prevent="onSubmit">
    <header class="flex items-center justify-between gap-3">
      <div class="flex items-center gap-3">
        <Button variant="ghost" size="sm" as="router-link" :to="{ name: 'orders.list' }">
          <ArrowLeft class="size-4" aria-hidden="true" />
          К списку
        </Button>
        <h1 class="text-2xl font-semibold text-ink-strong">
          Новый заказ
        </h1>
      </div>
      <div class="flex items-center gap-2">
        <Button variant="primary" type="submit" :loading="submitting">
          <Save class="size-4" aria-hidden="true" />
          Создать заказ
        </Button>
      </div>
    </header>

    <div class="grid gap-6 lg:grid-cols-[2fr_1fr]">
      <Card class="space-y-5 p-5">
        <div class="space-y-1.5">
          <Label for="order-customer" required>Клиент</Label>
          <CustomerPicker
            :model-value="customer ?? null"
            :invalid="Boolean(errors.customer)"
            v-bind="customerAttrs"
            @update:model-value="(v: string | null) => (customer = v ?? '')"
          />
          <p v-if="errors.customer" class="text-xs text-danger">
            {{ errors.customer }}
          </p>
        </div>

        <div class="space-y-1.5">
          <Label for="order-delivery-date" required>Срок исполнения</Label>
          <Input
            id="order-delivery-date"
            v-model="deliveryDate"
            type="date"
            v-bind="deliveryDateAttrs"
            :invalid="Boolean(errors.delivery_date)"
          />
          <p v-if="errors.delivery_date" class="text-xs text-danger">
            {{ errors.delivery_date }}
          </p>
        </div>

        <div class="space-y-2">
          <Label required>Состав заказа</Label>
          <OrderItemsTable
            :model-value="(values.items as CustomerOrderItem[]) ?? []"
            @update:model-value="onItemsUpdate"
          />
          <p v-if="typeof errors.items === 'string'" class="text-xs text-danger">
            {{ errors.items }}
          </p>
        </div>
      </Card>

      <Card class="space-y-5 p-5">
        <div class="space-y-1.5">
          <Label for="order-notes">Комментарий</Label>
          <Textarea
            id="order-notes"
            v-model="notes"
            v-bind="notesAttrs"
            :rows="6"
            placeholder="Дополнительные детали для производства"
          />
          <p v-if="errors.notes" class="text-xs text-danger">
            {{ errors.notes }}
          </p>
        </div>
        <div class="rounded border border-border bg-bg p-3 text-xs text-ink-muted">
          Заказ будет создан в статусе «новый». Дальнейшие изменения и переходы
          доступны на странице карточки.
        </div>
      </Card>
    </div>
  </form>
</template>
