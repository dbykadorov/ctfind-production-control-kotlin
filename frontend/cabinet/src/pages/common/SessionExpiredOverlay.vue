<script setup lang="ts">
import { storeToRefs } from 'pinia'
/**
 * Inline-overlay поверх любого экрана при 401. НЕ редиректит — позволяет пользователю
 * перелогиниться без потери введённых данных формы (FR-011).
 */
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { toast } from 'vue-sonner'
import { frappeCall } from '@/api/frappe-client'
import { Button, Dialog, Input, Label } from '@/components/ui'
import { useAuthStore } from '@/stores/auth'
import { toApiError } from '@/utils/errors'

const { t } = useI18n()
const auth = useAuthStore()
const { sessionExpired } = storeToRefs(auth)

const username = ref('')
const password = ref('')
const submitting = ref(false)

async function relogin(): Promise<void> {
  if (submitting.value)
    return
  submitting.value = true
  try {
    await frappeCall('login', { usr: username.value, pwd: password.value }, { withoutCsrf: true })
    await auth.refreshBoot()
    auth.clearSessionExpired()
    password.value = ''
  }
  catch (err) {
    const apiErr = toApiError(err)
    toast.error(apiErr.message || t('auth.login.failed'))
  }
  finally {
    submitting.value = false
  }
}
</script>

<template>
  <Dialog
    :open="sessionExpired"
    :title="t('auth.sessionExpired.title')"
    :description="t('auth.sessionExpired.description')"
    hide-close
    @update:open="(v) => !v && auth.clearSessionExpired()"
  >
    <form class="flex flex-col gap-4" @submit.prevent="relogin">
      <div class="flex flex-col gap-1.5">
        <Label for="se-username" required>{{ t('auth.login.username') }}</Label>
        <Input id="se-username" v-model="username" type="email" autocomplete="username" />
      </div>
      <div class="flex flex-col gap-1.5">
        <Label for="se-password" required>{{ t('auth.login.password') }}</Label>
        <Input id="se-password" v-model="password" type="password" autocomplete="current-password" />
      </div>
      <div class="flex justify-end gap-2 pt-2">
        <Button type="submit" variant="primary" :loading="submitting">
          {{ t('auth.sessionExpired.relogin') }}
        </Button>
      </div>
    </form>
  </Dialog>
</template>
