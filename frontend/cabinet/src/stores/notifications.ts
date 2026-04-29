import type { NotificationResponse, NotificationsPageResponse, UnreadCountResponse } from '@/api/types/notifications'
import { httpClient } from '@/api/api-client'
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useNotificationStore = defineStore('notifications', () => {
  const unreadCount = ref(0)
  const dropdownItems = ref<NotificationResponse[] | null>(null)
  const dropdownLoading = ref(false)
  let pollTimer: ReturnType<typeof setInterval> | null = null

  async function fetchUnreadCount() {
    try {
      const res = await httpClient.get<UnreadCountResponse>('/api/notifications/unread-count')
      unreadCount.value = res.data.count
    } catch {
      // silent — badge just keeps last known value
    }
  }

  async function fetchDropdown() {
    dropdownLoading.value = true
    try {
      const res = await httpClient.get<NotificationsPageResponse>('/api/notifications', {
        params: { page: 0, size: 10 },
      })
      dropdownItems.value = res.data.items
    } catch {
      // silent
    } finally {
      dropdownLoading.value = false
    }
  }

  async function markRead(id: string) {
    try {
      await httpClient.patch(`/api/notifications/${id}/read`)
      if (dropdownItems.value) {
        const item = dropdownItems.value.find(n => n.id === id)
        if (item && !item.read) {
          item.read = true
          item.readAt = new Date().toISOString()
          unreadCount.value = Math.max(0, unreadCount.value - 1)
        }
      }
    } catch {
      // silent
    }
  }

  async function markAllRead() {
    try {
      await httpClient.post('/api/notifications/mark-all-read')
      unreadCount.value = 0
      if (dropdownItems.value) {
        const now = new Date().toISOString()
        dropdownItems.value.forEach(n => {
          n.read = true
          n.readAt = now
        })
      }
    } catch {
      // silent
    }
  }

  function startPolling() {
    if (pollTimer) return
    fetchUnreadCount()
    pollTimer = setInterval(fetchUnreadCount, 30_000)
    document.addEventListener('visibilitychange', onVisibilityChange)
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
    document.removeEventListener('visibilitychange', onVisibilityChange)
  }

  function onVisibilityChange() {
    if (document.visibilityState === 'hidden') {
      if (pollTimer) {
        clearInterval(pollTimer)
        pollTimer = null
      }
    } else {
      fetchUnreadCount()
      pollTimer = setInterval(fetchUnreadCount, 30_000)
    }
  }

  return {
    unreadCount,
    dropdownItems,
    dropdownLoading,
    fetchUnreadCount,
    fetchDropdown,
    markRead,
    markAllRead,
    startPolling,
    stopPolling,
  }
})
