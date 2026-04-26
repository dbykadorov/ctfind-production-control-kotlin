<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{
  modelValue?: string
  placeholder?: string
  rows?: number
  disabled?: boolean
  invalid?: boolean
  id?: string
}>()

const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()

const value = computed({
  get: () => props.modelValue ?? '',
  set: v => emit('update:modelValue', v),
})
</script>

<template>
  <textarea
    :id="id"
    v-model="value"
    :rows="rows ?? 3"
    :placeholder="placeholder"
    :disabled="disabled"
    :aria-invalid="invalid || undefined"
    :class="cn(
      'w-full rounded border bg-surface px-3 py-2 text-sm text-ink-strong placeholder:text-ink-muted',
      'focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/20',
      'disabled:cursor-not-allowed disabled:bg-bg disabled:opacity-60',
      'resize-y min-h-[5rem]',
      invalid ? 'border-danger' : 'border-border',
    )"
  />
</template>
