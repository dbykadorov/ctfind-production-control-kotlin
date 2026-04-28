export type AuditCategory = 'AUTH' | 'ORDER' | 'PRODUCTION_TASK'

export interface AuditLogRowResponse {
  id: string
  occurredAt: string
  category: AuditCategory
  eventType: string
  actorDisplayName: string
  actorLogin: string | null
  summary: string
  targetType: string | null
  targetId: string | null
}

export interface AuditLogPageResponse {
  items: AuditLogRowResponse[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface AuditLogFilters {
  from?: string
  to?: string
  category?: AuditCategory[]
  actorUserId?: string
  search?: string
  page?: number
  size?: number
}

export interface UserSummaryResponse {
  id: string
  login: string
  displayName: string
}
