<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'subtle'
type Size = 'sm' | 'md' | 'lg'

const props = withDefaults(
  defineProps<{
    variant?: Variant
    size?: Size
    type?: 'button' | 'submit' | 'reset'
    disabled?: boolean
    loading?: boolean
    fullWidth?: boolean
    as?: string
  }>(),
  {
    variant: 'primary',
    size: 'md',
    type: 'button',
    disabled: false,
    loading: false,
    fullWidth: false,
    as: 'button',
  },
)

const VARIANT_CLASS: Record<Variant, string> = {
  primary:
    'bg-brand-500 text-white hover:bg-brand-600 active:bg-brand-700 focus-visible:ring-brand-500',
  secondary:
    'bg-surface text-ink-strong border border-border hover:border-border-strong hover:bg-bg focus-visible:ring-brand-500',
  ghost:
    'bg-transparent text-ink hover:bg-bg focus-visible:ring-brand-500',
  danger:
    'bg-danger text-white hover:opacity-90 active:opacity-80 focus-visible:ring-danger',
  subtle:
    'bg-bg text-ink hover:bg-border focus-visible:ring-brand-500',
}

const SIZE_CLASS: Record<Size, string> = {
  sm: 'h-8 px-3 text-xs gap-1.5',
  md: 'h-10 px-4 text-sm gap-2',
  lg: 'h-12 px-5 text-base gap-2.5',
}

const classes = computed(() =>
  cn(
    'inline-flex items-center justify-center rounded font-medium transition-colors duration-DEFAULT ease-out-expo',
    'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-bg',
    'disabled:pointer-events-none disabled:opacity-50',
    VARIANT_CLASS[props.variant],
    SIZE_CLASS[props.size],
    props.fullWidth && 'w-full',
    props.loading && 'opacity-70 pointer-events-none',
  ),
)
</script>

<template>
  <component
    :is="props.as"
    :type="props.as === 'button' ? props.type : undefined"
    :disabled="props.disabled || props.loading"
    :class="classes"
    :aria-busy="props.loading || undefined"
  >
    <span v-if="props.loading" class="inline-block size-3.5 animate-spin rounded-full border-2 border-current border-t-transparent" />
    <slot />
  </component>
</template>
