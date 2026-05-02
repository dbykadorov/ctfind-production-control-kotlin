<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const props = withDefaults(defineProps<{ size?: 'sm' | 'md' | 'lg' }>(), { size: 'md' })

const SIZE_CLASS = { sm: 'size-4 border-2', md: 'size-6 border-2', lg: 'size-8 border-[3px]' } as const

const classes = computed(() =>
  cn('inline-block animate-spin rounded-full border-current border-t-transparent', SIZE_CLASS[props.size]),
)
</script>

<template>
  <span :class="classes" role="status" aria-label="Загрузка" />
</template>
