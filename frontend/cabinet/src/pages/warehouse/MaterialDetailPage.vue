<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMaterialDetail, useMaterialMovements } from '@/api/composables/use-materials'
import { usePermissions } from '@/api/composables/use-permissions'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import StockReceiptDialog from '@/components/domain/warehouse/StockReceiptDialog.vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{ id: string }>()

const { t } = useI18n()
const router = useRouter()
const permissions = usePermissions()

const { data: material, loading: materialLoading, refetch: refetchMaterial } = useMaterialDetail(props.id)
const {
  data: movements,
  loading: movementsLoading,
  page,
  totalPages,
  refetch: refetchMovements,
  nextPage,
  prevPage,
} = useMaterialMovements(props.id)

const showReceiptDialog = ref(false)

function onReceived() {
  refetchMaterial()
  refetchMovements()
}
</script>

<template>
  <div class="flex flex-col gap-6 p-6">
    <div class="flex items-center gap-4">
      <Button variant="ghost" size="sm" @click="router.push('/cabinet/warehouse')">
        ← {{ t('warehouse.title') }}
      </Button>
    </div>

    <div v-if="materialLoading">
      <Skeleton class="h-24 w-full rounded" />
    </div>

    <template v-else-if="material">
      <div class="flex items-start justify-between">
        <div class="flex flex-col gap-1">
          <h1 class="text-2xl font-semibold text-ink-strong">
            {{ material.name }}
          </h1>
          <p class="text-ink-muted">
            {{ t('warehouse.fields.unit') }}: {{ t(`warehouse.units.${material.unit}`) }}
          </p>
          <p class="text-ink-muted">
            {{ t('warehouse.currentStock') }}: <span class="font-mono text-ink-strong">{{ material.currentStock }}</span>
          </p>
        </div>
        <Button
          v-if="permissions.isWarehouse || permissions.isAdmin"
          @click="showReceiptDialog = true"
        >
          {{ t('warehouse.receipt') }}
        </Button>
      </div>

      <h2 class="text-lg font-semibold text-ink-strong">
        {{ t('warehouse.movements') }}
      </h2>

      <div v-if="movementsLoading">
        <Skeleton v-for="i in 5" :key="i" class="h-10 w-full rounded mb-1" />
      </div>

      <template v-else>
        <div v-if="movements.length === 0" class="text-center text-ink-muted py-8">
          {{ t('common.empty') }}
        </div>

        <table v-else class="w-full text-sm">
          <thead>
            <tr class="border-b border-border text-ink-muted">
              <th class="py-2 text-left font-medium">Дата</th>
              <th class="py-2 text-left font-medium">{{ t('warehouse.movement.RECEIPT') }}</th>
              <th class="py-2 text-right font-medium">{{ t('warehouse.fields.quantity') }}</th>
              <th class="py-2 text-left font-medium">{{ t('warehouse.fields.comment') }}</th>
              <th class="py-2 text-left font-medium">{{ t('warehouse.fields.actorDisplayName') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="movement in movements"
              :key="movement.id"
              class="border-b border-border last:border-0"
            >
              <td class="py-2 text-ink-muted">{{ new Date(movement.createdAt).toLocaleDateString('ru-RU') }}</td>
              <td class="py-2 text-ink-strong">{{ t(`warehouse.movement.${movement.movementType}`) }}</td>
              <td class="py-2 text-right font-mono text-ink-strong">{{ movement.quantity }}</td>
              <td class="py-2 text-ink-muted">{{ movement.comment ?? '—' }}</td>
              <td class="py-2 text-ink-muted">{{ movement.actorDisplayName }}</td>
            </tr>
          </tbody>
        </table>

        <div v-if="totalPages > 1" class="flex items-center justify-between text-sm">
          <Button variant="ghost" size="sm" :disabled="page === 0" @click="prevPage">
            ←
          </Button>
          <span class="text-ink-muted">{{ page + 1 }} / {{ totalPages }}</span>
          <Button variant="ghost" size="sm" :disabled="page >= totalPages - 1" @click="nextPage">
            →
          </Button>
        </div>
      </template>

      <StockReceiptDialog
        v-model:open="showReceiptDialog"
        :material-id="material.id"
        :material-name="material.name"
        @received="onReceived"
      />
    </template>

    <div v-else class="text-center text-ink-muted py-12">
      {{ t('common.notFound') }}
    </div>
  </div>
</template>
