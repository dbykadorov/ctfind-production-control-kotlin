<script setup lang="ts">
import type { BomLine, BomLineListResponse, MaterialUsage } from '@/api/types/warehouse'
import { computed, ref, watch } from 'vue'
import { httpClient } from '@/api/api-client'
import { useActiveOrders } from '@/api/composables/use-active-orders'
import { useStockConsumption } from '@/api/composables/use-stock-consumption'
import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { toApiError } from '@/utils/errors'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  open: boolean
  preselectedOrderId?: string
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'consumed'): void
}>()

const { t } = useI18n()
const { items: activeOrders, loading: activeOrdersLoading, search: searchActiveOrders } = useActiveOrders()
const { loading: consumeLoading, error: consumeError, errorCode, availableStock, consume } = useStockConsumption()

const orderSearch = ref('')
const selectedOrderId = ref<string | null>(props.preselectedOrderId ?? null)
const selectedMaterialId = ref<string | null>(null)
const quantity = ref<number | undefined>(undefined)
const comment = ref('')
const bomLines = ref<BomLine[]>([])
const usage = ref<MaterialUsage | null>(null)
const loadingOrderData = ref(false)
const localError = ref<string | null>(null)

let searchTimer: ReturnType<typeof setTimeout> | null = null

const orderOptions = computed(() =>
  activeOrders.value.map(order => ({
    value: order.id,
    label: `${order.orderNumber} — ${order.customerName}`,
  })),
)

const materialOptions = computed(() =>
  bomLines.value.map(line => ({
    value: line.materialId,
    label: `${line.materialName} (${t(`warehouse.units.${line.materialUnit}`)})`,
  })),
)

const selectedUsageRow = computed(() =>
  usage.value?.rows.find(row => row.materialId === selectedMaterialId.value) ?? null,
)

const overconsumption = computed(() => {
  if (!selectedUsageRow.value || quantity.value === undefined || quantity.value <= 0)
    return 0
  const totalAfter = selectedUsageRow.value.consumedQuantity + quantity.value
  const extra = totalAfter - selectedUsageRow.value.requiredQuantity
  return extra > 0 ? extra : 0
})

watch(
  () => props.open,
  (isOpen) => {
    if (!isOpen)
      return
    selectedOrderId.value = props.preselectedOrderId ?? null
    selectedMaterialId.value = null
    quantity.value = undefined
    comment.value = ''
    localError.value = null
    if (props.preselectedOrderId)
      void fetchOrderData(props.preselectedOrderId)
  },
  { immediate: true },
)

watch(
  () => props.preselectedOrderId,
  (nextOrderId) => {
    if (!props.open)
      return
    selectedOrderId.value = nextOrderId ?? null
    if (nextOrderId)
      void fetchOrderData(nextOrderId)
  },
)

watch(orderSearch, (query) => {
  if (props.preselectedOrderId)
    return
  if (searchTimer)
    clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    void searchActiveOrders(query)
  }, 300)
})

watch(selectedOrderId, (orderId) => {
  if (!orderId)
    return
  void fetchOrderData(orderId)
})

async function fetchOrderData(orderId: string) {
  loadingOrderData.value = true
  localError.value = null
  try {
    const [bomRes, usageRes] = await Promise.all([
      httpClient.get<BomLineListResponse>(`/api/orders/${orderId}/bom`),
      httpClient.get<MaterialUsage>(`/api/orders/${orderId}/material-usage`),
    ])
    bomLines.value = bomRes.data.items
    usage.value = usageRes.data
    if (!selectedMaterialId.value || !bomLines.value.some(line => line.materialId === selectedMaterialId.value)) {
      selectedMaterialId.value = bomLines.value[0]?.materialId ?? null
    }
  }
  catch (e) {
    localError.value = toApiError(e).message
    bomLines.value = []
    usage.value = null
    selectedMaterialId.value = null
  }
  finally {
    loadingOrderData.value = false
  }
}

function close() {
  emit('update:open', false)
}

async function submit() {
  localError.value = null
  if (!selectedOrderId.value) {
    localError.value = t('consume.pickOrder')
    return
  }
  if (!selectedMaterialId.value) {
    localError.value = t('consume.pickMaterial')
    return
  }
  if (quantity.value === undefined || quantity.value <= 0) {
    localError.value = t('bom.quantityPositive')
    return
  }

  const result = await consume(selectedMaterialId.value, {
    orderId: selectedOrderId.value,
    quantity: quantity.value,
    comment: comment.value.trim() || undefined,
  })

  if (!result) {
    if (errorCode.value === 'material_not_in_bom')
      localError.value = t('consume.materialNotInBom')
    else if (errorCode.value === 'order_locked')
      localError.value = t('consume.orderLocked')
    else
      localError.value = consumeError.value?.message ?? t('common.retry')
    return
  }

  emit('consumed')
  close()
}
</script>

<template>
  <Dialog
    :open="props.open"
    :title="t('consume.title')"
    @update:open="(v) => { if (!v) close() }"
  >
    <form id="stock-consume-form" class="flex flex-col gap-4" @submit.prevent="submit">
      <div v-if="!props.preselectedOrderId" class="flex flex-col gap-1.5">
        <Label for="consume-order-search">{{ t('consume.pickOrder') }}</Label>
        <Input
          id="consume-order-search"
          v-model="orderSearch"
          :placeholder="t('consume.searchOrder')"
        />
        <Select
          v-model="selectedOrderId"
          :options="orderOptions"
          :disabled="activeOrdersLoading"
          :placeholder="t('consume.pickOrder')"
        />
      </div>

      <div class="flex flex-col gap-1.5">
        <Label for="consume-material">{{ t('consume.pickMaterial') }}</Label>
        <Select
          id="consume-material"
          v-model="selectedMaterialId"
          :options="materialOptions"
          :disabled="loadingOrderData || !selectedOrderId"
          :placeholder="t('consume.pickMaterial')"
        />
      </div>

      <div class="flex flex-col gap-1.5">
        <Label for="consume-quantity">{{ t('consume.quantity') }}</Label>
        <Input
          id="consume-quantity"
          v-model.number="quantity"
          type="number"
          min="0.0001"
          step="any"
          :disabled="consumeLoading"
          required
        />
      </div>

      <div class="flex flex-col gap-1.5">
        <Label for="consume-comment">{{ t('consume.comment') }}</Label>
        <Textarea
          id="consume-comment"
          v-model="comment"
          :rows="2"
          :disabled="consumeLoading"
        />
      </div>

      <p v-if="overconsumption > 0" class="text-sm text-warning">
        {{ t('consume.overconsumption', { value: overconsumption }) }}
      </p>

      <p v-if="availableStock !== null" class="text-sm text-danger">
        {{ t('consume.insufficientStock', { available: availableStock }) }}
      </p>

      <p v-if="localError" class="text-sm text-danger">
        {{ localError }}
      </p>
    </form>

    <template #footer>
      <Button type="button" variant="ghost" :disabled="consumeLoading" @click="close">
        {{ t('common.cancel') }}
      </Button>
      <Button
        type="submit"
        form="stock-consume-form"
        :loading="consumeLoading"
        :disabled="consumeLoading || !selectedOrderId || !selectedMaterialId || quantity === undefined || quantity <= 0"
      >
        {{ t('consume.submit') }}
      </Button>
    </template>
  </Dialog>
</template>
