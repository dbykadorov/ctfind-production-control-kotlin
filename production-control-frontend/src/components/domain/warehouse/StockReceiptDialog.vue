<script setup lang="ts">
import { ref } from 'vue'
import { httpClient } from '@/api/api-client'
import { toApiError } from '@/utils/errors'
import { Dialog } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  open: boolean
  materialId: string
  materialName?: string
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'received'): void
}>()

const { t } = useI18n()

const quantity = ref<number | undefined>(undefined)
const comment = ref('')
const loading = ref(false)
const fieldError = ref<string | null>(null)

function close() {
  quantity.value = undefined
  comment.value = ''
  fieldError.value = null
  emit('update:open', false)
}

async function submit() {
  fieldError.value = null
  if (quantity.value === undefined || quantity.value <= 0) {
    fieldError.value = t('warehouse.fields.quantity') + ' должно быть > 0'
    return
  }
  loading.value = true
  try {
    await httpClient.post(`/api/materials/${props.materialId}/receipt`, {
      quantity: quantity.value,
      comment: comment.value.trim() || undefined,
    })
    emit('received')
    close()
  }
  catch (e) {
    const apiErr = toApiError(e)
    fieldError.value = apiErr.message ?? t('common.retry')
  }
  finally {
    loading.value = false
  }
}
</script>

<template>
  <Dialog
    :open="props.open"
    :title="t('warehouse.receipt')"
    @update:open="(v) => { if (!v) close() }"
  >
    <form id="receipt-form" class="flex flex-col gap-4" @submit.prevent="submit">
      <div class="flex flex-col gap-1.5">
        <Label for="receipt-quantity">{{ t('warehouse.fields.quantity') }}</Label>
        <Input
          id="receipt-quantity"
          v-model.number="quantity"
          type="number"
          min="0.0001"
          step="any"
          :placeholder="t('warehouse.fields.quantity')"
          :disabled="loading"
          :invalid="!!fieldError"
          required
        />
      </div>

      <div class="flex flex-col gap-1.5">
        <Label for="receipt-comment">{{ t('warehouse.fields.comment') }}</Label>
        <Textarea
          id="receipt-comment"
          v-model="comment"
          :placeholder="t('warehouse.fields.comment')"
          :disabled="loading"
          :rows="2"
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
        form="receipt-form"
        :disabled="loading || quantity === undefined || quantity <= 0"
        :loading="loading"
      >
        {{ t('warehouse.receipt') }}
      </Button>
    </template>
  </Dialog>
</template>
