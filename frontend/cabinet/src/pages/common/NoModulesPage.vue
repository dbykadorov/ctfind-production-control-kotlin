<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Button, Card } from '@/components/ui'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const auth = useAuthStore()

/**
 * После logout всегда возвращаем пользователя на /cabinet/login.
 */
async function logoutAndReturnToLogin(): Promise<void> {
  try {
    await auth.logout()
  }
  finally {
    window.location.assign('/cabinet/login')
  }
}
</script>

<template>
  <section class="mx-auto flex min-h-[60vh] max-w-xl flex-col items-center justify-center gap-4 px-6 text-center">
    <Card class="w-full p-8">
      <h1 class="text-2xl font-semibold text-ink-strong">
        {{ t('auth.noModules.title') }}
      </h1>
      <p class="mt-3 text-sm text-ink-muted">
        {{ t('auth.noModules.description') }}
      </p>
      <div class="mt-6 flex flex-wrap justify-center gap-2">
        <Button variant="ghost" @click="logoutAndReturnToLogin">
          {{ t('nav.logout') }}
        </Button>
        <!-- Если у пользователя есть админская роль — даём короткий путь в Desk
             (например, чтобы добавить себе cabinet-роль). Для обычного юзера
             эта ссылка скрыта, чтобы не путать. -->
        <Button
          v-if="auth.permissions.isAdmin"
          as="a"
          variant="secondary"
          :href="auth.deskUrl || '/app'"
        >
          {{ t('nav.openInDesk') }}
        </Button>
      </div>
    </Card>
  </section>
</template>
