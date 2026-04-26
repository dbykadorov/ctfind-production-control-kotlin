<script setup lang="ts">
import type { OrderTransition } from '@/api/types/domain'
/**
 * Кнопки workflow-переходов для заказа.
 * Контракт: один клик = один переход (FR-022, SC-003). После успешного перехода
 * вызываем `onApplied` (parent reload) и показываем toast. Никакого confirm на MVP.
 */
import { ref } from 'vue'
import { toast } from 'vue-sonner'
import { applyTransition } from '@/api/composables/use-workflow'
import { Button } from '@/components/ui'
import { toApiError } from '@/utils/errors'

const props = defineProps<{
  orderName: string
  transitions: OrderTransition[]
  loading?: boolean
  /** Внешний disabled (например, для Shop Supervisor, US3). */
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'applied', action: string): void
}>()

const pendingAction = ref<string | null>(null)

async function trigger(action: string): Promise<void> {
  if (pendingAction.value)
    return
  pendingAction.value = action
  try {
    await applyTransition(props.orderName, action)
    toast.success(`Заказ переведён: ${action}`)
    emit('applied', action)
  }
  catch (e) {
    const apiErr = toApiError(e)
    toast.error(apiErr.message || 'Не удалось выполнить переход')
  }
  finally {
    pendingAction.value = null
  }
}
</script>

<template>
  <div class="flex flex-wrap items-center gap-2">
    <template v-if="!loading && transitions.length === 0">
      <span class="text-sm text-ink-muted">
        Нет доступных действий
      </span>
    </template>
    <template v-else>
      <Button
        v-for="t in transitions"
        :key="t.action"
        variant="primary"
        :loading="pendingAction === t.action"
        :disabled="disabled || (pendingAction !== null && pendingAction !== t.action)"
        @click="trigger(t.action)"
      >
        {{ t.action }}
      </Button>
    </template>
  </div>
</template>
