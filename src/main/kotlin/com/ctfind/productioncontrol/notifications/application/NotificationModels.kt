package com.ctfind.productioncontrol.notifications.application

import com.ctfind.productioncontrol.notifications.domain.NotificationTargetType
import com.ctfind.productioncontrol.notifications.domain.NotificationType
import java.util.UUID

data class NotificationListQuery(
	val recipientUserId: UUID,
	val unreadOnly: Boolean = false,
	val page: Int = 0,
	val size: Int = 20,
)

data class NotificationPageResult<T>(
	val items: List<T>,
	val page: Int,
	val size: Int,
	val totalItems: Long,
) {
	val totalPages: Int =
		if (size <= 0) 0 else ((totalItems + size - 1) / size).toInt()
}

data class CreateNotificationCommand(
	val recipientUserId: UUID,
	val type: NotificationType,
	val title: String,
	val body: String? = null,
	val targetType: NotificationTargetType? = null,
	val targetId: String? = null,
	val targetEntityId: UUID? = null,
)
