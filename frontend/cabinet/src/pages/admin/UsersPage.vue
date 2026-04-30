<script setup lang="ts">
import type { ApiError } from '@/api/types/domain'
import type { CreateUserFormState, RoleSummaryResponse } from '@/api/types/user-management'
import { Plus, RotateCw, Search } from 'lucide-vue-next'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { createUser, fetchRoleCatalog, useUsersList } from '@/api/composables/use-users'

const { t } = useI18n()
const { data: users, loading, error, refetch } = useUsersList()

const search = ref('')
const showCreate = ref(false)
const creating = ref(false)
const roleLoading = ref(false)
const roles = ref<RoleSummaryResponse[]>([])
const formError = ref<string | null>(null)
const formSuccess = ref<string | null>(null)

const form = ref<CreateUserFormState>({
  login: '',
  displayName: '',
  initialPassword: '',
  roleCodes: [],
})

const isForbidden = computed(() => error.value?.status === 403)

let debounceTimer: ReturnType<typeof setTimeout> | null = null

watch(search, () => {
  if (debounceTimer)
    clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    void refetch(search.value, 50)
  }, 300)
})

onMounted(() => {
  void refetch(undefined, 50)
})

onUnmounted(() => {
  if (debounceTimer)
    clearTimeout(debounceTimer)
})

function resetForm() {
  form.value = {
    login: '',
    displayName: '',
    initialPassword: '',
    roleCodes: [],
  }
}

function toggleRole(code: string) {
  if (form.value.roleCodes.includes(code)) {
    form.value.roleCodes = form.value.roleCodes.filter(v => v !== code)
  }
  else {
    form.value.roleCodes = [...form.value.roleCodes, code]
  }
}

async function openCreate() {
  showCreate.value = true
  formError.value = null
  formSuccess.value = null
  if (roles.value.length > 0)
    return
  roleLoading.value = true
  try {
    roles.value = await fetchRoleCatalog()
  }
  catch (e) {
    formError.value = mapCreateError(e as ApiError)
  }
  finally {
    roleLoading.value = false
  }
}

function closeCreate() {
  showCreate.value = false
  formError.value = null
}

function mapCreateError(error: ApiError): string {
  if (error.status === 409 || error.kind === 'conflict')
    return t('users.messages.duplicate')
  if (error.status === 403 || error.kind === 'permission')
    return t('users.messages.forbidden')
  if (error.status === 400 && error.message.toLowerCase().includes('role'))
    return t('users.messages.invalidRoles')
  if (error.status === 400 || error.kind === 'validation')
    return t('users.messages.validation')
  return error.message || t('users.messages.generic')
}

async function submitCreate() {
  formError.value = null
  formSuccess.value = null

  if (!form.value.login.trim() || !form.value.displayName.trim() || !form.value.initialPassword || form.value.roleCodes.length === 0) {
    formError.value = t('users.messages.validation')
    return
  }

  creating.value = true
  try {
    await createUser({
      login: form.value.login.trim(),
      displayName: form.value.displayName.trim(),
      initialPassword: form.value.initialPassword,
      roleCodes: form.value.roleCodes,
    })
    resetForm()
    showCreate.value = false
    formSuccess.value = t('users.messages.created')
    await refetch(search.value, 50)
  }
  catch (e) {
    formError.value = mapCreateError(e as ApiError)
  }
  finally {
    creating.value = false
  }
}
</script>

