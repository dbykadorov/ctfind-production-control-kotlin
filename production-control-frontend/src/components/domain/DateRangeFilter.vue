<script setup lang="ts">
import { format, parseISO } from 'date-fns'
import { ru } from 'date-fns/locale'
import { CalendarRange, X } from 'lucide-vue-next'
/**
 * Фильтр диапазона дат для списка заказов (поле delivery_date).
 * Минималистичная реализация без календарного picker — два type=date input'а
 * в Popover. На MVP это закрывает SC-002 / FR-013.
 */
import { computed, ref } from 'vue'
import { Button, Input, Label, Popover } from '@/components/ui'

const props = defineProps<{
  modelValue: { from?: string, to?: string }
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: { from?: string, to?: string }): void
}>()

const open = ref(false)

const draftFrom = ref(props.modelValue.from ?? '')
const draftTo = ref(props.modelValue.to ?? '')

function fmt(iso?: string): string | null {
  if (!iso)
    return null
  try {
    return format(parseISO(iso), 'd MMM yyyy', { locale: ru })
  }
  catch {
    return iso
  }
}

const buttonLabel = computed(() => {
  const fromLabel = fmt(props.modelValue.from)
  const toLabel = fmt(props.modelValue.to)
  if (fromLabel && toLabel)
    return `${fromLabel} — ${toLabel}`
  if (fromLabel)
    return `с ${fromLabel}`
  if (toLabel)
    return `по ${toLabel}`
  return 'Срок исполнения'
})

const isActive = computed(() => Boolean(props.modelValue.from || props.modelValue.to))

function syncDraft(): void {
  draftFrom.value = props.modelValue.from ?? ''
  draftTo.value = props.modelValue.to ?? ''
}

function onOpenChange(value: boolean): void {
  open.value = value
  if (value)
    syncDraft()
}

function apply(): void {
  emit('update:modelValue', {
    from: draftFrom.value || undefined,
    to: draftTo.value || undefined,
  })
  open.value = false
}

function clear(): void {
  draftFrom.value = ''
  draftTo.value = ''
  emit('update:modelValue', { from: undefined, to: undefined })
  open.value = false
}
</script>

<template>
  <Popover :open="open" @update:open="onOpenChange">
    <template #trigger>
      <Button
        variant="secondary"
        size="md"
        :class="isActive ? 'border-brand-500 text-brand-500' : ''"
      >
        <CalendarRange class="size-4" aria-hidden="true" />
        {{ buttonLabel }}
        <button
          v-if="isActive"
          type="button"
          aria-label="Сбросить диапазон"
          class="-mr-1 ml-1 rounded p-0.5 text-ink-muted hover:bg-bg hover:text-ink-strong"
          @click.stop="clear"
        >
          <X class="size-3" aria-hidden="true" />
        </button>
      </Button>
    </template>

    <div class="flex w-72 flex-col gap-3">
      <div class="space-y-1">
        <Label for="date-range-from">С</Label>
        <Input id="date-range-from" v-model="draftFrom" type="date" />
      </div>
      <div class="space-y-1">
        <Label for="date-range-to">По</Label>
        <Input id="date-range-to" v-model="draftTo" type="date" />
      </div>
      <div class="flex justify-between gap-2 pt-1">
        <Button variant="ghost" size="sm" @click="clear">
          Сбросить
        </Button>
        <Button variant="primary" size="sm" @click="apply">
          Применить
        </Button>
      </div>
    </div>
  </Popover>
</template>
