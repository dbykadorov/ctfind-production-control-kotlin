<script setup lang="ts">
/**
 * Создание производственной задачи из позиции заказа (Feature 005 US2).
 */
import type { CustomerOrderItem } from '@/api/types/domain'
import { createProductionTasksFromOrder } from '@/api/composables/use-production-tasks'
import ProductionTaskAssigneePicker from '@/components/domain/ProductionTaskAssigneePicker.vue'
import { Button, Input, Label, Textarea } from '@/components/ui'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'

const props = defineProps<{
  orderId: string
  items: CustomerOrderItem[]
}>()

const emit = defineEmits<{ (e: 'created', taskIds: string[]): void }>()

const router = useRouter()
const orderItemId = ref(props.items[0]?.name ?? '')
const purpose = ref('')
const quantity = ref(1)
const uom = ref('')
const executorUserId = ref<string | null>(null)
const plannedStart = ref('')
const plannedFinish = ref('')
const submitting = ref(false)

const selectedLine = computed(() => props.items.find(i => i.name === orderItemId.value))

function syncFromLine(): void {
  const line = selectedLine.value
  if (line) {
    quantity.value = Number(line.quantity)
    uom.value = line.uom
  }
}

watch(
  () => orderItemId.value,
  () => {
    syncFromLine()
  },
  { immediate: true },
)

watch(
  () => props.items,
  (next) => {
    if (!orderItemId.value && next[0])
      orderItemId.value = next[0].name
  },
  { deep: true },
)

async function submit(): Promise<void> {
  if (!orderItemId.value) {
    toast.error('Выберите позицию заказа')
    return
  }
  const p = purpose.value.trim()
  if (!p) {
    toast.error('Укажите назначение работы')
    return
  }
  if (quantity.value <= 0) {
    toast.error('Количество должно быть больше нуля')
    return
  }
  if (!uom.value.trim()) {
    toast.error('Укажите единицу измерения')
    return
  }
  submitting.value = true
  try {
    const res = await createProductionTasksFromOrder(props.orderId, [
      {
        orderItemId: orderItemId.value,
        purpose: p,
        quantity: quantity.value,
        uom: uom.value.trim(),
        executorUserId: executorUserId.value ?? undefined,
        plannedStartDate: plannedStart.value || undefined,
        plannedFinishDate: plannedFinish.value || undefined,
      },
    ])
    const ids = res.items.map(i => i.id)
    toast.success(`Создана задача ${res.items[0]?.taskNumber ?? ''}`.trim())
    purpose.value = ''
    executorUserId.value = null
    emit('created', ids)
    if (res.items[0])
      void router.push({ name: 'production-tasks.detail', params: { id: res.items[0].id } })
  }
  catch (e) {
    const err = e as { message?: string, response?: { data?: { message?: string } } }
    toast.error(err.response?.data?.message ?? err.message ?? 'Не удалось создать задачу')
  }
  finally {
    submitting.value = false
  }
}
</script>

<template>
  <form class="space-y-4" @submit.prevent="submit()">
    <div v-if="items.length === 0" class="text-sm text-slate-500">
      В заказе нет позиций — добавьте состав заказа перед созданием производственных задач.
    </div>
    <template v-else>
      <div class="space-y-1.5">
        <Label for="pt-line">Позиция заказа</Label>
        <select
          id="pt-line"
          v-model="orderItemId"
          class="flex h-10 w-full rounded-md border border-slate-200 bg-white px-3 text-sm"
        >
          <option
            v-for="(it, i) in items"
            :key="it.name"
            :value="it.name"
          >
            №{{ it.idx ?? i + 1 }} {{ it.item_name }} — {{ it.quantity }} {{ it.uom }}
          </option>
        </select>
      </div>
      <div class="space-y-1.5">
        <Label for="pt-purpose">Назначение (цель работы)</Label>
        <Textarea
          id="pt-purpose"
          v-model="purpose"
          :rows="2"
          required
          placeholder="Например: раскрой, фрезеровка"
        />
      </div>
      <div class="grid gap-3 sm:grid-cols-2">
        <div class="space-y-1.5">
          <Label for="pt-qty">Количество</Label>
          <Input
            id="pt-qty"
            v-model.number="quantity"
            type="number"
            min="0.0001"
            step="any"
            required
          />
        </div>
        <div class="space-y-1.5">
          <Label for="pt-uom">Ед. изм.</Label>
          <Input id="pt-uom" v-model="uom" required />
        </div>
      </div>
      <div class="grid gap-3 sm:grid-cols-2">
        <div class="space-y-1.5">
          <Label for="pt-ps">План: начало</Label>
          <Input id="pt-ps" v-model="plannedStart" type="date" />
        </div>
        <div class="space-y-1.5">
          <Label for="pt-pf">План: окончание</Label>
          <Input id="pt-pf" v-model="plannedFinish" type="date" />
        </div>
      </div>
      <div class="space-y-1.5">
        <div class="flex items-center justify-between gap-2">
          <Label>Исполнитель (необязательно)</Label>
          <Button
            type="button"
            variant="secondary"
            size="sm"
            :disabled="submitting || !executorUserId"
            @click="executorUserId = null"
          >
            Очистить
          </Button>
        </div>
        <ProductionTaskAssigneePicker v-model="executorUserId" :disabled="submitting" />
      </div>
      <Button type="submit" variant="primary" :loading="submitting" :disabled="items.length === 0">
        Создать производственную задачу
      </Button>
    </template>
  </form>
</template>
