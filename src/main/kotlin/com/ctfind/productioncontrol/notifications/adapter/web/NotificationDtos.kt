package com.ctfind.productioncontrol.notifications.adapter.web

import com.ctfind.productioncontrol.notifications.domain.Notification
import com.ctfind.productioncontrol.notifications.domain.NotificationTargetType
import com.ctfind.productioncontrol.notifications.domain.NotificationType
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.util.UUID

data class NotificationResponse(
	val id: UUID,
	val type: NotificationType,
	val title: String,
	val body: String?,
	val targetType: NotificationTargetType?,
	val targetId: String?,
	val read: Boolean,
	val readAt: Instant?,
	val createdAt: Instant,
)

data class NotificationPageResponse(
	val items: List<NotificationResponse>,
	val page: Int,
	val size: Int,
	val totalItems: Long,
	val totalPages: Int,
)

data class UnreadCountResponse(
	val count: Long,
)

data class MarkReadResponse(
	val id: UUID,
	val read: Boolean,
	val readAt: Instant?,
)

data class MarkAllReadResponse(
	val updated: Int,
)

fun Notification.toResponse() = NotificationResponse(
	id = id,
	type = type,
	title = title,
	body = body,
	targetType = targetType,
	targetId = targetId,
	read = read,
	readAt = readAt,
	createdAt = createdAt,
)

fun Jwt.extractUserId(): UUID =
	(claims["userId"] as? String)?.let(UUID::fromString)
		?: UUID.nameUUIDFromBytes(subject.toByteArray())
