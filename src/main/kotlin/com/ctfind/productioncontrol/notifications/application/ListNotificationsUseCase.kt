package com.ctfind.productioncontrol.notifications.application

import com.ctfind.productioncontrol.notifications.domain.Notification
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ListNotificationsUseCase(
	private val persistence: NotificationPersistencePort,
) {

	fun list(query: NotificationListQuery): NotificationPageResult<Notification> =
		persistence.findByRecipientUserId(query)

	fun countUnread(recipientUserId: UUID): Long =
		persistence.countUnreadByRecipientUserId(recipientUserId)
}
