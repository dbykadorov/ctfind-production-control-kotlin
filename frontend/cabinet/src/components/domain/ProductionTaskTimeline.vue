<script setup lang="ts">
/**
 * Reusable timeline для производственных задач (Feature 005 US5).
 */
import type { ProductionTaskHistoryEventResponse } from '@/api/types/production-tasks'
import { computed } from 'vue'
import { mapProductionTaskHistory } from '@/api/composables/use-production-task-history'

const props = defineProps<{
  history: readonly ProductionTaskHistoryEventResponse[] | null | undefined
  emptyMessage?: string
}>()

const entries = computed(() => mapProductionTaskHistory(props.history ?? []))
</script>

<template>
  <div data-testid="production-task-timeline">
    <p v-if="entries.length === 0" class="text-sm text-slate-500">
      {{ emptyMessage ?? 'История пуста' }}
    </p>
    <ol v-else class="space-y-3 border-l border-slate-200 pl-4">
      <li
        v-for="(entry, idx) in entries"
        :key="idx"
        class="relative text-sm"
        data-testid="production-task-timeline-entry"
      >
        <span class="absolute -left-[21px] top-1.5 size-2 rounded-full bg-slate-300" aria-hidden="true" />
        <p class="text-slate-900">
          <span class="font-medium">{{ entry.title }}</span>
          <span class="text-slate-500"> · {{ entry.actorDisplayName }}</span>
        </p>
        <p class="text-xs text-slate-500" :title="entry.eventAt">
          {{ entry.eventAtLabel }}
        </p>
        <ul v-if="entry.details.length > 0" class="mt-1 space-y-0.5 text-xs text-slate-600">
          <li v-for="(line, lineIdx) in entry.details" :key="lineIdx">
            {{ line }}
          </li>
        </ul>
      </li>
    </ol>
  </div>
</template>
