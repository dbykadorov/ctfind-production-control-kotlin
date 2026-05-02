<script setup lang="ts">
import type { BomLine, MaterialResponse } from '@/api/types/warehouse'
import { computed, ref, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { useI18n } from 'vue-i18n'

interface BomLineDraft {
  materialId: string
  quantity: number
  comment?: string
}

const props = defineProps<{
  open: boolean
  orderId: string
  availableMaterials: MaterialResponse[]
  editingLine?: BomLine | null
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'saved', draft: BomLineDraft): void
}>()

const { t } = useI18n()

const materialId = ref<string | null>(null)
const quantity = ref<number | undefined>(undefined)
const comment = ref('')
const fieldError = ref<string | null>(null)
const loading = ref(false)

const materialOptions = computed(() =>
  props.availableMaterials.map(material => ({
    value: material.id,
    label: `${material.name} (${t(`warehouse.units.${material.unit}`)})`,
  })),
)

const isEditMode = computed(() => Boolean(props.editingLine))

watch(
  () => props.open,
  (isOpen) => {
    if (!isOpen)
      return
    materialId.value = props.editingLine?.materialId ?? null
    quantity.value = props.editingLine?.quantity
    comment.value = props.editingLine?.comment ?? ''
    fieldError.value = null
    loading.value = false
  },
  { immediate: true },
)

function close() {
  emit('update:open', false)
}

function submit() {
  fieldError.value = null
  if (!materialId.value) {
    fieldError.value = t('bom.materialRequired')
    return
  }
  if (quantity.value === undefined) {
    fieldError.value = t('bom.quantityRequired')
    return
  }
  if (quantity.value <= 0) {
    fieldError.value = t('bom.quantityPositive')
    return
  }

  emit('saved', {
    materialId: materialId.value,
    quantity: quantity.value,
    comment: comment.value.trim() || undefined,
  })
}
</script>

<template>
  <Dialog
    :open="props.open"
    :title="isEditMode ? t('bom.edit') : t('bom.add')"
    @update:open="(v) => { if (!v) close() }"
  >
    <form id="bom-line-form" class="flex flex-col gap-4" @submit.prevent="submit">
      <div class="flex flex-col gap-1.5">
        <Label for="bom-material">{{ t('warehouse.fields.name') }}</Label>
        <Select
          id="bom-material"
          v-model="materialId"
          :options="materialOptions"
          :disabled="loading || isEditMode"
          :placeholder="t('consume.pickMaterial')"
        />
      </div>

      <div class="flex flex-col gap-1.5">
        <Label for="bom-quantity">{{ t('warehouse.fields.quantity') }}</Label>
        <Input
          id="bom-quantity"
          v-model.number="quantity"
          type="number"
          min="0.0001"
          step="any"
          :disabled="loading"
          :invalid="!!fieldError"
          required
        />
      </div>

      <div class="flex flex-col gap-1.5">
        <Label for="bom-comment">{{ t('warehouse.fields.comment') }}</Label>
        <Textarea
          id="bom-comment"
          v-model="comment"
          :rows="2"
          :disabled="loading"
        />
      </div>

      <p v-if="fieldError" class="text-sm text-danger">
        {{ fieldError }}
      </p>
    </form>

    <template #footer>
      <Button type="button" variant="ghost" :disabled="loading" @click="close">
        {{ t('common.cancel') }}
      </Button>
      <Button
        type="submit"
        form="bom-line-form"
        :disabled="loading || !materialId || quantity === undefined || quantity <= 0"
      >
        {{ isEditMode ? t('bom.edit') : t('bom.add') }}
      </Button>
    </template>
  </Dialog>
</template>
