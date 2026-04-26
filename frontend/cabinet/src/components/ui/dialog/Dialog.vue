<script setup lang="ts">
import {
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogOverlay,
  DialogPortal,
  DialogRoot,
  DialogTitle,
} from 'radix-vue'
import { X } from 'lucide-vue-next'
import { cn } from '@/lib/utils'

const props = defineProps<{
  open: boolean
  title?: string
  description?: string
  hideClose?: boolean
}>()

const emit = defineEmits<{ (e: 'update:open', value: boolean): void }>()

function onOpenChange(value: boolean): void {
  emit('update:open', value)
}
</script>

<template>
  <DialogRoot :open="props.open" @update:open="onOpenChange">
    <DialogPortal>
      <DialogOverlay
        :class="cn(
          'fixed inset-0 z-40 bg-overlay backdrop-blur-sm',
          'data-[state=open]:animate-in data-[state=open]:fade-in-0',
          'data-[state=closed]:animate-out data-[state=closed]:fade-out-0',
        )"
      />
      <DialogContent
        :class="cn(
          'fixed left-1/2 top-1/2 z-50 w-[min(560px,calc(100vw-2rem))] -translate-x-1/2 -translate-y-1/2',
          'rounded-lg border border-border bg-elevated p-6 shadow-elevated',
          'data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=open]:zoom-in-95',
        )"
      >
        <header class="mb-4 flex items-start justify-between gap-4">
          <div class="flex flex-col gap-1">
            <DialogTitle v-if="title" class="text-lg font-semibold text-ink-strong">
              {{ title }}
            </DialogTitle>
            <DialogDescription v-if="description" class="text-sm text-ink-muted">
              {{ description }}
            </DialogDescription>
          </div>
          <DialogClose
            v-if="!hideClose"
            class="rounded p-1 text-ink-muted hover:bg-bg hover:text-ink-strong focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
            aria-label="Закрыть"
          >
            <X class="size-4" />
          </DialogClose>
        </header>
        <slot />
        <footer v-if="$slots.footer" class="mt-6 flex items-center justify-end gap-2">
          <slot name="footer" />
        </footer>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
