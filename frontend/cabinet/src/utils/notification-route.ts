import type { RouteLocationRaw } from 'vue-router'
import type { NotificationResponse } from '@/api/types/notifications'

export function notificationRoute(n: NotificationResponse): RouteLocationRaw | null {
  if (!n.targetType || !n.targetEntityId) return null
  switch (n.targetType) {
    case 'PRODUCTION_TASK':
      return { name: 'production-tasks.detail', params: { id: n.targetEntityId } }
    case 'ORDER':
      return { name: 'orders.detail', params: { name: n.targetEntityId } }
    default:
      return null
  }
}
