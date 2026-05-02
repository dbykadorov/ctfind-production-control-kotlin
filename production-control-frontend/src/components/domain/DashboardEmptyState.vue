<script setup lang="ts">
import { Plus, Sparkles } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { Card } from '@/components/ui'

interface Props {
  canCreateOrder?: boolean
}
withDefaults(defineProps<Props>(), { canCreateOrder: false })

const { t } = useI18n()
</script>

<template>
  <Card class="flex flex-col items-center gap-4 p-10 text-center">
    <span class="flex size-12 items-center justify-center rounded-full bg-brand-100 text-brand-500">
      <Sparkles class="size-6" aria-hidden="true" />
    </span>
    <p class="max-w-md text-sm text-ink">
      {{ t('dashboard.emptyAll') }}
    </p>
    <RouterLink
      v-if="canCreateOrder"
      to="/cabinet/orders/new"
      class="inline-flex items-center gap-2 rounded bg-brand-500 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-brand-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
    >
      <Plus class="size-4" aria-hidden="true" />
      {{ t('dashboard.createFirst') }}
    </RouterLink>
  </Card>
</template>
