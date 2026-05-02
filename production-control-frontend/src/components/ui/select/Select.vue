<script setup lang="ts" generic="T extends string | number">
import { computed } from 'vue'
import {
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectItemText,
  SelectPortal,
  SelectRoot,
  SelectTrigger,
  SelectValue,
  SelectViewport,
} from 'radix-vue'
import { Check, ChevronDown } from 'lucide-vue-next'
import { cn } from '@/lib/utils'

interface Option<V extends string | number> {
  value: V
  label: string
  disabled?: boolean
}

const props = defineProps<{
  modelValue?: T | null
  options: Option<T>[]
  placeholder?: string
  disabled?: boolean
  invalid?: boolean
  id?: string
}>()

const emit = defineEmits<{ (e: 'update:modelValue', value: T | null): void }>()

// Radix запрещает пустую строку как value у SelectItem (она зарезервирована
// для «нет выбора»). Используем sentinel внутри компонента, не протекая его
// наружу: все вызывающие страницы работают с обычными string/number/null.
const EMPTY_SENTINEL = '__cabinet_select_empty__'

const value = computed({
  get: () => {
    const raw = props.modelValue
    return raw === null || raw === undefined || raw === '' ? EMPTY_SENTINEL : String(raw)
  },
  set: (v: string) => emit('update:modelValue', v === EMPTY_SENTINEL ? null : (v as unknown as T)),
})

const renderableOptions = computed(() =>
  props.options.map(opt => ({
    ...opt,
    _renderValue: opt.value === '' || opt.value === null || opt.value === undefined
      ? EMPTY_SENTINEL
      : String(opt.value),
  })),
)
</script>

<template>
  <SelectRoot v-model="value" :disabled="disabled">
    <SelectTrigger
      :id="id"
      :aria-invalid="invalid || undefined"
      :class="cn(
        'flex h-10 w-full items-center justify-between rounded border bg-surface px-3 text-sm text-ink-strong',
        'focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/20',
        'disabled:cursor-not-allowed disabled:opacity-60',
        invalid ? 'border-danger' : 'border-border',
      )"
    >
      <SelectValue :placeholder="placeholder ?? 'Выберите…'" />
      <ChevronDown class="size-4 text-ink-muted" />
    </SelectTrigger>
    <SelectPortal>
      <SelectContent
        :class="cn(
          'z-50 overflow-hidden rounded border border-border bg-elevated shadow-elevated',
          'data-[state=open]:animate-in data-[state=open]:fade-in-0',
        )"
        position="popper"
        :side-offset="4"
      >
        <SelectViewport class="p-1">
          <SelectGroup>
            <SelectItem
              v-for="opt in renderableOptions"
              :key="opt._renderValue"
              :value="opt._renderValue"
              :disabled="opt.disabled"
              :class="cn(
                'relative flex h-9 cursor-pointer select-none items-center gap-2 rounded-sm px-2 pr-8 text-sm text-ink-strong',
                'data-[highlighted]:bg-brand-500/15 data-[highlighted]:text-brand-500',
                'data-[disabled]:pointer-events-none data-[disabled]:opacity-50',
              )"
            >
              <SelectItemText>{{ opt.label }}</SelectItemText>
              <span class="absolute right-2 flex items-center justify-center">
                <Check class="size-4 hidden data-[state=checked]:block" />
              </span>
            </SelectItem>
          </SelectGroup>
        </SelectViewport>
      </SelectContent>
    </SelectPortal>
  </SelectRoot>
</template>
