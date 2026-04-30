<script setup lang="ts">
import { ref, watch } from 'vue'
import { useOrderMaterialUsage } from '@/api/composables/use-order-material-usage'
import { usePermissions } from '@/api/composables/use-permissions'
import StockConsumeDialog from '@/components/domain/warehouse/StockConsumeDialog.vue'
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
  (e: 'consumed'): void
}>()

const { t } = useI18n()
const permissions = usePermissions()
const { usage, loading, error, refetch } = useOrderMaterialUsage(props.orderId)

const consumeOpen = ref(false)

watch(() => props.refreshKey, () => {
  refetch()
})

function openConsume() {
  consumeOpen.value = true
}

function onConsumed() {
  refetch()
  emit('consumed')
}
</script>

<template>
  <Card class="space-y-4 p-5">
    <div class="flex items-center justify-between gap-2">
      <h3 class="text-base font-semibold text-ink-strong">
        {{ t('usage.section.title') }}
      </h3>
      <Button
        v-if="permissions.canConsumeStock && !props.orderShipped"
        size="sm"
        @click="openConsume"
      >
        {{ t('usage.consumeButton') }}
      </Button>
    </div>

    <div v-if="loading" class="space-y-2">
      <Skeleton v-for="i in 3" :key="i" class="h-10 w-full rounded" />
    </div>

    <template v-else>
      <p v-if="error" class="text-sm text-danger">
        {{ error.message }}
      </p>

      <p v-if="!usage || usage.rows.length === 0" class="text-sm text-ink-muted">
        {{ t('usage.empty') }}
      </p>

      <table v-else class="w-full text-sm">
        <thead>
          <tr class="border-b border-border text-ink-muted">
            <th class="py-2 text-left font-medium">{{ t('warehouse.fields.name') }}</th>
            <th class="py-2 text-right font-medium">{{ t('usage.column.required') }}</th>
            <th class="py-2 text-right font-medium">{{ t('usage.column.consumed') }}</th>
            <th class="py-2 text-right font-medium">{{ t('usage.column.remaining') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in usage.rows" :key="row.materialId" class="border-b border-border last:border-0">
            <td class="py-2 text-ink-strong">
              {{ row.materialName }} ({{ t(`warehouse.units.${row.materialUnit}`) }})
            </td>
            <td class="py-2 text-right font-mono text-ink-strong">
              {{ row.requiredQuantity }}
            </td>
            <td class="py-2 text-right font-mono text-ink-strong">
              {{ row.consumedQuantity }}
              <span v-if="row.overconsumption > 0" class="ml-2 text-warning">
                {{ t('usage.overconsumption', { value: row.overconsumption }) }}
              </span>
            </td>
            <td class="py-2 text-right font-mono text-ink-muted">
              {{ row.remainingToConsume }}
            </td>
          </tr>
        </tbody>
      </table>
    </template>

    <StockConsumeDialog
      v-model:open="consumeOpen"
      :preselected-order-id="props.orderId"
      @consumed="onConsumed"
    />
  </Card>
</template>
