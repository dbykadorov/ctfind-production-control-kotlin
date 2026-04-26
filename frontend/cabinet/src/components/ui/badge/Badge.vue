<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

type Tone = 'neutral' | 'brand' | 'success' | 'warning' | 'danger'

const props = withDefaults(
  defineProps<{ tone?: Tone, dot?: boolean }>(),
  { tone: 'neutral', dot: false },
)

const TONE_CLASS: Record<Tone, string> = {
  neutral: 'bg-bg text-ink border border-border',
  brand: 'bg-brand-500/15 text-brand-500 border border-brand-500/30',
  success: 'bg-success/15 text-success border border-success/30',
  warning: 'bg-warning/15 text-warning border border-warning/30',
  danger: 'bg-danger/15 text-danger border border-danger/30',
}

const classes = computed(() =>
  cn(
    'inline-flex items-center gap-1.5 rounded-sm px-2 py-0.5 text-xs font-medium',
    TONE_CLASS[props.tone],
  ),
)
</script>

<template>
  <span :class="classes">
    <span
      v-if="dot"
      aria-hidden="true"
      class="size-1.5 rounded-full bg-current opacity-80"
    />
    <slot />
  </span>
</template>
