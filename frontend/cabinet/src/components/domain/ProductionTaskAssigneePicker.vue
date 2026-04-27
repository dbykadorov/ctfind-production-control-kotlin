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
      class="max-h-40 overflow-auto rounded border border-slate-200 text-sm"
      role="listbox"
    >
      <li v-if="loading" class="px-3 py-2 text-slate-500">
        Загрузка…
      </li>
      <li v-else-if="items.length === 0" class="px-3 py-2 text-slate-500">
        Ничего не найдено
      </li>
      <template v-else>
        <li
          v-for="u in items"
          :key="u.id"
          role="option"
          class="cursor-pointer px-3 py-2 hover:bg-slate-50"
          :class="{ 'bg-slate-100': modelValue === u.id }"
          @click="pick(u.id)"
        >
          {{ u.displayName }} <span class="text-slate-500">({{ u.login }})</span>
        </li>
      </template>
    </ul>
  </div>
</template>
