<script setup lang="ts">
import type { ProductionTaskAssigneeRow } from '@/api/types/production-tasks'
import { fetchProductionTaskAssignees } from '@/api/composables/use-production-task-detail'
import { Input, Label } from '@/components/ui'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const { disabled = false } = defineProps<{
  modelValue: string | null
  disabled?: boolean
}>()

const emit = defineEmits<{ (e: 'update:modelValue', v: string | null): void }>()

const search = ref('')
const loading = ref(false)
const items = ref<ProductionTaskAssigneeRow[]>([])

async function load(): Promise<void> {
  loading.value = true
  try {
    const r = await fetchProductionTaskAssignees(search.value.trim() || undefined, 30)
    items.value = r.items
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

function pick(id: string): void {
  emit('update:modelValue', id)
}
</script>

<template>
  <div class="space-y-2">
    <Label :for="'assignee-search'">Исполнитель</Label>
    <Input
      :id="'assignee-search'"
      v-model="search"
      type="search"
      placeholder="Поиск по имени или логину"
      :disabled="disabled"
      autocomplete="off"
    />
    <ul
      class="max-h-40 overflow-auto rounded border border-border bg-surface text-sm"
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
          class="cursor-pointer px-3 py-2 text-ink hover:bg-bg/70"
          :class="{ 'bg-bg text-ink-strong': modelValue === u.id }"
          @click="pick(u.id)"
        >
          {{ u.displayName }} <span class="text-ink-muted">({{ u.login }})</span>
        </li>
      </template>
    </ul>
  </div>
</template>
