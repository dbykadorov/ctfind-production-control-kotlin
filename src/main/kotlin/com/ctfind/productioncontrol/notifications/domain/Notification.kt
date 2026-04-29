package com.ctfind.productioncontrol.notifications.domain

import java.time.Instant
import java.util.UUID

data class Notification(
	val id: UUID = UUID.randomUUID(),
	val recipientUserId: UUID,
	val type: NotificationType,
	val title: String,
	val body: String? = null,
	val targetType: NotificationTargetType? = null,
	val targetId: String? = null,
	val targetEntityId: UUID? = null,
	val read: Boolean = false,
	val readAt: Instant? = null,
	val createdAt: Instant,
) {
	init {
		require(title.isNotBlank()) { "title must not be blank" }
		require(title.length <= 200) { "title must not exceed 200 characters" }
		if (body != null) {
			require(body.length <= 1000) { "body must not exceed 1000 characters" }
		}
		require((targetType == null) == (targetId == null)) {
			"targetType and targetId must be both null or both non-null"
		}
	}
}
