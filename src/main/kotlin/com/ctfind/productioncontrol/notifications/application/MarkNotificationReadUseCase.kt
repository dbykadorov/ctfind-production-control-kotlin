package com.ctfind.productioncontrol.notifications.application

import com.ctfind.productioncontrol.notifications.domain.Notification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class MarkNotificationReadUseCase(
	private val persistence: NotificationPersistencePort,
) {

	fun markRead(notificationId: UUID, currentUserId: UUID): Notification? {
		val notification = persistence.findById(notificationId)
			?: return null
		if (notification.recipientUserId != currentUserId) return null
		if (notification.read) return notification

		val updated = notification.copy(
			read = true,
			readAt = Instant.now(),
		)
		return persistence.save(updated)
	}

	@Transactional
	fun markAllRead(currentUserId: UUID): Int =
		persistence.markAllReadByRecipientUserId(currentUserId, Instant.now())
}
