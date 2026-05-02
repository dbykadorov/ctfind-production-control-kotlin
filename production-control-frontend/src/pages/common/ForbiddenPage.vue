<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { Button, Card } from '@/components/ui'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const auth = useAuthStore()
</script>

<template>
  <section class="mx-auto flex min-h-[60vh] max-w-xl flex-col items-center justify-center gap-4 px-6 text-center">
    <Card class="w-full p-8">
      <h1 class="text-2xl font-semibold text-ink-strong">
        {{ t('auth.forbidden.title') }}
      </h1>
      <p class="mt-3 text-sm text-ink-muted">
        {{ t('auth.forbidden.description') }}
      </p>
      <p class="mt-2 text-xs text-ink-muted">
        {{ t('nav.dashboard') }} · {{ auth.user || '—' }}
      </p>
      <div class="mt-6 flex justify-center gap-2">
        <RouterLink to="/cabinet" class="contents">
          <Button variant="primary">
            {{ t('nav.dashboard') }}
          </Button>
        </RouterLink>
      </div>
    </Card>
  </section>
</template>
