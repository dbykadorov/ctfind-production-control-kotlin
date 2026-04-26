<script setup lang="ts">
import {
  PopoverArrow,
  PopoverContent,
  PopoverPortal,
  PopoverRoot,
  PopoverTrigger,
} from 'radix-vue'
import { cn } from '@/lib/utils'

defineProps<{
  open?: boolean
  align?: 'start' | 'center' | 'end'
  sideOffset?: number
}>()

const emit = defineEmits<{ (e: 'update:open', value: boolean): void }>()

function onOpenChange(value: boolean): void {
  emit('update:open', value)
}
</script>

<template>
  <PopoverRoot :open="open" @update:open="onOpenChange">
    <PopoverTrigger as-child>
      <slot name="trigger" />
    </PopoverTrigger>
    <PopoverPortal>
      <PopoverContent
        :align="align ?? 'start'"
        :side-offset="sideOffset ?? 6"
        :class="cn(
          'z-50 w-auto rounded border border-border bg-elevated p-3 text-ink shadow-elevated',
          'data-[state=open]:animate-in data-[state=open]:fade-in-0',
        )"
      >
        <slot />
        <PopoverArrow class="fill-elevated" />
      </PopoverContent>
    </PopoverPortal>
  </PopoverRoot>
</template>
