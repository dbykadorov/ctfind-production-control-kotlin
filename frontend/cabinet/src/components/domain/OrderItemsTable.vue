<script setup lang="ts">
import type { CustomerOrderItem, OrderEditability } from '@/api/types/domain'
import { Plus, Trash2 } from 'lucide-vue-next'
/**
 * Таблица позиций заказа (child table CustomerOrderItem).
 * Поддерживает два режима:
 *  - readonly (`mode === 'view'` или `editability.frozen.includes('items')`) — список без действий;
 *  - edit — добавление/удаление строк, инлайн-редактирование (item_name, quantity, uom).
 *
 * Двусторонняя связь через v-model. Не выполняет валидацию — она в Zod-схеме формы (US1).
 */
import { computed } from 'vue'
import { Button, Input } from '@/components/ui'

interface ItemRow {
  item_name: string
  quantity: number | null
  uom: string
  /** Локальный id для v-for (Frappe `name` для существующих, uuid для новых). */
  _key: string
}

const props = withDefaults(
  defineProps<{
    modelValue: CustomerOrderItem[]
    editability?: OrderEditability
    /** Жёсткий readonly (например, для US3 Shop Supervisor). */
    forceReadonly?: boolean
  }>(),
  { forceReadonly: false },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: CustomerOrderItem[]): void
}>()

const isReadonly = computed(() =>
  props.forceReadonly
  || props.editability?.frozen?.includes('items')
  || props.editability?.frozen?.includes('*')
  || props.editability?.readonly,
)

let nextKey = 0
function nextLocalKey(): string {
  nextKey += 1
  return `new-${Date.now()}-${nextKey}`
}

const rows = computed<ItemRow[]>(() =>
  props.modelValue.map(it => ({
    item_name: it.item_name ?? '',
    quantity: typeof it.quantity === 'number' ? it.quantity : (it.quantity ? Number(it.quantity) : null),
    uom: it.uom ?? '',
    _key: it.name ?? `existing-${it.item_name}-${it.quantity}-${it.uom}`,
  })),
)

function emitRows(updated: ItemRow[]): void {
  const next: CustomerOrderItem[] = updated.map((row, idx) => {
    const existing = props.modelValue[idx]
    return {
      ...(existing ?? { name: '', creation: '', modified: '', owner: '', modified_by: '' } as CustomerOrderItem),
      item_name: row.item_name.trim(),
      quantity: typeof row.quantity === 'number' ? row.quantity : 0,
      uom: row.uom.trim(),
    }
  })
  emit('update:modelValue', next)
}

function updateField(index: number, field: 'item_name' | 'quantity' | 'uom', value: string | number | null): void {
  const updated = [...rows.value]
  const current = updated[index]
  if (!current)
    return
  const target: ItemRow = { ...current }
  if (field === 'quantity') {
    const numeric = value === '' || value === null ? null : Number(value)
    target.quantity = Number.isFinite(numeric ?? Number.NaN) ? (numeric as number) : null
  }
  else {
    target[field] = String(value ?? '')
  }
  updated[index] = target
  emitRows(updated)
}

function addRow(): void {
  const updated: ItemRow[] = [
    ...rows.value,
    { item_name: '', quantity: 1, uom: 'шт', _key: nextLocalKey() },
  ]
  emitRows(updated)
}

function removeRow(index: number): void {
  const updated = rows.value.filter((_, i) => i !== index)
  emitRows(updated)
}
</script>

<template>
  <div class="space-y-3">
    <div class="overflow-hidden rounded-lg border border-border bg-surface">
      <table class="w-full text-sm">
        <thead class="bg-elevated text-left text-xs uppercase tracking-wide text-ink-muted">
          <tr>
            <th class="px-3 py-2 font-medium" scope="col">
              Наименование
            </th>
            <th class="w-32 px-3 py-2 text-right font-medium" scope="col">
              Количество
            </th>
            <th class="w-28 px-3 py-2 font-medium" scope="col">
              Ед.
            </th>
            <th v-if="!isReadonly" class="w-12 px-2 py-2" scope="col" />
          </tr>
        </thead>
        <tbody>
          <tr v-if="rows.length === 0">
            <td :colspan="isReadonly ? 3 : 4" class="px-3 py-6 text-center text-ink-muted">
              Позиций нет
            </td>
          </tr>
          <tr
            v-for="(row, index) in rows"
            :key="row._key"
            class="border-t border-border align-middle transition-colors hover:bg-elevated/60"
          >
            <td class="px-3 py-2">
              <span v-if="isReadonly" class="block truncate text-ink-strong">
                {{ row.item_name || '—' }}
              </span>
              <Input
                v-else
                :model-value="row.item_name"
                placeholder="Например, Опора СВ-110"
                aria-label="Наименование позиции"
                @update:model-value="(v: string) => updateField(index, 'item_name', v)"
              />
            </td>
            <td class="px-3 py-2 text-right">
              <span v-if="isReadonly" class="text-ink-strong">
                {{ row.quantity ?? '—' }}
              </span>
              <Input
                v-else
                type="number"
                inputmode="decimal"
                step="0.001"
                min="0"
                class="text-right"
                :model-value="row.quantity ?? ''"
                @update:model-value="(v: string) => updateField(index, 'quantity', v)"
              />
            </td>
            <td class="px-3 py-2">
              <span v-if="isReadonly" class="text-ink-strong">
                {{ row.uom || '—' }}
              </span>
              <Input
                v-else
                :model-value="row.uom"
                placeholder="шт, м, кг…"
                @update:model-value="(v: string) => updateField(index, 'uom', v)"
              />
            </td>
            <td v-if="!isReadonly" class="px-2 py-2 text-right">
              <Button
                variant="ghost"
                size="sm"
                aria-label="Удалить позицию"
                @click="removeRow(index)"
              >
                <Trash2 class="size-4" aria-hidden="true" />
              </Button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-if="!isReadonly" class="flex justify-start">
      <Button variant="secondary" size="sm" @click="addRow">
        <Plus class="size-4" aria-hidden="true" />
        Добавить позицию
      </Button>
    </div>
  </div>
</template>
