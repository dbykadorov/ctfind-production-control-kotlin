/**
 * Объединённая лента истории заказа: Frappe Version (изменения полей) +
 * Customer Order Status Change (переходы статусов).
 *
 * См. data-model.md §2.3, contracts/http-endpoints.md §History.
 */

import type {
  ApiError,
  CustomerOrderStatusChange,
  ParsedDiff,
  TimelineEntry,
} from '@/api/types/domain'
import type { FrappeVersion } from '@/api/types/frappe.generated'
import { computed, type ComputedRef, onScopeDispose, ref, type Ref, shallowRef, watch } from 'vue'
import { frappeCall } from '@/api/frappe-client'
import { toApiError } from '@/utils/errors'

const ORDER_DOCTYPE = 'Customer Order'

interface UseHistoryResult {
  entries: ComputedRef<TimelineEntry[]>
  loading: Ref<boolean>
  error: Ref<ApiError | null>
  reload: () => Promise<void>
}

interface FrappeVersionDataPayload {
  changed?: Array<[string, unknown, unknown]>
  added?: Array<[string, Record<string, unknown>]>
  removed?: Array<[string, Record<string, unknown>]>
}

function parseVersionData(raw: string): ParsedDiff[] {
  try {
    const parsed = JSON.parse(raw) as FrappeVersionDataPayload
    const out: ParsedDiff[] = []
    for (const [field, from, to] of parsed.changed ?? []) {
      out.push({ fieldname: field, from_value: from, to_value: to })
    }
    for (const [field, payload] of parsed.added ?? []) {
      out.push({ fieldname: field, from_value: undefined, to_value: payload })
    }
    for (const [field, payload] of parsed.removed ?? []) {
      out.push({ fieldname: field, from_value: payload, to_value: undefined })
    }
    return out
  }
  catch {
    return []
  }
}

export function useOrderHistory(name: Ref<string>): UseHistoryResult {
  const versions = shallowRef<FrappeVersion[]>([])
  const statusChanges = shallowRef<CustomerOrderStatusChange[]>([])
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
      const [v, s] = await Promise.all([
        frappeCall<FrappeVersion[]>(
          'frappe.client.get_list',
          {
            doctype: 'Version',
            fields: ['name', 'data', 'owner', 'creation'],
            filters: [
              ['ref_doctype', '=', ORDER_DOCTYPE],
              ['docname', '=', name.value],
            ],
            order_by: 'creation desc',
            limit_page_length: 200,
          },
          { signal: abortController.signal },
        ),
        frappeCall<CustomerOrderStatusChange[]>(
          'frappe.client.get_list',
          {
            doctype: 'Customer Order Status Change',
            fields: [
              'name',
              'from_status',
              'to_status',
              'actor_user',
              'event_at',
              'via_admin_correction',
              'note',
            ],
            filters: [['order', '=', name.value]],
            order_by: 'event_at desc',
            limit_page_length: 200,
          },
          { signal: abortController.signal },
        ),
      ])
      versions.value = v
      statusChanges.value = s
    }
    catch (e) {
      if ((e as { name?: string }).name === 'CanceledError')
        return
      error.value = toApiError(e)
    }
    finally {
      loading.value = false
    }
  }

  const entries = computed<TimelineEntry[]>(() => {
    const merged: TimelineEntry[] = []
    for (const sc of statusChanges.value) {
      merged.push({
        id: `status:${sc.name}`,
        kind: 'status',
        at: sc.event_at,
        actor: sc.actor_user,
        via_admin_correction: sc.via_admin_correction === 1,
        details: [
          { fieldname: 'status', from_value: sc.from_status, to_value: sc.to_status },
          ...(sc.note ? [{ fieldname: 'note', to_value: sc.note }] : []),
        ],
      })
    }
    for (const v of versions.value) {
      const diffs = parseVersionData(v.data)
      if (diffs.length === 0)
        continue
      merged.push({
        id: `version:${v.name}`,
        kind: 'edit',
        at: v.creation,
        actor: v.owner,
        details: diffs,
      })
    }
    return merged.sort((a, b) => (a.at < b.at ? 1 : a.at > b.at ? -1 : 0))
  })

  watch(name, () => { void reload() }, { immediate: true })

  onScopeDispose(() => abortController?.abort())

  return { entries, loading, error, reload }
}
