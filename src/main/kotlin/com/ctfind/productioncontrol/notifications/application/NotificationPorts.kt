package com.ctfind.productioncontrol.notifications.application

import com.ctfind.productioncontrol.notifications.domain.Notification
import com.ctfind.productioncontrol.notifications.domain.NotificationType
import java.time.Instant
import java.util.UUID

interface NotificationPersistencePort {
	fun save(notification: Notification): Notification
	fun findById(id: UUID): Notification?
	fun findByRecipientUserId(query: NotificationListQuery): NotificationPageResult<Notification>
	fun countUnreadByRecipientUserId(recipientUserId: UUID): Long
	fun markAllReadByRecipientUserId(recipientUserId: UUID, readAt: Instant): Int
	fun existsByTypeAndTargetIdAndRecipient(type: NotificationType, targetId: String, recipientUserId: UUID): Boolean
}

interface NotificationCreatePort {
	fun create(command: CreateNotificationCommand): Notification
}
