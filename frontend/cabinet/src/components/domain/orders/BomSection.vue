<script setup lang="ts">
import type { BomLine } from '@/api/types/warehouse'
import { ref, watch } from 'vue'
import { useOrderBom } from '@/api/composables/use-order-bom'
import { usePermissions } from '@/api/composables/use-permissions'
import { useMaterials } from '@/api/composables/use-materials'
import BomLineDialog from '@/components/domain/orders/BomLineDialog.vue'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { useI18n } from 'vue-i18n'

const props = withDefaults(defineProps<{
  orderId: string
  orderShipped: boolean
  refreshKey?: number
}>(), {
  refreshKey: 0,
})

const emit = defineEmits<{
  (e: 'changed'): void
}>()

const { t } = useI18n()
const permissions = usePermissions()
const canEdit = () => permissions.value.canEditOrderBom && !props.orderShipped

const { lines, loading, error, refetch, addLine, updateLine, removeLine } = useOrderBom(props.orderId)
const { data: materials } = useMaterials(200)

const dialogOpen = ref(false)
const editingLine = ref<BomLine | null>(null)
const actionError = ref<string | null>(null)

watch(() => props.refreshKey, () => {
  refetch()
})

function openCreate() {
  editingLine.value = null
  actionError.value = null
  dialogOpen.value = true
}

function openEdit(line: BomLine) {
  editingLine.value = line
  actionError.value = null
  dialogOpen.value = true
}

function mapBomError(): string {
  const raw = error.value?.raw as { error?: string } | undefined
  switch (raw?.error) {
    case 'bom_line_duplicate':
      return t('bom.duplicate')
    case 'bom_line_has_consumption':
      return t('bom.cannotDeleteWithConsumption')
    case 'order_locked':
      return t('bom.lockedShipped')
    default:
      return error.value?.message ?? t('common.retry')
  }
}

async function onDialogSaved(payload: { materialId: string, quantity: number, comment?: string }) {
  actionError.value = null
  const result = editingLine.value
    ? await updateLine(editingLine.value.id, { quantity: payload.quantity, comment: payload.comment })
    : await addLine(payload)
  if (!result) {
    actionError.value = mapBomError()
    return
  }
  dialogOpen.value = false
  editingLine.value = null
  emit('changed')
}

async function onDelete(line: BomLine) {
  if (!confirm(t('bom.confirmDelete')))
    return
  const ok = await removeLine(line.id)
  if (!ok) {
    actionError.value = mapBomError()
    return
  }
  emit('changed')
}
</script>

<template>
  <Card class="space-y-4 p-5">
    <div class="flex items-center justify-between gap-2">
      <h3 class="text-base font-semibold text-ink-strong">
        {{ t('bom.section.title') }}
      </h3>
      <Button
        v-if="canEdit()"
        size="sm"
        @click="openCreate"
      >
        {{ t('bom.add') }}
      </Button>
    </div>

    <div v-if="loading" class="space-y-2">
      <Skeleton v-for="i in 3" :key="i" class="h-10 w-full rounded" />
    </div>

    <template v-else>
      <p v-if="props.orderShipped" class="text-sm text-ink-muted">
        {{ t('bom.lockedShipped') }}
      </p>
      <p v-if="actionError" class="text-sm text-danger">
        {{ actionError }}
      </p>
      <p v-if="error" class="text-sm text-danger">
        {{ error.message }}
      </p>

      <p v-if="lines.length === 0" class="text-sm text-ink-muted">
        {{ t('bom.empty') }}
      </p>

      <table v-else class="w-full text-sm">
        <thead>
          <tr class="border-b border-border text-ink-muted">
            <th class="py-2 text-left font-medium">{{ t('warehouse.fields.name') }}</th>
            <th class="py-2 text-right font-medium">{{ t('usage.column.required') }}</th>
            <th class="py-2 text-left font-medium">{{ t('warehouse.fields.comment') }}</th>
            <th class="py-2 text-right font-medium">{{ t('common.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="line in lines" :key="line.id" class="border-b border-border last:border-0">
            <td class="py-2 text-ink-strong">
              {{ line.materialName }} ({{ t(`warehouse.units.${line.materialUnit}`) }})
            </td>
            <td class="py-2 text-right font-mono text-ink-strong">
              {{ line.quantity }}
            </td>
            <td class="py-2 text-ink-muted">
              {{ line.comment || '—' }}
            </td>
            <td class="py-2 text-right">
              <div v-if="canEdit()" class="inline-flex gap-2">
                <Button size="sm" variant="ghost" @click="openEdit(line)">
                  {{ t('bom.edit') }}
                </Button>
                <Button size="sm" variant="ghost" @click="onDelete(line)">
                  {{ t('bom.delete') }}
                </Button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </template>

    <BomLineDialog
      v-model:open="dialogOpen"
      :order-id="props.orderId"
      :available-materials="materials"
      :editing-line="editingLine"
      @saved="onDialogSaved"
    />
  </Card>
</template>
