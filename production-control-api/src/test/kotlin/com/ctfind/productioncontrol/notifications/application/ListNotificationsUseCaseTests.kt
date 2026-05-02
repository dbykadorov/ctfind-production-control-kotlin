package com.ctfind.productioncontrol.notifications.application

import com.ctfind.productioncontrol.notifications.domain.Notification
import com.ctfind.productioncontrol.notifications.domain.NotificationType
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class ListNotificationsUseCaseTests {

	private val userA = UUID.fromString("10000000-0000-0000-0000-000000000001")
	private val userB = UUID.fromString("10000000-0000-0000-0000-000000000002")

	private fun notification(
		recipientUserId: UUID,
		read: Boolean = false,
		createdAt: Instant = Instant.now(),
	) = Notification(
		recipientUserId = recipientUserId,
		type = NotificationType.TASK_ASSIGNED,
		title = "Test notification",
		read = read,
		readAt = if (read) createdAt else null,
		createdAt = createdAt,
	)

	private fun useCase(notifications: List<Notification>): ListNotificationsUseCase {
		val store = notifications.toMutableList()
		return ListNotificationsUseCase(
			persistence = object : NotificationPersistencePort {
				override fun save(notification: Notification): Notification {
					store.add(notification)
					return notification
				}
				override fun findById(id: UUID) = store.find { it.id == id }
				override fun findByRecipientUserId(query: NotificationListQuery): NotificationPageResult<Notification> {
					val filtered = store.filter { it.recipientUserId == query.recipientUserId }
						.let { list -> if (query.unreadOnly) list.filter { !it.read } else list }
						.sortedByDescending { it.createdAt }
					val start = query.page * query.size
					val pageItems = filtered.drop(start).take(query.size)
					return NotificationPageResult(
						items = pageItems,
						page = query.page,
						size = query.size,
						totalItems = filtered.size.toLong(),
					)
				}
				override fun countUnreadByRecipientUserId(recipientUserId: UUID) =
					store.count { it.recipientUserId == recipientUserId && !it.read }.toLong()
				override fun markAllReadByRecipientUserId(recipientUserId: UUID, readAt: Instant) = 0
				override fun existsByTypeAndTargetIdAndRecipient(type: com.ctfind.productioncontrol.notifications.domain.NotificationType, targetId: String, recipientUserId: UUID) = false
			},
		)
	}

	@Test
	fun `user sees only their own notifications`() {
		val uc = useCase(
			listOf(
				notification(userA),
				notification(userB),
				notification(userA),
			),
		)

		val result = uc.list(NotificationListQuery(recipientUserId = userA))
		assertEquals(2, result.items.size)
		result.items.forEach { assertEquals(userA, it.recipientUserId) }
	}

	@Test
	fun `unreadOnly filters out read notifications`() {
		val uc = useCase(
			listOf(
				notification(userA, read = false),
				notification(userA, read = true),
				notification(userA, read = false),
			),
		)

		val result = uc.list(NotificationListQuery(recipientUserId = userA, unreadOnly = true))
		assertEquals(2, result.items.size)
		result.items.forEach { assertEquals(false, it.read) }
	}

	@Test
	fun `pagination works correctly`() {
		val notifications = (1..5).map {
			notification(userA, createdAt = Instant.parse("2026-04-28T12:0${it}:00Z"))
		}
		val uc = useCase(notifications)

		val page0 = uc.list(NotificationListQuery(recipientUserId = userA, page = 0, size = 2))
		assertEquals(2, page0.items.size)
		assertEquals(5, page0.totalItems)
		assertEquals(3, page0.totalPages)

		val page2 = uc.list(NotificationListQuery(recipientUserId = userA, page = 2, size = 2))
		assertEquals(1, page2.items.size)
	}

	@Test
	fun `countUnread returns only unread for specific user`() {
		val uc = useCase(
			listOf(
				notification(userA, read = false),
				notification(userA, read = true),
				notification(userA, read = false),
				notification(userB, read = false),
			),
		)

		assertEquals(2, uc.countUnread(userA))
		assertEquals(1, uc.countUnread(userB))
	}

	@Test
	fun `empty result for user with no notifications`() {
		val uc = useCase(listOf(notification(userA)))

		val result = uc.list(NotificationListQuery(recipientUserId = userB))
		assertEquals(0, result.items.size)
		assertEquals(0, result.totalItems)
	}
}
