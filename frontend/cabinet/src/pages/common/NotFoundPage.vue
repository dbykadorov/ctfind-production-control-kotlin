<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { Button } from '@/components/ui'

const { t } = useI18n()
</script>

<template>
  <section class="flex min-h-[60vh] flex-col items-center justify-center gap-4 px-6 text-center">
    <h1 class="text-3xl font-semibold text-ink-strong">
      404
    </h1>
    <p class="max-w-md text-sm text-ink-muted">
      {{ t('common.notFound') }}
    </p>
    <RouterLink to="/cabinet" class="contents">
      <Button variant="primary">
        {{ t('nav.dashboard') }}
      </Button>
    </RouterLink>
  </section>
</template>
