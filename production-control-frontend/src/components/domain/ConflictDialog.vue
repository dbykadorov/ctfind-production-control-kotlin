<script setup lang="ts">
import { AlertTriangle } from 'lucide-vue-next'
/**
 * Диалог конфликта оптимистической блокировки (FR-021).
 * Появляется при `TimestampMismatchError`. Без авто-merge: пользователь сам выбирает,
 * перезагрузить страницу (потерять локальные правки) или открыть копию в новой вкладке
 * (см. data-model.md §4.3, contracts/http-endpoints.md §Optimistic concurrency).
 */
import { Button, Dialog } from '@/components/ui'

const props = defineProps<{
  open: boolean
  /** Имя заказа, чтобы построить ссылку «Открыть копию». */
  orderName?: string
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'reload'): void
  (e: 'open-copy'): void
}>()

function onUpdateOpen(value: boolean): void {
  emit('update:open', value)
}

function reload(): void {
  emit('reload')
  emit('update:open', false)
}

function openCopy(): void {
  if (props.orderName) {
    window.open(`/cabinet/orders/${props.orderName}`, '_blank', 'noopener')
  }
  emit('open-copy')
  emit('update:open', false)
}
</script>

<template>
  <Dialog
    :open="open"
    title="Заказ изменён другим пользователем"
    description="Чтобы избежать перезаписи изменений, выберите, как продолжить."
    hide-close
    @update:open="onUpdateOpen"
  >
    <div class="flex items-start gap-3 rounded border border-warning/30 bg-warning/5 p-3 text-sm text-ink">
      <AlertTriangle class="mt-0.5 size-4 shrink-0 text-warning" aria-hidden="true" />
      <div class="space-y-1">
        <p>
          Пока вы редактировали этот заказ, его изменили в другой вкладке или другим
          пользователем.
        </p>
        <p class="text-ink-muted">
          Можно перезагрузить страницу (ваши незакоммиченные правки будут утеряны) или
          открыть актуальную копию в новой вкладке, чтобы перенести изменения вручную.
        </p>
      </div>
    </div>

    <template #footer>
      <Button variant="secondary" @click="openCopy">
        Открыть копию
      </Button>
      <Button variant="primary" @click="reload">
        Перезагрузить
      </Button>
    </template>
  </Dialog>
</template>
