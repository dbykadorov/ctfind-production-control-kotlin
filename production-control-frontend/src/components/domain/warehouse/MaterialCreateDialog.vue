<script setup lang="ts">
import type { MeasurementUnit } from '@/api/types/warehouse'
import { ref } from 'vue'
import { httpClient } from '@/api/api-client'
import { toApiError } from '@/utils/errors'
import { Dialog } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'created'): void
}>()

const { t } = useI18n()

const name = ref('')
const unit = ref<MeasurementUnit>('PIECE')
const loading = ref(false)
const fieldError = ref<string | null>(null)

const unitOptions: { value: MeasurementUnit; label: string }[] = [
  { value: 'PIECE', label: 'шт' },
  { value: 'KILOGRAM', label: 'кг' },
  { value: 'METER', label: 'м' },
  { value: 'LITER', label: 'л' },
  { value: 'SQUARE_METER', label: 'м²' },
  { value: 'CUBIC_METER', label: 'м³' },
]

function close() {
  name.value = ''
  unit.value = 'PIECE'
  fieldError.value = null
  emit('update:open', false)
}

async function submit() {
  fieldError.value = null
  if (!name.value.trim()) {
    fieldError.value = t('warehouse.fields.name') + ' обязательно'
    return
  }
  loading.value = true
  try {
    await httpClient.post('/api/materials', { name: name.value.trim(), unit: unit.value })
    emit('created')
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
    :title="t('warehouse.addMaterial')"
    @update:open="(v) => { if (!v) close() }"
  >
    <form id="material-create-form" class="flex flex-col gap-4" @submit.prevent="submit">
      <div class="flex flex-col gap-1.5">
        <Label for="material-name">{{ t('warehouse.fields.name') }}</Label>
        <Input
          id="material-name"
          v-model="name"
          :placeholder="t('warehouse.fields.name')"
          :disabled="loading"
          :invalid="!!fieldError"
          required
        />
      </div>

      <div class="flex flex-col gap-1.5">
        <Label for="material-unit">{{ t('warehouse.fields.unit') }}</Label>
        <Select
          id="material-unit"
          v-model="unit"
          :options="unitOptions"
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
        form="material-create-form"
        :disabled="loading || !name.trim()"
        :loading="loading"
      >
        {{ t('common.create') }}
      </Button>
    </template>
  </Dialog>
</template>
