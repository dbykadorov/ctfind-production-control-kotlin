package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.notifications.application.CreateNotificationCommand
import com.ctfind.productioncontrol.notifications.application.NotificationCreatePort
import com.ctfind.productioncontrol.notifications.application.NotificationPersistencePort
import com.ctfind.productioncontrol.notifications.application.NotificationListQuery
import com.ctfind.productioncontrol.notifications.application.NotificationPageResult
import com.ctfind.productioncontrol.notifications.domain.Notification
import com.ctfind.productioncontrol.notifications.domain.NotificationTargetType
import com.ctfind.productioncontrol.notifications.domain.NotificationType
import com.ctfind.productioncontrol.production.domain.ProductionTask
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OverdueTaskNotificationJobTests {

	private val creatorId = UUID.fromString("70000000-0000-0000-0000-000000000007")
	private val executorId = UUID.fromString("80000000-0000-0000-0000-000000000008")
	private val today = LocalDate.parse("2026-05-01")

	@Test
	fun `overdue task sends notification to executor and creator`() {
		val notifications = mutableListOf<CreateNotificationCommand>()
		val task = sampleTask(
			executorUserId = executorId,
			plannedFinishDate = LocalDate.parse("2026-04-30"),
			status = ProductionTaskStatus.IN_PROGRESS,
		)
		val job = job(overdueTasks = listOf(task), onNotification = { notifications += it })

		job.checkOverdueTasks(today)

		assertEquals(2, notifications.size)
		val recipients = notifications.map { it.recipientUserId }.toSet()
		assertEquals(setOf(executorId, creatorId), recipients)
		notifications.forEach {
			assertEquals(NotificationType.TASK_OVERDUE, it.type)
			assertEquals(NotificationTargetType.PRODUCTION_TASK, it.targetType)
			assertTrue(it.title.contains("PT-000001"))
		}
	}

	@Test
	fun `completed task is skipped`() {
		val notifications = mutableListOf<CreateNotificationCommand>()
		val task = sampleTask(
			executorUserId = executorId,
			plannedFinishDate = LocalDate.parse("2026-04-30"),
			status = ProductionTaskStatus.COMPLETED,
		)
		val job = job(overdueTasks = listOf(task), onNotification = { notifications += it })

		job.checkOverdueTasks(today)

		assertEquals(0, notifications.size)
	}

	@Test
	fun `duplicate notification prevented`() {
		val notifications = mutableListOf<CreateNotificationCommand>()
		val task = sampleTask(
			executorUserId = executorId,
			plannedFinishDate = LocalDate.parse("2026-04-30"),
			status = ProductionTaskStatus.IN_PROGRESS,
		)
		val alreadySent = mutableSetOf<Pair<UUID, String>>()
		alreadySent += executorId to "PT-000001"
		alreadySent += creatorId to "PT-000001"

		val job = job(
			overdueTasks = listOf(task),
			onNotification = { notifications += it },
			existingOverdueNotifications = alreadySent,
		)

		job.checkOverdueTasks(today)

		assertEquals(0, notifications.size)
	}

	@Test
	fun `task without executor sends only to creator`() {
		val notifications = mutableListOf<CreateNotificationCommand>()
		val task = sampleTask(
			executorUserId = null,
			plannedFinishDate = LocalDate.parse("2026-04-30"),
			status = ProductionTaskStatus.NOT_STARTED,
		)
		val job = job(overdueTasks = listOf(task), onNotification = { notifications += it })

		job.checkOverdueTasks(today)

		assertEquals(1, notifications.size)
		assertEquals(creatorId, notifications.single().recipientUserId)
	}

	@Test
	fun `exception in one notification does not stop processing others`() {
		val notifications = mutableListOf<CreateNotificationCommand>()
		var callCount = 0
		val task = sampleTask(
			executorUserId = executorId,
			plannedFinishDate = LocalDate.parse("2026-04-30"),
			status = ProductionTaskStatus.IN_PROGRESS,
		)
		val job = job(
			overdueTasks = listOf(task),
			onNotification = {
				callCount++
				if (callCount == 1) throw RuntimeException("first notification fails")
				notifications += it
			},
		)

		job.checkOverdueTasks(today)

		assertEquals(1, notifications.size)
	}

	@Test
	fun `executor equals creator sends only one notification`() {
		val notifications = mutableListOf<CreateNotificationCommand>()
		val task = sampleTask(
			executorUserId = creatorId,
			plannedFinishDate = LocalDate.parse("2026-04-30"),
			status = ProductionTaskStatus.IN_PROGRESS,
		)
		val job = job(overdueTasks = listOf(task), onNotification = { notifications += it })

		job.checkOverdueTasks(today)

		assertEquals(1, notifications.size)
		assertEquals(creatorId, notifications.single().recipientUserId)
	}

	private fun sampleTask(
		executorUserId: UUID? = null,
		plannedFinishDate: LocalDate? = null,
		status: ProductionTaskStatus = ProductionTaskStatus.NOT_STARTED,
	): ProductionTask =
		ProductionTask(
			id = UUID.randomUUID(),
			taskNumber = "PT-000001",
			orderId = UUID.fromString("10000000-0000-0000-0000-000000000001"),
			orderItemId = UUID.fromString("20000000-0000-0000-0000-000000000002"),
			purpose = "Раскрой",
			itemName = "Столешница",
			quantity = BigDecimal("2"),
			uom = "шт",
			status = status,
			executorUserId = executorUserId,
			plannedFinishDate = plannedFinishDate,
			createdByUserId = creatorId,
			createdAt = Instant.parse("2026-04-27T10:00:00Z"),
			updatedAt = Instant.parse("2026-04-27T10:00:00Z"),
		)

	private fun job(
		overdueTasks: List<ProductionTask> = emptyList(),
		onNotification: (CreateNotificationCommand) -> Unit = {},
		existingOverdueNotifications: Set<Pair<UUID, String>> = emptySet(),
	): OverdueTaskNotificationJob {
		val tasks = object : ProductionTaskPort {
			override fun findById(id: UUID): ProductionTask? = null
			override fun save(task: ProductionTask) = task
			override fun search(query: ProductionTaskListQuery, currentUserId: UUID?, roleCodes: Set<String>) =
				ProductionTaskPageResult<ProductionTask>(emptyList(), 0, 20, 0)
			override fun existsByOrderItemIdAndPurpose(orderItemId: UUID, purpose: String) = false
			override fun findOverdue(today: LocalDate): List<ProductionTask> =
				overdueTasks.filter { it.status != ProductionTaskStatus.COMPLETED }
		}
		val notifications = object : NotificationCreatePort {
			override fun create(command: CreateNotificationCommand): Notification {
				onNotification(command)
				return Notification(
					recipientUserId = command.recipientUserId,
					type = command.type,
					title = command.title,
					body = command.body,
					targetType = command.targetType,
					targetId = command.targetId,
					targetEntityId = command.targetEntityId,
					createdAt = Instant.now(),
				)
			}
		}
		val notificationQuery = object : NotificationPersistencePort {
			override fun save(notification: Notification) = notification
			override fun findById(id: UUID): Notification? = null
			override fun findByRecipientUserId(query: NotificationListQuery) =
				NotificationPageResult<Notification>(emptyList(), 0, 20, 0)
			override fun countUnreadByRecipientUserId(recipientUserId: UUID) = 0L
			override fun markAllReadByRecipientUserId(recipientUserId: UUID, readAt: Instant) = 0
			override fun existsByTypeAndTargetIdAndRecipient(type: NotificationType, targetId: String, recipientUserId: UUID) =
				(recipientUserId to targetId) in existingOverdueNotifications
		}
		return OverdueTaskNotificationJob(tasks, notifications, notificationQuery)
	}
}
