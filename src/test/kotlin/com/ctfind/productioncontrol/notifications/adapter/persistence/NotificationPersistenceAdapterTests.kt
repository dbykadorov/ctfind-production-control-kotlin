package com.ctfind.productioncontrol.notifications.adapter.persistence

import com.ctfind.productioncontrol.notifications.domain.Notification
import com.ctfind.productioncontrol.notifications.domain.NotificationTargetType
import com.ctfind.productioncontrol.notifications.domain.NotificationType
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NotificationPersistenceAdapterTests {

	private val userA = UUID.fromString("10000000-0000-0000-0000-000000000001")

	@Test
	fun `toEntity maps all domain fields correctly`() {
		val n = Notification(
			id = UUID.fromString("20000000-0000-0000-0000-000000000001"),
			recipientUserId = userA,
			type = NotificationType.TASK_ASSIGNED,
			title = "New task",
			body = "Check PT-001",
			targetType = NotificationTargetType.PRODUCTION_TASK,
			targetId = "PT-001",
			read = false,
			readAt = null,
			createdAt = Instant.parse("2026-04-28T12:00:00Z"),
		)

		val entity = NotificationEntity(
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

		assertEquals(n.id, entity.id)
		assertEquals(n.recipientUserId, entity.recipientUserId)
		assertEquals("TASK_ASSIGNED", entity.type)
		assertEquals("New task", entity.title)
		assertEquals("Check PT-001", entity.body)
		assertEquals("PRODUCTION_TASK", entity.targetType)
		assertEquals("PT-001", entity.targetId)
		assertEquals(false, entity.read)
		assertNull(entity.readAt)
		assertEquals(Instant.parse("2026-04-28T12:00:00Z"), entity.createdAt)
	}

	@Test
	fun `toDomain round-trips via entity correctly`() {
		val entity = NotificationEntity(
			id = UUID.fromString("20000000-0000-0000-0000-000000000001"),
			recipientUserId = userA,
			type = "STATUS_CHANGED",
			title = "Status updated",
			body = null,
			targetType = "ORDER",
			targetId = "ORD-42",
			read = true,
			readAt = Instant.parse("2026-04-28T13:00:00Z"),
			createdAt = Instant.parse("2026-04-28T12:00:00Z"),
		)

		val domain = Notification(
			id = entity.id,
			recipientUserId = entity.recipientUserId,
			type = NotificationType.valueOf(entity.type),
			title = entity.title,
			body = entity.body,
			targetType = entity.targetType?.let { NotificationTargetType.valueOf(it) },
			targetId = entity.targetId,
			read = entity.read,
			readAt = entity.readAt,
			createdAt = entity.createdAt,
		)

		assertEquals(entity.id, domain.id)
		assertEquals(NotificationType.STATUS_CHANGED, domain.type)
		assertEquals(NotificationTargetType.ORDER, domain.targetType)
		assertEquals("ORD-42", domain.targetId)
		assertEquals(true, domain.read)
		assertNotNull(domain.readAt)
	}

	@Test
	fun `nullable fields map correctly in both directions`() {
		val n = Notification(
			recipientUserId = userA,
			type = NotificationType.TASK_OVERDUE,
			title = "Overdue",
			body = null,
			targetType = null,
			targetId = null,
			createdAt = Instant.now(),
		)

		val entity = NotificationEntity(
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

		assertNull(entity.body)
		assertNull(entity.targetType)
		assertNull(entity.targetId)
		assertNull(entity.readAt)

		val roundTripped = Notification(
			id = entity.id,
			recipientUserId = entity.recipientUserId,
			type = NotificationType.valueOf(entity.type),
			title = entity.title,
			body = entity.body,
			targetType = entity.targetType?.let { NotificationTargetType.valueOf(it) },
			targetId = entity.targetId,
			read = entity.read,
			readAt = entity.readAt,
			createdAt = entity.createdAt,
		)

		assertEquals(n, roundTripped)
	}
}
