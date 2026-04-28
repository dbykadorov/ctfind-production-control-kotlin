package com.ctfind.productioncontrol.notifications.adapter.persistence

import com.ctfind.productioncontrol.notifications.application.NotificationListQuery
import com.ctfind.productioncontrol.notifications.application.NotificationPageResult
import com.ctfind.productioncontrol.notifications.application.NotificationPersistencePort
import com.ctfind.productioncontrol.notifications.domain.Notification
import com.ctfind.productioncontrol.notifications.domain.NotificationTargetType
import com.ctfind.productioncontrol.notifications.domain.NotificationType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
class NotificationPersistenceAdapter(
	private val repo: NotificationJpaRepository,
) : NotificationPersistencePort {

	override fun save(notification: Notification): Notification {
		val entity = toEntity(notification)
		return toDomain(repo.save(entity))
	}

	override fun findById(id: UUID): Notification? =
		repo.findById(id).map(::toDomain).orElse(null)

	override fun findByRecipientUserId(query: NotificationListQuery): NotificationPageResult<Notification> {
		val pageable = PageRequest.of(query.page, query.size, Sort.by(Sort.Direction.DESC, "createdAt"))
		val page = if (query.unreadOnly) {
			repo.findByRecipientUserIdAndReadFalse(query.recipientUserId, pageable)
		} else {
			repo.findByRecipientUserId(query.recipientUserId, pageable)
		}
		return NotificationPageResult(
			items = page.content.map(::toDomain),
			page = query.page,
			size = query.size,
			totalItems = page.totalElements,
		)
	}

	override fun countUnreadByRecipientUserId(recipientUserId: UUID): Long =
		repo.countByRecipientUserIdAndReadFalse(recipientUserId)

	@Transactional
	override fun markAllReadByRecipientUserId(recipientUserId: UUID, readAt: Instant): Int =
		repo.markAllReadByRecipientUserId(recipientUserId, readAt)

	private fun toEntity(n: Notification) = NotificationEntity(
		id = n.id,
		recipientUserId = n.recipientUserId,
		type = n.type.name,
		title = n.title,
		body = n.body,
		targetType = n.targetType?.name,
		targetId = n.targetId,
		read = n.read,
		readAt = n.readAt,
		createdAt = n.createdAt,
	)

	private fun toDomain(e: NotificationEntity) = Notification(
		id = e.id,
		recipientUserId = e.recipientUserId,
		type = NotificationType.valueOf(e.type),
		title = e.title,
		body = e.body,
		targetType = e.targetType?.let { NotificationTargetType.valueOf(it) },
		targetId = e.targetId,
		read = e.read,
		readAt = e.readAt,
		createdAt = e.createdAt,
	)
}
