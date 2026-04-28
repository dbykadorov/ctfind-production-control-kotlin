package com.ctfind.productioncontrol.notifications.domain

import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NotificationTests {

	private val recipientId = UUID.fromString("10000000-0000-0000-0000-000000000001")
	private val now = Instant.parse("2026-04-28T12:00:00Z")

	private fun validNotification(
		title: String = "Task assigned",
		body: String? = null,
		targetType: NotificationTargetType? = null,
		targetId: String? = null,
	) = Notification(
		recipientUserId = recipientId,
		type = NotificationType.TASK_ASSIGNED,
		title = title,
		body = body,
		targetType = targetType,
		targetId = targetId,
		createdAt = now,
	)

	@Test
	fun `happy path with all fields`() {
		val n = validNotification(
			title = "New task",
			body = "Please review",
			targetType = NotificationTargetType.PRODUCTION_TASK,
			targetId = "PT-001",
		)
		assertEquals("New task", n.title)
		assertEquals("Please review", n.body)
		assertEquals(NotificationTargetType.PRODUCTION_TASK, n.targetType)
		assertEquals("PT-001", n.targetId)
		assertEquals(false, n.read)
		assertEquals(null, n.readAt)
	}

	@Test
	fun `happy path with nullable body and target`() {
		val n = validNotification()
		assertEquals(null, n.body)
		assertEquals(null, n.targetType)
		assertEquals(null, n.targetId)
	}

	@Test
	fun `blank title is rejected`() {
		val e = assertFailsWith<IllegalArgumentException> {
			validNotification(title = "")
		}
		assertEquals("title must not be blank", e.message)
	}

	@Test
	fun `whitespace-only title is rejected`() {
		assertFailsWith<IllegalArgumentException> {
			validNotification(title = "   ")
		}
	}

	@Test
	fun `title exceeding 200 characters is rejected`() {
		val e = assertFailsWith<IllegalArgumentException> {
			validNotification(title = "a".repeat(201))
		}
		assertEquals("title must not exceed 200 characters", e.message)
	}

	@Test
	fun `title at exactly 200 characters is accepted`() {
		val n = validNotification(title = "a".repeat(200))
		assertEquals(200, n.title.length)
	}

	@Test
	fun `body exceeding 1000 characters is rejected`() {
		val e = assertFailsWith<IllegalArgumentException> {
			validNotification(body = "b".repeat(1001))
		}
		assertEquals("body must not exceed 1000 characters", e.message)
	}

	@Test
	fun `body at exactly 1000 characters is accepted`() {
		val n = validNotification(body = "b".repeat(1000))
		assertEquals(1000, n.body!!.length)
	}

	@Test
	fun `targetType without targetId is rejected`() {
		val e = assertFailsWith<IllegalArgumentException> {
			validNotification(targetType = NotificationTargetType.ORDER, targetId = null)
		}
		assertEquals("targetType and targetId must be both null or both non-null", e.message)
	}

	@Test
	fun `targetId without targetType is rejected`() {
		assertFailsWith<IllegalArgumentException> {
			validNotification(targetType = null, targetId = "ORD-123")
		}
	}

	@Test
	fun `both targetType and targetId present is accepted`() {
		val n = validNotification(
			targetType = NotificationTargetType.ORDER,
			targetId = "ORD-123",
		)
		assertEquals(NotificationTargetType.ORDER, n.targetType)
		assertEquals("ORD-123", n.targetId)
	}
}
