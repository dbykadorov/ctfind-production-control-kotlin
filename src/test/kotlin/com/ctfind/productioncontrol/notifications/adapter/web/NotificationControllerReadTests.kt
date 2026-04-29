package com.ctfind.productioncontrol.notifications.adapter.web

import com.ctfind.productioncontrol.notifications.application.ListNotificationsUseCase
import com.ctfind.productioncontrol.notifications.application.MarkNotificationReadUseCase
import com.ctfind.productioncontrol.notifications.application.NotificationListQuery
import com.ctfind.productioncontrol.notifications.application.NotificationPageResult
import com.ctfind.productioncontrol.notifications.application.NotificationPersistencePort
import com.ctfind.productioncontrol.notifications.domain.Notification
import com.ctfind.productioncontrol.notifications.domain.NotificationType
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NotificationControllerReadTests {

	private val userId = UUID.fromString("10000000-0000-0000-0000-000000000001")
	private val otherUserId = UUID.fromString("10000000-0000-0000-0000-000000000002")

	private fun notification(
		recipientUserId: UUID = userId,
		read: Boolean = false,
		readAt: Instant? = null,
	) = Notification(
		recipientUserId = recipientUserId,
		type = NotificationType.TASK_ASSIGNED,
		title = "Test",
		read = read,
		readAt = readAt,
		createdAt = Instant.parse("2026-04-28T12:00:00Z"),
	)

	@Test
	fun `markRead returns 200 with read state`() {
		val n = notification()
		val controller = controller(n)

		val response = controller.markRead(jwtFor(userId), n.id)

		assertEquals(HttpStatus.OK, response.statusCode)
		val body = response.body!!
		assertEquals(n.id, body.id)
		assertTrue(body.read)
		assertNotNull(body.readAt)
	}

	@Test
	fun `markRead is idempotent - preserves readAt`() {
		val readAt = Instant.parse("2026-04-28T10:00:00Z")
		val n = notification(read = true, readAt = readAt)
		val controller = controller(n)

		val response = controller.markRead(jwtFor(userId), n.id)

		assertEquals(HttpStatus.OK, response.statusCode)
		assertEquals(readAt, response.body!!.readAt)
	}

	@Test
	fun `markRead returns 404 for foreign notification`() {
		val n = notification(recipientUserId = otherUserId)
		val controller = controller(n)

		val response = controller.markRead(jwtFor(userId), n.id)

		assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
	}

	@Test
	fun `markRead returns 404 for missing notification`() {
		val controller = controller()

		val response = controller.markRead(jwtFor(userId), UUID.randomUUID())

		assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
	}

	@Test
	fun `markAllRead returns updated count`() {
		val n1 = notification(read = false)
		val n2 = notification(read = false)
		val n3 = notification(read = true, readAt = Instant.now())
		val controller = controller(n1, n2, n3)

		val response = controller.markAllRead(jwtFor(userId))

		assertEquals(2, response.updated)
	}

	@Test
	fun `markAllRead returns zero when none unread`() {
		val n = notification(read = true, readAt = Instant.now())
		val controller = controller(n)

		val response = controller.markAllRead(jwtFor(userId))

		assertEquals(0, response.updated)
	}

	private fun jwtFor(uid: UUID): Jwt =
		Jwt.withTokenValue("tok")
			.header("alg", "none")
			.subject("user1")
			.claim("userId", uid.toString())
			.build()

	private fun controller(vararg notifications: Notification): NotificationController {
		val store = notifications.toMutableList()
		val persistence = object : NotificationPersistencePort {
			override fun save(notification: Notification): Notification {
				store.removeIf { it.id == notification.id }
				store.add(notification)
				return notification
			}
			override fun findById(id: UUID) = store.find { it.id == id }
			override fun findByRecipientUserId(query: NotificationListQuery) =
				NotificationPageResult<Notification>(emptyList(), 0, 20, 0)
			override fun countUnreadByRecipientUserId(recipientUserId: UUID) = 0L
			override fun markAllReadByRecipientUserId(recipientUserId: UUID, readAt: Instant): Int {
				var count = 0
				store.replaceAll { n ->
					if (n.recipientUserId == recipientUserId && !n.read) {
						count++
						n.copy(read = true, readAt = readAt)
					} else {
						n
					}
				}
				return count
			}
			override fun existsByTypeAndTargetIdAndRecipient(type: com.ctfind.productioncontrol.notifications.domain.NotificationType, targetId: String, recipientUserId: UUID) = false
		}
		return NotificationController(
			listUseCase = ListNotificationsUseCase(persistence),
			markReadUseCase = MarkNotificationReadUseCase(persistence),
		)
	}
}
