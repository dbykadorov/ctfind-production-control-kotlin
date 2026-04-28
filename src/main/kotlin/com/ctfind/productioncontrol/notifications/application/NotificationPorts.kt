package com.ctfind.productioncontrol.notifications.application

import com.ctfind.productioncontrol.notifications.domain.Notification
import java.time.Instant
import java.util.UUID

interface NotificationPersistencePort {
	fun save(notification: Notification): Notification
	fun findById(id: UUID): Notification?
	fun findByRecipientUserId(query: NotificationListQuery): NotificationPageResult<Notification>
	fun countUnreadByRecipientUserId(recipientUserId: UUID): Long
	fun markAllReadByRecipientUserId(recipientUserId: UUID, readAt: Instant): Int
}

interface NotificationCreatePort {
	fun create(command: CreateNotificationCommand): Notification
}
