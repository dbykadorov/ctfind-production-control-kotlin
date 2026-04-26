<script setup lang="ts">
import { cn } from '@/lib/utils'

defineProps<{ for?: string, required?: boolean }>()
</script>

<template>
  <label
    :for="$props.for"
    :class="cn('inline-flex items-center gap-1 text-sm font-medium text-ink-strong')"
  >
    <slot />
    <span v-if="required" aria-hidden="true" class="text-danger">*</span>
  </label>
</template>
