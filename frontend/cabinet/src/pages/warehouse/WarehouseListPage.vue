<script setup lang="ts">
import { ref, watch } from 'vue'
import { useMaterials } from '@/api/composables/use-materials'
import { usePermissions } from '@/api/composables/use-permissions'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import MaterialCreateDialog from '@/components/domain/warehouse/MaterialCreateDialog.vue'
import StockReceiptDialog from '@/components/domain/warehouse/StockReceiptDialog.vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const permissions = usePermissions()

const {
  data: materials,
  loading,
  error,
  page,
  totalPages,
  search,
  refetch,
  nextPage,
  prevPage,
} = useMaterials()

const showCreateDialog = ref(false)
const receiptMaterialId = ref<string | null>(null)
const showReceiptDialog = ref(false)

let searchTimeout: ReturnType<typeof setTimeout> | null = null
watch(search, () => {
  if (searchTimeout)
    clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    page.value = 0
    refetch()
  }, 300)
})

function openReceipt(materialId: string) {
  receiptMaterialId.value = materialId
  showReceiptDialog.value = true
}

function onCreated() {
  refetch()
}

function onReceived() {
  refetch()
}
</script>

<template>
  <div class="flex flex-col gap-6 p-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-semibold text-ink-strong">
        {{ t('warehouse.title') }}
      </h1>
      <Button
        v-if="permissions.isWarehouse || permissions.isAdmin"
        @click="showCreateDialog = true"
      >
        {{ t('warehouse.addMaterial') }}
      </Button>
    </div>

    <Input
      v-model="search"
      :placeholder="t('warehouse.search')"
      class="max-w-sm"
    />

    <div v-if="loading" class="flex flex-col gap-2">
      <Skeleton v-for="i in 5" :key="i" class="h-12 w-full rounded" />
    </div>

    <div v-else-if="error" class="text-sm text-danger">
      {{ error.message }}
    </div>

    <template v-else>
      <div v-if="materials.length === 0" class="text-center text-ink-muted py-12">
        {{ t('warehouse.emptyMaterials') }}
      </div>

      <table v-else class="w-full text-sm">
        <thead>
          <tr class="border-b border-border text-ink-muted">
            <th class="py-2 text-left font-medium">{{ t('warehouse.fields.name') }}</th>
            <th class="py-2 text-left font-medium">{{ t('warehouse.fields.unit') }}</th>
            <th class="py-2 text-right font-medium">{{ t('warehouse.currentStock') }}</th>
            <th class="py-2" />
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="material in materials"
            :key="material.id"
            class="border-b border-border last:border-0"
          >
            <td class="py-3 text-ink-strong">{{ material.name }}</td>
            <td class="py-3 text-ink-muted">{{ t(`warehouse.units.${material.unit}`) }}</td>
            <td class="py-3 text-right font-mono text-ink-strong">{{ material.currentStock }}</td>
            <td class="py-3 text-right">
              <Button
                v-if="permissions.isWarehouse || permissions.isAdmin"
                size="sm"
                variant="secondary"
                @click="openReceipt(material.id)"
              >
                {{ t('warehouse.receipt') }}
              </Button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="totalPages > 1" class="flex items-center justify-between text-sm">
        <Button variant="ghost" size="sm" :disabled="page === 0" @click="prevPage">
          {{ t('common.actions') }} ←
        </Button>
        <span class="text-ink-muted">{{ page + 1 }} / {{ totalPages }}</span>
        <Button variant="ghost" size="sm" :disabled="page >= totalPages - 1" @click="nextPage">
          → {{ t('common.actions') }}
        </Button>
      </div>
    </template>

    <MaterialCreateDialog
      v-model:open="showCreateDialog"
      @created="onCreated"
    />

    <StockReceiptDialog
      v-if="receiptMaterialId"
      v-model:open="showReceiptDialog"
      :material-id="receiptMaterialId"
      @received="onReceived"
    />
  </div>
</template>