<template>
  <div class="space-y-6 p-6">
    <header class="flex flex-wrap items-start justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold text-ink-strong">
          {{ t('users.title') }}
        </h1>
        <p class="text-sm text-ink-muted">
          {{ t('users.subtitle') }}
        </p>
      </div>
      <div class="flex items-center gap-2">
        <button
          type="button"
          class="inline-flex items-center gap-1.5 rounded-md border border-border bg-surface px-3 py-1.5 text-sm font-medium text-ink-strong hover:bg-bg"
          :disabled="loading"
          @click="refetch(search, 50)"
        >
          <RotateCw class="size-4" aria-hidden="true" />
          {{ t('users.actions.refresh') }}
        </button>
        <button
          type="button"
          class="inline-flex items-center gap-1.5 rounded-md bg-brand-500 px-3 py-1.5 text-sm font-medium text-white hover:bg-brand-600"
          @click="openCreate"
        >
          <Plus class="size-4" aria-hidden="true" />
          {{ t('users.create') }}
        </button>
      </div>
    </header>

    <p
      v-if="formSuccess"
      class="rounded border border-emerald-300 bg-emerald-50 px-3 py-2 text-sm text-emerald-700"
      data-testid="users-success"
    >
      {{ formSuccess }}
    </p>

    <div class="relative">
      <Search class="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-ink-muted" aria-hidden="true" />
      <input
        v-model="search"
        type="search"
        :placeholder="t('users.search')"
        class="w-full rounded border border-border bg-bg py-1.5 pl-8 pr-3 text-sm"
      >
    </div>

    <div
      v-if="isForbidden"
      class="rounded border border-dashed border-border bg-surface p-10 text-center text-sm text-ink-muted"
      data-testid="users-forbidden"
    >
      {{ t('users.forbidden') }}
    </div>

    <div v-else class="overflow-hidden rounded border border-border bg-surface">
      <table class="w-full text-sm">
        <thead class="bg-bg/60 text-left text-xs uppercase tracking-wide text-ink-muted">
          <tr>
            <th class="px-3 py-2">{{ t('users.fields.login') }}</th>
            <th class="px-3 py-2">{{ t('users.fields.displayName') }}</th>
            <th class="px-3 py-2">{{ t('users.fields.roles') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="3" class="px-3 py-3 text-ink-muted">
              {{ t('common.loading') }}
            </td>
          </tr>
          <tr v-else-if="users.length === 0">
            <td colspan="3" class="px-3 py-3 text-ink-muted">
              {{ t('users.empty') }}
            </td>
          </tr>
          <tr v-for="user in users" :key="user.id" class="border-t border-border/70">
            <td class="px-3 py-2 font-medium text-ink-strong">
              {{ user.login }}
            </td>
            <td class="px-3 py-2 text-ink-strong">
              {{ user.displayName }}
            </td>
            <td class="px-3 py-2">
              <div class="flex flex-wrap gap-1">
                <span
                  v-for="role in user.roles"
                  :key="`${user.id}-${role.code}`"
                  class="rounded border border-border bg-bg px-2 py-0.5 text-xs text-ink-muted"
                >
                  {{ role.name }}
                </span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div
      v-if="showCreate"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/45 px-4"
      @click.self="closeCreate"
    >
      <div class="w-full max-w-lg rounded-lg border border-border bg-surface p-5 shadow-xl">
        <h2 class="text-lg font-semibold text-ink-strong">
          {{ t('users.create') }}
        </h2>
        <div class="mt-4 space-y-3">
          <label class="block space-y-1">
            <span class="text-xs font-medium text-ink-muted">{{ t('users.fields.login') }}</span>
            <input
              v-model="form.login"
              type="text"
              class="w-full rounded border border-border bg-bg px-3 py-2 text-sm"
            >
          </label>
          <label class="block space-y-1">
            <span class="text-xs font-medium text-ink-muted">{{ t('users.fields.displayName') }}</span>
            <input
              v-model="form.displayName"
              type="text"
              class="w-full rounded border border-border bg-bg px-3 py-2 text-sm"
            >
          </label>
          <label class="block space-y-1">
            <span class="text-xs font-medium text-ink-muted">{{ t('users.fields.initialPassword') }}</span>
            <input
              v-model="form.initialPassword"
              type="password"
              autocomplete="new-password"
              class="w-full rounded border border-border bg-bg px-3 py-2 text-sm"
            >
          </label>
          <fieldset class="space-y-2">
            <legend class="text-xs font-medium text-ink-muted">
              {{ t('users.fields.roles') }}
            </legend>
            <div v-if="roleLoading" class="text-sm text-ink-muted">
              {{ t('common.loading') }}
            </div>
            <div v-else class="grid grid-cols-1 gap-1.5 sm:grid-cols-2">
              <label
                v-for="role in roles"
                :key="role.code"
                class="inline-flex items-center gap-2 rounded border border-border px-2 py-1.5 text-sm"
              >
                <input
                  type="checkbox"
                  :checked="form.roleCodes.includes(role.code)"
                  @change="toggleRole(role.code)"
                >
                <span>{{ role.name }}</span>
              </label>
            </div>
          </fieldset>
        </div>

        <p
          v-if="formError"
          class="mt-3 rounded border border-rose-300 bg-rose-50 px-3 py-2 text-sm text-rose-700"
          data-testid="users-form-error"
        >
          {{ formError }}
        </p>

        <div class="mt-4 flex justify-end gap-2">
          <button
            type="button"
            class="rounded border border-border px-3 py-1.5 text-sm text-ink-muted hover:bg-bg"
            :disabled="creating"
            @click="closeCreate"
          >
            {{ t('users.actions.cancel') }}
          </button>
          <button
            type="button"
            class="rounded bg-brand-500 px-3 py-1.5 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-60"
            :disabled="creating"
            @click="submitCreate"
          >
            {{ t('users.actions.submit') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
