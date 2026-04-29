package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.notifications.application.CreateNotificationCommand
import com.ctfind.productioncontrol.notifications.application.NotificationCreatePort
import com.ctfind.productioncontrol.notifications.domain.Notification
import com.ctfind.productioncontrol.notifications.domain.NotificationType
import com.ctfind.productioncontrol.production.domain.ProductionTask
import com.ctfind.productioncontrol.production.domain.ProductionTaskAuditEvent
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEvent
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChangeProductionTaskStatusNotificationTests {

	private val taskId = UUID.fromString("60000000-0000-0000-0000-000000000006")
	private val orderId = UUID.fromString("10000000-0000-0000-0000-000000000001")
	private val itemId = UUID.fromString("20000000-0000-0000-0000-000000000002")
	private val creatorId = UUID.fromString("70000000-0000-0000-0000-000000000007")
	private val executorId = UUID.fromString("80000000-0000-0000-0000-000000000008")

	@Test
	fun `notification sent to creator on status change by executor`() {
		val notifications = mutableListOf<CreateNotificationCommand>()
		val uc = useCase(
			initialTask = sampleTask(status = ProductionTaskStatus.NOT_STARTED, executorUserId = executorId),
			onNotification = { notifications += it },
		)

		uc.execute(command(actorUserId = executorId, toStatus = ProductionTaskStatus.IN_PROGRESS))

		assertEquals(1, notifications.size)
		val n = notifications.single()
		assertEquals(creatorId, n.recipientUserId)
		assertEquals(NotificationType.STATUS_CHANGED, n.type)
		assertTrue(n.title.contains("PT-000001"))
		assertTrue(n.title.contains("IN_PROGRESS"))
	}

	@Test
	fun `self-notification suppressed when actor is creator`() {
		val notifications = mutableListOf<CreateNotificationCommand>()
		val uc = useCase(
			initialTask = sampleTask(status = ProductionTaskStatus.NOT_STARTED, executorUserId = creatorId),
			onNotification = { notifications += it },
		)

		uc.execute(command(actorUserId = creatorId, toStatus = ProductionTaskStatus.IN_PROGRESS))

		assertEquals(0, notifications.size)
	}

	@Test
	fun `BLOCKED status generates notification`() {
		val notifications = mutableListOf<CreateNotificationCommand>()
		val uc = useCase(
			initialTask = sampleTask(status = ProductionTaskStatus.IN_PROGRESS, executorUserId = executorId),
			onNotification = { notifications += it },
		)

		uc.execute(command(actorUserId = executorId, toStatus = ProductionTaskStatus.BLOCKED, reason = "Нет материала"))

		assertEquals(1, notifications.size)
		assertTrue(notifications.single().title.contains("BLOCKED"))
	}

	@Test
	fun `COMPLETED status generates notification`() {
		val notifications = mutableListOf<CreateNotificationCommand>()
		val uc = useCase(
			initialTask = sampleTask(status = ProductionTaskStatus.IN_PROGRESS, executorUserId = executorId),
			onNotification = { notifications += it },
		)

		uc.execute(command(actorUserId = executorId, toStatus = ProductionTaskStatus.COMPLETED))

		assertEquals(1, notifications.size)
		assertTrue(notifications.single().title.contains("COMPLETED"))
	}

	@Test
	fun `exception in notification does not fail use-case`() {
		val uc = useCase(
			initialTask = sampleTask(status = ProductionTaskStatus.NOT_STARTED, executorUserId = executorId),
			onNotification = { throw RuntimeException("notification service down") },
		)

		val result = uc.execute(command(actorUserId = executorId, toStatus = ProductionTaskStatus.IN_PROGRESS))

		assertIs<ProductionTaskMutationResult.Success<Unit>>(result)
	}

	private fun command(
		actorUserId: UUID = executorId,
		toStatus: ProductionTaskStatus = ProductionTaskStatus.IN_PROGRESS,
		reason: String? = null,
	): ChangeProductionTaskStatusCommand =
		ChangeProductionTaskStatusCommand(
			taskId = taskId,
			expectedVersion = 0,
			toStatus = toStatus,
			reason = reason,
			note = null,
			actorUserId = actorUserId,
			roleCodes = setOf(ADMIN_ROLE_CODE),
		)

	private fun sampleTask(
		status: ProductionTaskStatus = ProductionTaskStatus.NOT_STARTED,
		executorUserId: UUID? = null,
	): ProductionTask =
		ProductionTask(
			id = taskId,
			taskNumber = "PT-000001",
			orderId = orderId,
			orderItemId = itemId,
			purpose = "Раскрой",
			itemName = "Столешница",
			quantity = BigDecimal("2"),
			uom = "шт",
			status = status,
			executorUserId = executorUserId,
			createdByUserId = creatorId,
			createdAt = Instant.parse("2026-04-27T10:00:00Z"),
			updatedAt = Instant.parse("2026-04-27T10:00:00Z"),
		)

	private fun useCase(
		initialTask: ProductionTask?,
		onNotification: (CreateNotificationCommand) -> Unit = {},
	): ChangeProductionTaskStatusUseCase {
		val store = mutableListOf<ProductionTask>().apply {
			if (initialTask != null) add(initialTask)
		}
		val auditPort = object : ProductionTaskAuditPort {
			override fun record(event: ProductionTaskAuditEvent) = event
		}
		return ChangeProductionTaskStatusUseCase(
			tasks = object : ProductionTaskPort {
				override fun findById(id: UUID) = store.find { it.id == id }
				override fun save(task: ProductionTask): ProductionTask {
					store.removeIf { it.id == task.id }
					store.add(task)
					return task
				}
				override fun search(query: ProductionTaskListQuery, currentUserId: UUID?, roleCodes: Set<String>) =
					ProductionTaskPageResult<ProductionTask>(emptyList(), 0, 20, 0)
				override fun existsByOrderItemIdAndPurpose(orderItemId: UUID, purpose: String) = false
				override fun findOverdue(today: LocalDate) = emptyList<ProductionTask>()
			},
			traces = object : ProductionTaskTracePort {
				override fun saveHistoryEvent(event: ProductionTaskHistoryEvent) = event
				override fun findHistoryEvents(taskId: UUID) = emptyList<ProductionTaskHistoryEvent>()
			},
			audit = ProductionTaskAuditService(auditPort),
			notifications = object : NotificationCreatePort {
				override fun create(command: CreateNotificationCommand): Notification {
					onNotification(command)
					return Notification(
						recipientUserId = command.recipientUserId,
						type = command.type,
						title = command.title,
						body = command.body,
						targetType = command.targetType,
						targetId = command.targetId,
						createdAt = Instant.now(),
					)
				}
			},
		)
	}
}
