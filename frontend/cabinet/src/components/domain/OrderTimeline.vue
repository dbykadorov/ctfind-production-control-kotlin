<script setup lang="ts">
import type { ParsedDiff, TimelineEntry } from '@/api/types/domain'
import { format, isToday, isYesterday, parseISO } from 'date-fns'
import { ru } from 'date-fns/locale'
import { Activity, ArrowRight, Edit3, Shield } from 'lucide-vue-next'
/**
 * Лента истории заказа: объединённый Version + Customer Order Status Change
 * (см. data-model.md §2.3, composables/use-history.ts). Группировка по дате,
 * иконка по типу события, человекочитаемые формулировки изменений.
 */
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    entries: TimelineEntry[]
    loading?: boolean
  }>(),
  { loading: false },
)

interface Group {
  dateKey: string
  label: string
  items: TimelineEntry[]
}

const FIELD_LABELS: Record<string, string> = {
  status: 'Статус',
  customer: 'Клиент',
  delivery_date: 'Срок исполнения',
  notes: 'Комментарий',
  items: 'Позиции',
  created_by_staff: 'Создатель',
  note: 'Комментарий перехода',
}

function fieldLabel(name: string): string {
  return FIELD_LABELS[name] ?? name
}

function fmtValue(v: unknown): string {
  if (v === null || v === undefined || v === '')
    return '—'
  if (typeof v === 'object')
    return JSON.stringify(v)
  return String(v)
}

function dayLabel(iso: string): string {
  try {
    const d = parseISO(iso)
    if (isToday(d))
      return 'Сегодня'
    if (isYesterday(d))
      return 'Вчера'
    return format(d, 'd MMMM yyyy', { locale: ru })
  }
  catch {
    return iso
  }
}

function timeLabel(iso: string): string {
  try {
    return format(parseISO(iso), 'HH:mm', { locale: ru })
  }
  catch {
    return ''
  }
}

const groups = computed<Group[]>(() => {
  const map = new Map<string, Group>()
  for (const entry of props.entries) {
    const dateKey = entry.at.slice(0, 10)
    const existing = map.get(dateKey)
    if (existing) {
      existing.items.push(entry)
    }
    else {
      map.set(dateKey, { dateKey, label: dayLabel(entry.at), items: [entry] })
    }
  }
  return Array.from(map.values()).sort((a, b) => (a.dateKey < b.dateKey ? 1 : -1))
})

function diffSummary(diff: ParsedDiff): string {
  return `${fmtValue(diff.from_value)} → ${fmtValue(diff.to_value)}`
}
</script>

<template>
  <section aria-label="История заказа" class="space-y-6">
    <div v-if="loading" class="text-sm text-ink-muted">
      Загрузка истории…
    </div>
    <div v-else-if="entries.length === 0" class="text-sm text-ink-muted">
      История отсутствует
    </div>
    <div v-for="group in groups" v-else :key="group.dateKey" class="space-y-3">
      <h4 class="text-xs font-semibold uppercase tracking-wide text-ink-muted">
        {{ group.label }}
      </h4>
      <ol class="space-y-3">
        <li
          v-for="entry in group.items"
          :key="entry.id"
          class="flex items-start gap-3 rounded border border-border bg-surface p-3"
        >
          <div
            class="flex size-8 shrink-0 items-center justify-center rounded-full" :class="[
              entry.kind === 'status' ? 'bg-brand-50 text-brand-600' : 'bg-bg text-ink',
            ]"
            aria-hidden="true"
          >
            <component :is="entry.kind === 'status' ? Activity : Edit3" class="size-4" />
          </div>
          <div class="min-w-0 flex-1 space-y-1">
            <div class="flex flex-wrap items-baseline justify-between gap-2 text-sm">
              <span class="font-medium text-ink-strong">
                {{ entry.kind === 'status' ? 'Изменён статус' : 'Изменены поля' }}
              </span>
              <span class="text-xs text-ink-muted">
                {{ timeLabel(entry.at) }} · {{ entry.actor_label || entry.actor }}
              </span>
            </div>
            <ul class="space-y-1 text-sm text-ink">
              <li
                v-for="(diff, idx) in entry.details"
                :key="`${entry.id}:${idx}`"
                class="flex flex-wrap items-baseline gap-x-2"
              >
                <span class="text-ink-muted">{{ fieldLabel(diff.fieldname) }}:</span>
                <span class="text-ink-strong">{{ fmtValue(diff.from_value) }}</span>
                <ArrowRight class="size-3 text-ink-muted" aria-hidden="true" />
                <span class="font-medium text-ink-strong">{{ fmtValue(diff.to_value) }}</span>
                <span class="sr-only">{{ diffSummary(diff) }}</span>
              </li>
            </ul>
            <div
              v-if="entry.via_admin_correction"
              class="inline-flex items-center gap-1 text-xs font-medium text-warning"
            >
              <Shield class="size-3" aria-hidden="true" />
              Административная корректировка
            </div>
          </div>
        </li>
      </ol>
    </div>
  </section>
</template>
