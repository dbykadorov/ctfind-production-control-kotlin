<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{
  modelValue?: string | number
  type?: string
  placeholder?: string
  disabled?: boolean
  invalid?: boolean
  id?: string
  autocomplete?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'blur', event: FocusEvent): void
  (e: 'focus', event: FocusEvent): void
}>()

const value = computed({
  get: () => props.modelValue ?? '',
  set: v => emit('update:modelValue', String(v)),
})
</script>

<template>
  <input
    :id="id"
    v-model="value"
    :type="type ?? 'text'"
    :placeholder="placeholder"
    :disabled="disabled"
    :autocomplete="autocomplete"
    :aria-invalid="invalid || undefined"
    :class="cn(
      'h-10 w-full rounded border bg-surface px-3 text-sm text-ink-strong placeholder:text-ink-muted',
      'focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/20',
      'disabled:cursor-not-allowed disabled:bg-bg disabled:opacity-60',
      invalid ? 'border-danger' : 'border-border',
    )"
    @blur="(e) => emit('blur', e)"
    @focus="(e) => emit('focus', e)"
  />
</template>
