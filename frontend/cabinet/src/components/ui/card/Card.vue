<script setup lang="ts">
import { cn } from '@/lib/utils'

defineProps<{ as?: string }>()
</script>

<template>
  <component
    :is="as ?? 'div'"
    :class="cn('ctfind-card transition-shadow duration-DEFAULT ease-out-expo')"
  >
    <slot />
  </component>
</template>
