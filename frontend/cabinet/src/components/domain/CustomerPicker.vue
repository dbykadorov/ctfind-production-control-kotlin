<script setup lang="ts">
import type { Customer } from '@/api/types/frappe.generated'
import { Check, ChevronsUpDown, Loader2, Search, X } from 'lucide-vue-next'
/**
 * Comboboxen для выбора клиента (Customer).
 * Без inline-create на US1 — это переезжает в US5 (FR-024).
 * Использует Popover + поисковый input + список с подсветкой.
 */
import { onMounted, ref, watch } from 'vue'
import { getCustomer, useCustomersSearch } from '@/api/composables/use-customers'
import { Button, Input, Popover } from '@/components/ui'
import { cn } from '@/lib/utils'

const props = defineProps<{
  modelValue: string | null
  placeholder?: string
  invalid?: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | null): void
}>()

const open = ref(false)
const query = ref('')
const selectedLabel = ref<string | null>(null)

const { data, loading, search } = useCustomersSearch({ onlyActive: true })

let searchDebounce: ReturnType<typeof setTimeout> | null = null
watch(query, (q) => {
  if (searchDebounce)
    clearTimeout(searchDebounce)
  searchDebounce = setTimeout(() => {
    void search(q)
  }, 250)
})

async function loadSelectedLabel(name: string | null): Promise<void> {
  if (!name) {
    selectedLabel.value = null
    return
  }
  try {
    const c = await getCustomer(name)
    selectedLabel.value = c.customer_name || c.name
  }
  catch {
    selectedLabel.value = name
  }
}

watch(() => props.modelValue, name => loadSelectedLabel(name), { immediate: true })

onMounted(() => {
  void search('')
})

function onOpenChange(value: boolean): void {
  open.value = value
  if (value) {
    query.value = ''
    void search('')
  }
}

function pick(customer: Customer): void {
  emit('update:modelValue', customer.name)
  selectedLabel.value = customer.customer_name || customer.name
  open.value = false
}

function clear(): void {
  emit('update:modelValue', null)
  selectedLabel.value = null
}
</script>

<template>
  <Popover :open="open" @update:open="onOpenChange">
    <template #trigger>
      <Button
        as="button"
        type="button"
        variant="secondary"
        :disabled="disabled"
        full-width
        :class="cn(
          'justify-between font-normal',
          invalid ? 'border-danger' : '',
          modelValue ? 'text-ink-strong' : 'text-ink-muted',
        )"
      >
        <span class="truncate">
          {{ selectedLabel || placeholder || 'Выберите клиента' }}
        </span>
        <span class="ml-2 inline-flex items-center gap-1">
          <button
            v-if="modelValue && !disabled"
            type="button"
            class="rounded p-0.5 text-ink-muted hover:bg-bg hover:text-ink-strong"
            aria-label="Очистить выбор клиента"
            @click.stop="clear"
          >
            <X class="size-3.5" aria-hidden="true" />
          </button>
          <ChevronsUpDown class="size-4 text-ink-muted" aria-hidden="true" />
        </span>
      </Button>
    </template>

    <div class="w-80 space-y-2">
      <div class="relative">
        <Search class="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-ink-muted" aria-hidden="true" />
        <Input
          v-model="query"
          placeholder="Поиск по имени или коду…"
          class="pl-8"
          autocomplete="off"
          aria-label="Поиск клиента"
        />
      </div>
      <div class="max-h-64 min-h-[6rem] overflow-y-auto rounded border border-border">
        <div v-if="loading" class="flex items-center gap-2 px-3 py-3 text-sm text-ink-muted">
          <Loader2 class="size-4 animate-spin" aria-hidden="true" />
          Загрузка…
        </div>
        <ul v-else-if="data.length > 0" role="listbox" class="divide-y divide-border">
          <li
            v-for="c in data"
            :key="c.name"
            role="option"
            :aria-selected="c.name === modelValue"
          >
            <button
              type="button"
              class="flex w-full items-center justify-between gap-3 px-3 py-2 text-left text-sm hover:bg-brand-50"
              @click="pick(c)"
            >
              <span class="min-w-0">
                <span class="block truncate font-medium text-ink-strong">
                  {{ c.customer_name }}
                </span>
                <span class="block truncate text-xs text-ink-muted">
                  {{ c.name }}<span v-if="c.contact_person"> · {{ c.contact_person }}</span>
                </span>
              </span>
              <Check
                v-if="c.name === modelValue"
                class="size-4 text-brand-600"
                aria-hidden="true"
              />
            </button>
          </li>
        </ul>
        <div v-else class="px-3 py-6 text-center text-sm text-ink-muted">
          Клиенты не найдены
        </div>
      </div>
    </div>
  </Popover>
</template>
