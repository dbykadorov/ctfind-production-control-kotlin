/**
 * История задачи (Feature 005 US5): преобразование API-событий в человекочитаемые
 * пункты таймлайна — заголовок, детальная строка и значки статусов.
 */
import type {
  ProductionTaskHistoryEventResponse,
  ProductionTaskHistoryEventType,
  ProductionTaskStatus,
} from '@/api/types/production-tasks'
import { format, parseISO } from 'date-fns'
import { ru } from 'date-fns/locale'

export interface ProductionTaskTimelineEntry {
  type: ProductionTaskHistoryEventType | string
  title: string
  details: string[]
  actorDisplayName: string
  eventAt: string
  eventAtLabel: string
  fromStatus?: ProductionTaskStatus | null
  toStatus?: ProductionTaskStatus | null
  reason?: string | null
  note?: string | null
}

const TYPE_TITLE_RU: Record<ProductionTaskHistoryEventType, string> = {
  CREATED: 'Задача создана',
  ASSIGNED: 'Назначен исполнитель',
  PLANNING_UPDATED: 'План скорректирован',
  STATUS_CHANGED: 'Изменён статус',
  BLOCKED: 'Заблокировано',
  UNBLOCKED: 'Разблокировано',
  COMPLETED: 'Завершено',
}

const STATUS_LABEL_RU: Record<ProductionTaskStatus, string> = {
  NOT_STARTED: 'не начато',
  IN_PROGRESS: 'в работе',
  BLOCKED: 'заблокировано',
  COMPLETED: 'выполнено',
}

function formatDate(input?: string | null): string | null {
  if (!input)
    return null
  try {
    return format(parseISO(input), 'd MMM yyyy', { locale: ru })
  }
  catch {
    return input
  }
}

function formatTimestamp(input: string): string {
  try {
    return format(parseISO(input), "d MMM yyyy, HH:mm", { locale: ru })
  }
  catch {
    return input
  }
}

function statusLabel(status?: ProductionTaskStatus | null): string | null {
  return status ? STATUS_LABEL_RU[status] : null
}

function buildExecutorLine(prev?: string | null, next?: string | null): string | null {
  if (!prev && !next)
    return null
  if (prev && next)
    return `Исполнитель: ${prev} → ${next}`
  if (next)
    return `Исполнитель: ${next}`
  return `Исполнитель снят (был ${prev})`
}

function buildPlannedLine(prefix: string, before?: string | null, after?: string | null): string | null {
  const beforeLabel = formatDate(before)
  const afterLabel = formatDate(after)
  if (!beforeLabel && !afterLabel)
    return null
  if (beforeLabel && afterLabel && beforeLabel === afterLabel)
    return null
  if (beforeLabel && afterLabel)
    return `${prefix}: ${beforeLabel} → ${afterLabel}`
  if (afterLabel)
    return `${prefix}: ${afterLabel}`
  return `${prefix}: снято (было ${beforeLabel})`
}

function buildStatusLine(from?: ProductionTaskStatus | null, to?: ProductionTaskStatus | null): string | null {
  const fromLabel = statusLabel(from)
  const toLabel = statusLabel(to)
  if (!toLabel)
    return null
  if (fromLabel)
    return `Статус: ${fromLabel} → ${toLabel}`
  return `Статус: ${toLabel}`
}

export function formatProductionTaskHistoryEvent(
  ev: ProductionTaskHistoryEventResponse,
): ProductionTaskTimelineEntry {
  const knownType = (TYPE_TITLE_RU as Record<string, string>)[ev.type] ?? ev.type
  const details: string[] = []

  const executorLine = buildExecutorLine(ev.previousExecutorDisplayName, ev.newExecutorDisplayName)
  if (executorLine)
    details.push(executorLine)

  const startLine = buildPlannedLine('План: начало', ev.plannedStartDateBefore, ev.plannedStartDateAfter)
  if (startLine)
    details.push(startLine)

  const finishLine = buildPlannedLine('План: окончание', ev.plannedFinishDateBefore, ev.plannedFinishDateAfter)
  if (finishLine)
    details.push(finishLine)

  if (ev.type !== 'BLOCKED' && ev.type !== 'UNBLOCKED' && ev.type !== 'COMPLETED') {
    const statusLine = buildStatusLine(ev.fromStatus, ev.toStatus)
    if (statusLine)
      details.push(statusLine)
  }

  if (ev.reason)
    details.push(`Причина: ${ev.reason}`)
  if (ev.note)
    details.push(`Комментарий: ${ev.note}`)

  return {
    type: ev.type,
    title: knownType,
    details,
    actorDisplayName: ev.actorDisplayName,
    eventAt: ev.eventAt,
    eventAtLabel: formatTimestamp(ev.eventAt),
    fromStatus: ev.fromStatus ?? null,
    toStatus: ev.toStatus ?? null,
    reason: ev.reason ?? null,
    note: ev.note ?? null,
  }
}

export function mapProductionTaskHistory(
  events: readonly ProductionTaskHistoryEventResponse[] | undefined,
): ProductionTaskTimelineEntry[] {
  return (events ?? []).map(formatProductionTaskHistoryEvent)
}
