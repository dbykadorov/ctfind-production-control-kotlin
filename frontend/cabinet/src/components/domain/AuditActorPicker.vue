<script setup lang="ts">
import type { UserSummaryResponse } from '@/api/types/audit-log'
import { fetchUsers } from '@/api/composables/use-users-search'
import { Input } from '@/components/ui'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const { disabled = false } = defineProps<{
  modelValue: string | null
  disabled?: boolean
}>()

const emit = defineEmits<{ (e: 'update:modelValue', v: string | null): void }>()

const search = ref('')
const loading = ref(false)
const items = ref<UserSummaryResponse[]>([])
const selectedName = ref<string | null>(null)

async function load(): Promise<void> {
  loading.value = true
  try {
    items.value = await fetchUsers(search.value.trim() || undefined, 30)
  }
  catch {
    items.value = []
  }
  finally {
    loading.value = false
  }
}

let timer: ReturnType<typeof setTimeout> | null = null
watch(search, () => {
  if (timer)
    clearTimeout(timer)
  timer = setTimeout(() => {
    void load()
  }, 300)
})

onMounted(() => {
  void load()
})

onBeforeUnmount(() => {
  if (timer)
    clearTimeout(timer)
})

function pick(user: UserSummaryResponse): void {
  emit('update:modelValue', user.id)
  selectedName.value = `${user.displayName} (${user.login})`
}

function clear(): void {
  emit('update:modelValue', null)
  selectedName.value = null
  search.value = ''
}
</script>

<template>
  <div class="space-y-2">
    <div class="flex items-center gap-2">
      <Input
        v-model="search"
        type="search"
        :placeholder="selectedName ?? 'Поиск по имени или логину'"
        :disabled="disabled"
        autocomplete="off"
        class="flex-1"
      />
      <button
        v-if="modelValue"
        type="button"
        class="shrink-0 rounded border border-border px-2 py-1 text-xs text-ink-muted hover:bg-bg"
        :disabled="disabled"
        @click="clear"
      >
        ✕
      </button>
    </div>
    <ul
      class="max-h-40 overflow-auto rounded border border-border text-sm"
      role="listbox"
    >
      <li v-if="loading" class="px-3 py-2 text-ink-muted">
        Загрузка…
      </li>
      <li v-else-if="items.length === 0" class="px-3 py-2 text-ink-muted">
        Ничего не найдено
      </li>
      <template v-else>
        <li
          v-for="u in items"
          :key="u.id"
          role="option"
          class="cursor-pointer px-3 py-2 hover:bg-bg/50"
          :class="{ 'bg-bg': modelValue === u.id }"
          @click="pick(u)"
        >
          {{ u.displayName }} <span class="text-ink-muted">({{ u.login }})</span>
        </li>
      </template>
    </ul>
  </div>
</template>
