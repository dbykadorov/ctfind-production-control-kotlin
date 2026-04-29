package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.notifications.application.CreateNotificationCommand
import com.ctfind.productioncontrol.notifications.application.NotificationCreatePort
import com.ctfind.productioncontrol.notifications.domain.Notification
import com.ctfind.productioncontrol.notifications.domain.NotificationTargetType
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

class AssignProductionTaskNotificationTests {

	private val taskId = UUID.fromString("60000000-0000-0000-0000-000000000006")
	private val orderId = UUID.fromString("10000000-0000-0000-0000-000000000001")
	private val itemId = UUID.fromString("20000000-0000-0000-0000-000000000002")
	private val supervisorId = UUID.fromString("70000000-0000-0000-0000-000000000007")
	private val executorId = UUID.fromString("80000000-0000-0000-0000-000000000008")
	private val otherExecutorId = UUID.fromString("90000000-0000-0000-0000-000000000009")

	@Test
	fun `notification created when executor changes`() {
		val notifications = mutableListOf<CreateNotificationCommand>()
		val uc = useCase(
			initialTask = sampleTask(executorUserId = null),
			onNotification = { notifications += it },
		)

		uc.execute(command(executorUserId = executorId))

		assertEquals(1, notifications.size)
		val n = notifications.single()
		assertEquals(executorId, n.recipientUserId)
		assertEquals(NotificationType.TASK_ASSIGNED, n.type)
		assertEquals(NotificationTargetType.PRODUCTION_TASK, n.targetType)
		assertEquals("PT-000001", n.targetId)
		assertTrue(n.title.contains("PT-000001"))
	}

	@Test
	fun `notification NOT created when executor unchanged`() {
		val notifications = mutableListOf<CreateNotificationCommand>()
		val uc = useCase(
			initialTask = sampleTask(executorUserId = executorId, plannedFinishDate = LocalDate.parse("2026-05-01")),
			onNotification = { notifications += it },
		)

		uc.execute(command(executorUserId = executorId, plannedFinishDate = LocalDate.parse("2026-05-05")))

		assertEquals(0, notifications.size)
	}

	@Test
	fun `notification NOT created on no-op`() {
		val notifications = mutableListOf<CreateNotificationCommand>()
		val uc = useCase(
			initialTask = sampleTask(executorUserId = executorId),
			onNotification = { notifications += it },
		)

		uc.execute(command(executorUserId = executorId))

		assertEquals(0, notifications.size)
	}

	@Test
	fun `notification on reassignment goes to new executor`() {
		val notifications = mutableListOf<CreateNotificationCommand>()
		val uc = useCase(
			initialTask = sampleTask(executorUserId = otherExecutorId),
			onNotification = { notifications += it },
		)

		uc.execute(command(executorUserId = executorId))

		assertEquals(1, notifications.size)
		assertEquals(executorId, notifications.single().recipientUserId)
	}

	@Test
	fun `exception in notification does not fail use-case`() {
		val uc = useCase(
			initialTask = sampleTask(executorUserId = null),
			onNotification = { throw RuntimeException("notification service down") },
		)

		val result = uc.execute(command(executorUserId = executorId))

		assertIs<ProductionTaskMutationResult.Success<Unit>>(result)
	}

	private fun command(
		executorUserId: UUID = this.executorId,
		plannedStartDate: LocalDate? = null,
		plannedFinishDate: LocalDate? = null,
	): AssignProductionTaskCommand =
		AssignProductionTaskCommand(
			taskId = taskId,
			expectedVersion = 0,
			executorUserId = executorUserId,
			plannedStartDate = plannedStartDate,
			plannedFinishDate = plannedFinishDate,
			note = null,
			actorUserId = supervisorId,
			roleCodes = setOf(ADMIN_ROLE_CODE),
		)

	private fun sampleTask(
		executorUserId: UUID? = null,
		plannedFinishDate: LocalDate? = null,
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
			status = ProductionTaskStatus.NOT_STARTED,
			executorUserId = executorUserId,
			plannedFinishDate = plannedFinishDate,
			createdByUserId = supervisorId,
			createdAt = Instant.parse("2026-04-27T10:00:00Z"),
			updatedAt = Instant.parse("2026-04-27T10:00:00Z"),
		)

	private fun useCase(
		initialTask: ProductionTask?,
		onNotification: (CreateNotificationCommand) -> Unit = {},
	): AssignProductionTaskUseCase {
		val store = mutableListOf<ProductionTask>().apply {
			if (initialTask != null) add(initialTask)
		}
		val auditPort = object : ProductionTaskAuditPort {
			override fun record(event: ProductionTaskAuditEvent) = event
		}
		return AssignProductionTaskUseCase(
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
			executors = object : ProductionExecutorPort {
				override fun findExecutor(id: UUID) =
					if (id == executorId || id == otherExecutorId) ProductionTaskExecutorSummary(id, "Exec-$id", "exec-$id") else null
				override fun searchExecutors(search: String?, limit: Int) = emptyList<ProductionTaskExecutorSummary>()
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
