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
import kotlin.test.assertNull

class NotificationControllerSecurityTests {

	private val userA = UUID.fromString("10000000-0000-0000-0000-000000000001")
	private val userB = UUID.fromString("10000000-0000-0000-0000-000000000002")

	private fun notification(recipientUserId: UUID) = Notification(
		recipientUserId = recipientUserId,
		type = NotificationType.TASK_ASSIGNED,
		title = "Test",
		read = false,
		createdAt = Instant.parse("2026-04-28T12:00:00Z"),
	)

	@Test
	fun `user A cannot see user B notifications via list`() {
		val nB = notification(userB)
		val controller = controller(nB)

		val response = controller.list(jwtFor(userA), page = 0, size = 20, unreadOnly = false)

		assertEquals(0, response.items.size)
		assertEquals(0, response.totalItems)
	}

	@Test
	fun `user A cannot mark user B notification as read - returns 404`() {
		val nB = notification(userB)
		val controller = controller(nB)

		val response = controller.markRead(jwtFor(userA), nB.id)

		assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
		assertNull(response.body)
	}

	@Test
	fun `user A unread count does not include user B notifications`() {
		val controller = controller(notification(userB), notification(userB))

		val response = controller.unreadCount(jwtFor(userA))
		assertEquals(0, response.count)
	}

	@Test
	fun `user A mark-all-read does not affect user B notifications`() {
		val nB = notification(userB)
		val controller = controller(nB)

		val response = controller.markAllRead(jwtFor(userA))
		assertEquals(0, response.updated)
	}

	private fun jwtFor(uid: UUID): Jwt =
		Jwt.withTokenValue("tok")
			.header("alg", "none")
			.subject("user")
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
			override fun findByRecipientUserId(query: NotificationListQuery): NotificationPageResult<Notification> {
				val filtered = store.filter { it.recipientUserId == query.recipientUserId }
					.let { if (query.unreadOnly) it.filter { n -> !n.read } else it }
				return NotificationPageResult(
					items = filtered,
					page = query.page,
					size = query.size,
					totalItems = filtered.size.toLong(),
				)
			}
			override fun countUnreadByRecipientUserId(recipientUserId: UUID) =
				store.count { it.recipientUserId == recipientUserId && !it.read }.toLong()
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
