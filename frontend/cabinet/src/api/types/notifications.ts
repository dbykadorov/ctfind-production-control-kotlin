export type NotificationType = 'TASK_ASSIGNED' | 'STATUS_CHANGED' | 'TASK_OVERDUE'
export type NotificationTargetType = 'ORDER' | 'PRODUCTION_TASK'

export interface NotificationResponse {
  id: string
  type: NotificationType
  title: string
  body: string | null
  targetType: NotificationTargetType | null
  targetId: string | null
  targetEntityId: string | null
  read: boolean
  readAt: string | null
  createdAt: string
}

export interface NotificationsPageResponse {
  items: NotificationResponse[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface UnreadCountResponse {
  count: number
}

export interface MarkReadResponse {
  id: string
  read: boolean
  readAt: string | null
}

export interface MarkAllReadResponse {
  updated: number
}
