package com.ctfind.productioncontrol.notifications.application

import com.ctfind.productioncontrol.notifications.domain.Notification
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CreateNotificationUseCase(
	private val persistence: NotificationPersistencePort,
) : NotificationCreatePort {

	override fun create(command: CreateNotificationCommand): Notification {
		val notification = Notification(
			recipientUserId = command.recipientUserId,
			type = command.type,
			title = command.title,
			body = command.body,
			targetType = command.targetType,
			targetId = command.targetId,
			read = false,
			readAt = null,
			createdAt = Instant.now(),
		)
		return persistence.save(notification)
	}
}
