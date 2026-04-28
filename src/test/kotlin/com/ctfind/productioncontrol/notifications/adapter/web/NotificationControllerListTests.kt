package com.ctfind.productioncontrol.notifications.adapter.web

import com.ctfind.productioncontrol.notifications.application.ListNotificationsUseCase
import com.ctfind.productioncontrol.notifications.application.MarkNotificationReadUseCase
import com.ctfind.productioncontrol.notifications.application.NotificationListQuery
import com.ctfind.productioncontrol.notifications.application.NotificationPageResult
import com.ctfind.productioncontrol.notifications.application.NotificationPersistencePort
import com.ctfind.productioncontrol.notifications.domain.Notification
import com.ctfind.productioncontrol.notifications.domain.NotificationTargetType
import com.ctfind.productioncontrol.notifications.domain.NotificationType
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationControllerListTests {

	private val userId = UUID.fromString("10000000-0000-0000-0000-000000000001")

	private val sampleNotification = Notification(
		id = UUID.fromString("20000000-0000-0000-0000-000000000001"),
		recipientUserId = userId,
		type = NotificationType.TASK_ASSIGNED,
		title = "New task",
		body = "Check PT-001",
		targetType = NotificationTargetType.PRODUCTION_TASK,
		targetId = "PT-001",
		read = false,
		readAt = null,
		createdAt = Instant.parse("2026-04-28T12:00:00Z"),
	)

	@Test
	fun `list returns paginated notifications`() {
		val controller = controller(
			listOf(sampleNotification),
			totalItems = 1,
		)

		val response = controller.list(jwtFor(userId), page = 0, size = 20, unreadOnly = false)

		assertEquals(1, response.items.size)
		assertEquals(sampleNotification.id, response.items[0].id)
		assertEquals("New task", response.items[0].title)
		assertEquals("Check PT-001", response.items[0].body)
		assertEquals(NotificationType.TASK_ASSIGNED, response.items[0].type)
		assertEquals(NotificationTargetType.PRODUCTION_TASK, response.items[0].targetType)
		assertEquals("PT-001", response.items[0].targetId)
		assertEquals(false, response.items[0].read)
		assertEquals(1, response.totalItems)
	}

	@Test
	fun `list returns empty page when no notifications`() {
		val controller = controller(emptyList(), totalItems = 0)

		val response = controller.list(jwtFor(userId), page = 0, size = 20, unreadOnly = false)

		assertEquals(0, response.items.size)
		assertEquals(0, response.totalItems)
	}

	@Test
	fun `size is clamped to 1-100 range`() {
		var capturedSize: Int? = null
		val controller = controller(emptyList(), totalItems = 0, onQuery = { capturedSize = it.size })

		controller.list(jwtFor(userId), page = 0, size = 0, unreadOnly = false)
		assertEquals(1, capturedSize)

		controller.list(jwtFor(userId), page = 0, size = 200, unreadOnly = false)
		assertEquals(100, capturedSize)

		controller.list(jwtFor(userId), page = 0, size = 50, unreadOnly = false)
		assertEquals(50, capturedSize)
	}

	@Test
	fun `unreadOnly param is forwarded`() {
		var capturedUnreadOnly: Boolean? = null
		val controller = controller(emptyList(), totalItems = 0, onQuery = { capturedUnreadOnly = it.unreadOnly })

		controller.list(jwtFor(userId), page = 0, size = 20, unreadOnly = true)
		assertEquals(true, capturedUnreadOnly)
	}

	@Test
	fun `unread-count returns count`() {
		val controller = controller(emptyList(), totalItems = 0, unreadCount = 5)

		val response = controller.unreadCount(jwtFor(userId))
		assertEquals(5, response.count)
	}

	@Test
	fun `unread-count returns zero`() {
		val controller = controller(emptyList(), totalItems = 0, unreadCount = 0)

		val response = controller.unreadCount(jwtFor(userId))
		assertEquals(0, response.count)
	}

	private fun jwtFor(uid: UUID): Jwt =
		Jwt.withTokenValue("tok")
			.header("alg", "none")
			.subject("user1")
			.claim("userId", uid.toString())
			.build()

	private fun controller(
		notifications: List<Notification>,
		totalItems: Long,
		unreadCount: Long = 0,
		onQuery: (NotificationListQuery) -> Unit = {},
	): NotificationController {
		val listUc = ListNotificationsUseCase(
			persistence = object : NotificationPersistencePort {
				override fun save(notification: Notification) = notification
				override fun findById(id: UUID): Notification? = null
				override fun findByRecipientUserId(query: NotificationListQuery): NotificationPageResult<Notification> {
					onQuery(query)
					return NotificationPageResult(
						items = notifications,
						page = query.page,
						size = query.size,
						totalItems = totalItems,
					)
				}
				override fun countUnreadByRecipientUserId(recipientUserId: UUID) = unreadCount
				override fun markAllReadByRecipientUserId(recipientUserId: UUID, readAt: Instant) = 0
			},
		)
		val markReadUc = MarkNotificationReadUseCase(
			persistence = object : NotificationPersistencePort {
				override fun save(notification: Notification) = notification
				override fun findById(id: UUID): Notification? = null
				override fun findByRecipientUserId(query: NotificationListQuery) =
					NotificationPageResult<Notification>(emptyList(), 0, 20, 0)
				override fun countUnreadByRecipientUserId(recipientUserId: UUID) = 0L
				override fun markAllReadByRecipientUserId(recipientUserId: UUID, readAt: Instant) = 0
			},
		)
		return NotificationController(listUc, markReadUc)
	}
}
