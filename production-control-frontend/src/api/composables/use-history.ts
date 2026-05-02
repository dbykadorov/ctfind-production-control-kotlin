/**
 * Объединённая лента истории заказа: версии изменений полей +
 * Customer Order Status Change (переходы статусов).
 *
 * См. data-model.md §2.3, contracts/http-endpoints.md §History.
 */

import type {
  ApiError,
  TimelineEntry,
} from '@/api/types/domain'
import type { OrderChangeDiffResponse, OrderDetailResponse, OrderFieldDiffResponse, OrderTimelineResponse } from '@/api/types/orders'
import { computed, type ComputedRef, onScopeDispose, ref, type Ref, shallowRef, watch } from 'vue'
import { httpClient } from '@/api/api-client'
import { toApiError } from '@/utils/errors'

interface UseHistoryResult {
  entries: ComputedRef<TimelineEntry[]>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  reload: () => Promise<void>
}

export function useOrderHistory(name: Ref<string>): UseHistoryResult {
  const changeDiffs = shallowRef<OrderChangeDiffResponse[]>([])
  const statusChanges = shallowRef<OrderTimelineResponse[]>([])
  const loading = ref(false)
  const error = ref<ApiError | null>(null)
  let abortController: AbortController | null = null

  async function reload(): Promise<void> {
    if (!name.value)
      return
    abortController?.abort()
    abortController = new AbortController()
    loading.value = true
    error.value = null
    try {
      const response = await httpClient.get<Pick<OrderDetailResponse, 'history' | 'changeDiffs'>>(
        `/api/orders/${encodeURIComponent(name.value)}`,
        { signal: abortController.signal },
      )
      statusChanges.value = response.data.history ?? []
      changeDiffs.value = response.data.changeDiffs ?? []
    }
    catch (e) {
      if ((e as { name?: string }).name === 'CanceledError')
        return
      error.value = toApiError(e)
      statusChanges.value = []
      changeDiffs.value = []
    }
    finally {
      loading.value = false
    }
  }

  const entries = computed<TimelineEntry[]>(() => {
    const merged: TimelineEntry[] = []
    for (const sc of statusChanges.value) {
      merged.push({
        id: `status:${sc.changedAt}:${sc.toStatus ?? 'unknown'}`,
        kind: 'status',
        at: sc.changedAt,
        actor: sc.actorDisplayName,
        actor_label: sc.actorDisplayName,
        details: [
          { fieldname: 'status', from_value: sc.fromStatus, to_value: sc.toStatus },
          ...(sc.note ? [{ fieldname: 'note', to_value: sc.note }] : []),
        ],
      })
    }
    for (const [index, diff] of changeDiffs.value.entries()) {
      const details = diff.fieldDiffs.map(mapFieldDiff)
      if (details.length === 0)
        continue
      merged.push({
        id: `diff:${diff.changedAt}:${index}`,
        kind: 'edit',
        at: diff.changedAt,
        actor: diff.actorDisplayName,
        actor_label: diff.actorDisplayName,
        details,
      })
    }
    return merged.sort((a, b) => (a.at < b.at ? 1 : a.at > b.at ? -1 : 0))
  })

  watch(name, () => { void reload() }, { immediate: true })

  onScopeDispose(() => abortController?.abort())

  return { entries, loading, error, reload }
}

function mapFieldDiff(diff: OrderFieldDiffResponse) {
  return {
    fieldname: diff.fieldname,
    field_label: diff.fieldLabel,
    from_value: diff.fromValue,
    to_value: diff.toValue,
  }
}
