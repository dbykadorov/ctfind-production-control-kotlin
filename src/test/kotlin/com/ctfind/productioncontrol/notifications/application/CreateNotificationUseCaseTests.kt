package com.ctfind.productioncontrol.notifications.application

import com.ctfind.productioncontrol.notifications.domain.Notification
import com.ctfind.productioncontrol.notifications.domain.NotificationTargetType
import com.ctfind.productioncontrol.notifications.domain.NotificationType
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateNotificationUseCaseTests {

	private val recipientId = UUID.fromString("10000000-0000-0000-0000-000000000001")

	private fun useCase(
		onSave: (Notification) -> Unit = {},
	): CreateNotificationUseCase {
		val store = mutableListOf<Notification>()
		return CreateNotificationUseCase(
			persistence = object : NotificationPersistencePort {
				override fun save(notification: Notification): Notification {
					onSave(notification)
					store.add(notification)
					return notification
				}
				override fun findById(id: UUID): Notification? = store.find { it.id == id }
				override fun findByRecipientUserId(query: NotificationListQuery) =
					NotificationPageResult<Notification>(emptyList(), 0, 20, 0)
				override fun countUnreadByRecipientUserId(recipientUserId: UUID) = 0L
				override fun markAllReadByRecipientUserId(recipientUserId: UUID, readAt: Instant) = 0
			},
		)
	}

	@Test
	fun `successful creation sets read false and createdAt`() {
		var saved: Notification? = null
		val uc = useCase(onSave = { saved = it })

		val result = uc.create(
			CreateNotificationCommand(
				recipientUserId = recipientId,
				type = NotificationType.TASK_ASSIGNED,
				title = "You have a new task",
				body = "Please check PT-001",
				targetType = NotificationTargetType.PRODUCTION_TASK,
				targetId = "PT-001",
			),
		)

		assertNotNull(saved)
		assertEquals(false, result.read)
		assertNull(result.readAt)
		assertNotNull(result.createdAt)
		assertEquals(recipientId, result.recipientUserId)
		assertEquals(NotificationType.TASK_ASSIGNED, result.type)
		assertEquals("You have a new task", result.title)
		assertEquals("Please check PT-001", result.body)
		assertEquals(NotificationTargetType.PRODUCTION_TASK, result.targetType)
		assertEquals("PT-001", result.targetId)
	}

	@Test
	fun `creation with nullable body and target succeeds`() {
		val result = useCase().create(
			CreateNotificationCommand(
				recipientUserId = recipientId,
				type = NotificationType.STATUS_CHANGED,
				title = "Status changed",
			),
		)

		assertNull(result.body)
		assertNull(result.targetType)
		assertNull(result.targetId)
	}

	@Test
	fun `createdAt is set to approximately now`() {
		val before = Instant.now()
		val result = useCase().create(
			CreateNotificationCommand(
				recipientUserId = recipientId,
				type = NotificationType.TASK_OVERDUE,
				title = "Task overdue",
			),
		)
		val after = Instant.now()

		assertTrue(result.createdAt >= before)
		assertTrue(result.createdAt <= after)
	}

	@Test
	fun `blank title propagates domain validation error`() {
		assertFailsWith<IllegalArgumentException> {
			useCase().create(
				CreateNotificationCommand(
					recipientUserId = recipientId,
					type = NotificationType.TASK_ASSIGNED,
					title = "",
				),
			)
		}
	}

	@Test
	fun `title exceeding 200 chars propagates domain validation error`() {
		assertFailsWith<IllegalArgumentException> {
			useCase().create(
				CreateNotificationCommand(
					recipientUserId = recipientId,
					type = NotificationType.TASK_ASSIGNED,
					title = "x".repeat(201),
				),
			)
		}
	}

	@Test
	fun `targetType without targetId propagates domain validation error`() {
		assertFailsWith<IllegalArgumentException> {
			useCase().create(
				CreateNotificationCommand(
					recipientUserId = recipientId,
					type = NotificationType.TASK_ASSIGNED,
					title = "Task assigned",
					targetType = NotificationTargetType.ORDER,
					targetId = null,
				),
			)
		}
	}
}
