package com.ctfind.productioncontrol.notifications.application

import com.ctfind.productioncontrol.notifications.domain.Notification
import com.ctfind.productioncontrol.notifications.domain.NotificationType
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarkNotificationReadUseCaseTests {

	private val userA = UUID.fromString("10000000-0000-0000-0000-000000000001")
	private val userB = UUID.fromString("10000000-0000-0000-0000-000000000002")

	private fun notification(
		recipientUserId: UUID = userA,
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

	private fun useCase(vararg notifications: Notification): MarkNotificationReadUseCase {
		val store = notifications.toMutableList()
		return MarkNotificationReadUseCase(
			persistence = object : NotificationPersistencePort {
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
			},
		)
	}

	@Test
	fun `first read sets readAt`() {
		val n = notification()
		val uc = useCase(n)

		val before = Instant.now()
		val result = uc.markRead(n.id, userA)
		val after = Instant.now()

		assertNotNull(result)
		assertTrue(result.read)
		assertNotNull(result.readAt)
		assertTrue(result.readAt!! >= before)
		assertTrue(result.readAt!! <= after)
	}

	@Test
	fun `second read preserves original readAt`() {
		val originalReadAt = Instant.parse("2026-04-28T10:00:00Z")
		val n = notification(read = true, readAt = originalReadAt)
		val uc = useCase(n)

		val result = uc.markRead(n.id, userA)

		assertNotNull(result)
		assertEquals(originalReadAt, result.readAt)
	}

	@Test
	fun `foreign notification returns null`() {
		val n = notification(recipientUserId = userA)
		val uc = useCase(n)

		val result = uc.markRead(n.id, userB)
		assertNull(result)
	}

	@Test
	fun `missing notification returns null`() {
		val uc = useCase()
		val result = uc.markRead(UUID.randomUUID(), userA)
		assertNull(result)
	}

	@Test
	fun `markAllRead returns affected count`() {
		val n1 = notification(read = false)
		val n2 = notification(read = false)
		val n3 = notification(read = true, readAt = Instant.now())
		val n4 = notification(recipientUserId = userB, read = false)
		val uc = useCase(n1, n2, n3, n4)

		val count = uc.markAllRead(userA)
		assertEquals(2, count)
	}

	@Test
	fun `markAllRead returns zero when none unread`() {
		val n = notification(read = true, readAt = Instant.now())
		val uc = useCase(n)

		assertEquals(0, uc.markAllRead(userA))
	}
}
