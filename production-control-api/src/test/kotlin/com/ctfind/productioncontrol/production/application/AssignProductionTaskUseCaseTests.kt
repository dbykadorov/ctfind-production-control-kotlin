package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.production.domain.ProductionTask
import com.ctfind.productioncontrol.production.domain.ProductionTaskAuditEvent
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEvent
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEventType
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AssignProductionTaskUseCaseTests {

	private val taskId = UUID.fromString("60000000-0000-0000-0000-000000000006")
	private val orderId = UUID.fromString("10000000-0000-0000-0000-000000000001")
	private val itemId = UUID.fromString("20000000-0000-0000-0000-000000000002")
	private val supervisorId = UUID.fromString("70000000-0000-0000-0000-000000000007")
	private val executorId = UUID.fromString("80000000-0000-0000-0000-000000000008")
	private val otherExecutorId = UUID.fromString("90000000-0000-0000-0000-000000000009")

	@Test
	fun `forbidden when role cannot assign`() {
		val uc = useCase(initialTask = sampleTask())
		val r = uc.execute(command(roleCodes = setOf("VIEWER_ONLY")))
		assertEquals(ProductionTaskMutationResult.Forbidden, r)
	}

	@Test
	fun `not found when task is missing`() {
		val uc = useCase(initialTask = null)
		val r = uc.execute(command())
		assertEquals(ProductionTaskMutationResult.NotFound, r)
	}

	@Test
	fun `completed task cannot be reassigned`() {
		val uc = useCase(initialTask = sampleTask(status = ProductionTaskStatus.COMPLETED))
		val r = uc.execute(command())
		val v = assertIs<ProductionTaskMutationResult.ValidationFailed>(r)
		assertTrue(v.message.contains("Completed", ignoreCase = true))
	}

	@Test
	fun `planned finish before start fails validation`() {
		val uc = useCase(initialTask = sampleTask())
		val r = uc.execute(
			command(
				plannedStartDate = LocalDate.parse("2026-05-05"),
				plannedFinishDate = LocalDate.parse("2026-05-01"),
			),
		)
		val v = assertIs<ProductionTaskMutationResult.ValidationFailed>(r)
		assertTrue(v.message.contains("Planned finish", ignoreCase = true))
	}

	@Test
	fun `stale version returns conflict`() {
		val uc = useCase(initialTask = sampleTask(version = 5))
		val r = uc.execute(command(expectedVersion = 4))
		assertEquals(ProductionTaskMutationResult.StaleVersion, r)
	}

	@Test
	fun `invalid executor fails validation`() {
		val uc = useCase(initialTask = sampleTask(), validExecutorIds = emptySet())
		val r = uc.execute(command(executorUserId = executorId))
		val v = assertIs<ProductionTaskMutationResult.ValidationFailed>(r)
		assertTrue(v.message.contains("Executor", ignoreCase = true))
	}

	@Test
	fun `initial assignment writes ASSIGNED history and audit once`() {
		val historyCaptured = mutableListOf<ProductionTaskHistoryEvent>()
		val auditCaptured = mutableListOf<ProductionTaskAuditEvent>()
		val uc = useCase(
			initialTask = sampleTask(executorUserId = null),
			onHistory = { historyCaptured += it },
			onAudit = { auditCaptured += it },
		)

		val r = uc.execute(
			command(
				executorUserId = executorId,
				plannedStartDate = LocalDate.parse("2026-05-01"),
				plannedFinishDate = LocalDate.parse("2026-05-03"),
			),
		)

		assertIs<ProductionTaskMutationResult.Success<Unit>>(r)
		assertEquals(1, historyCaptured.size)
		val event = historyCaptured.single()
		assertEquals(ProductionTaskHistoryEventType.ASSIGNED, event.eventType)
		assertEquals(null, event.previousExecutorUserId)
		assertEquals(executorId, event.newExecutorUserId)
		assertEquals(LocalDate.parse("2026-05-01"), event.plannedStartDateAfter)
		assertEquals(LocalDate.parse("2026-05-03"), event.plannedFinishDateAfter)
		assertEquals(1, auditCaptured.size)
		assertEquals("PRODUCTION_TASK_ASSIGNED", auditCaptured.single().eventType)
	}

	@Test
	fun `reassignment captures previous and new executor in history`() {
		val historyCaptured = mutableListOf<ProductionTaskHistoryEvent>()
		val uc = useCase(
			initialTask = sampleTask(executorUserId = otherExecutorId),
			validExecutorIds = setOf(executorId, otherExecutorId),
			onHistory = { historyCaptured += it },
		)

		val r = uc.execute(command(executorUserId = executorId))

		assertIs<ProductionTaskMutationResult.Success<Unit>>(r)
		val event = historyCaptured.single()
		assertEquals(ProductionTaskHistoryEventType.ASSIGNED, event.eventType)
		assertEquals(otherExecutorId, event.previousExecutorUserId)
		assertEquals(executorId, event.newExecutorUserId)
	}

	@Test
	fun `planning-only update writes PLANNING_UPDATED event without ASSIGNED`() {
		val historyCaptured = mutableListOf<ProductionTaskHistoryEvent>()
		val uc = useCase(
			initialTask = sampleTask(executorUserId = executorId, plannedFinishDate = LocalDate.parse("2026-05-03")),
			onHistory = { historyCaptured += it },
		)

		val r = uc.execute(
			command(
				executorUserId = executorId,
				plannedFinishDate = LocalDate.parse("2026-05-05"),
			),
		)

		assertIs<ProductionTaskMutationResult.Success<Unit>>(r)
		val event = historyCaptured.single()
		assertEquals(ProductionTaskHistoryEventType.PLANNING_UPDATED, event.eventType)
		assertEquals(LocalDate.parse("2026-05-03"), event.plannedFinishDateBefore)
		assertEquals(LocalDate.parse("2026-05-05"), event.plannedFinishDateAfter)
	}

	@Test
	fun `no-op when nothing changed and note is blank`() {
		val historyCaptured = mutableListOf<ProductionTaskHistoryEvent>()
		val auditCaptured = mutableListOf<ProductionTaskAuditEvent>()
		val saveCount = intArrayOf(0)
		val uc = useCase(
			initialTask = sampleTask(executorUserId = executorId),
			onHistory = { historyCaptured += it },
			onAudit = { auditCaptured += it },
			onSave = { saveCount[0] += 1 },
		)

		val r = uc.execute(command(executorUserId = executorId))

		assertIs<ProductionTaskMutationResult.Success<Unit>>(r)
		assertEquals(0, saveCount[0])
		assertEquals(0, historyCaptured.size)
		assertEquals(0, auditCaptured.size)
	}

	private fun command(
		executorUserId: UUID = this.executorId,
		expectedVersion: Long = 0,
		plannedStartDate: LocalDate? = null,
		plannedFinishDate: LocalDate? = null,
		note: String? = null,
		roleCodes: Set<String> = setOf(ADMIN_ROLE_CODE),
	): AssignProductionTaskCommand =
		AssignProductionTaskCommand(
			taskId = taskId,
			expectedVersion = expectedVersion,
			executorUserId = executorUserId,
			plannedStartDate = plannedStartDate,
			plannedFinishDate = plannedFinishDate,
			note = note,
			actorUserId = supervisorId,
			roleCodes = roleCodes,
		)

	private fun sampleTask(
		status: ProductionTaskStatus = ProductionTaskStatus.NOT_STARTED,
		executorUserId: UUID? = null,
		plannedStartDate: LocalDate? = null,
		plannedFinishDate: LocalDate? = null,
		version: Long = 0,
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
			plannedStartDate = plannedStartDate,
			plannedFinishDate = plannedFinishDate,
			createdByUserId = supervisorId,
			createdAt = Instant.parse("2026-04-27T10:00:00Z"),
			updatedAt = Instant.parse("2026-04-27T10:00:00Z"),
			version = version,
		)

	private fun useCase(
		initialTask: ProductionTask?,
		validExecutorIds: Set<UUID> = setOf(executorId, otherExecutorId),
		onHistory: (ProductionTaskHistoryEvent) -> Unit = {},
		onAudit: (ProductionTaskAuditEvent) -> Unit = {},
		onSave: (ProductionTask) -> Unit = {},
	): AssignProductionTaskUseCase {
		val store = mutableListOf<ProductionTask>().apply {
			if (initialTask != null) add(initialTask)
		}
		val auditPort = object : ProductionTaskAuditPort {
			override fun record(event: ProductionTaskAuditEvent): ProductionTaskAuditEvent {
				onAudit(event)
				return event
			}
		}
		return AssignProductionTaskUseCase(
			tasks = object : ProductionTaskPort {
				override fun findById(id: UUID): ProductionTask? = store.find { it.id == id }
				override fun save(task: ProductionTask): ProductionTask {
					onSave(task)
					store.removeIf { it.id == task.id }
					store.add(task)
					return task
				}

				override fun search(
					query: ProductionTaskListQuery,
					currentUserId: UUID?,
					roleCodes: Set<String>,
				): ProductionTaskPageResult<ProductionTask> = ProductionTaskPageResult(emptyList(), 0, 20, 0)

				override fun existsByOrderItemIdAndPurpose(orderItemId: UUID, purpose: String): Boolean = false
				override fun findOverdue(today: java.time.LocalDate): List<ProductionTask> = emptyList()
			},
			executors = object : ProductionExecutorPort {
				override fun findExecutor(id: UUID): ProductionTaskExecutorSummary? =
					if (id in validExecutorIds) ProductionTaskExecutorSummary(id, "Exec-$id", "exec-$id") else null

				override fun searchExecutors(search: String?, limit: Int): List<ProductionTaskExecutorSummary> = emptyList()
			},
			traces = object : ProductionTaskTracePort {
				override fun saveHistoryEvent(event: ProductionTaskHistoryEvent): ProductionTaskHistoryEvent {
					onHistory(event)
					return event
				}

				override fun findHistoryEvents(taskId: UUID): List<ProductionTaskHistoryEvent> = emptyList()
			},
			audit = ProductionTaskAuditService(auditPort),
			notifications = object : com.ctfind.productioncontrol.notifications.application.NotificationCreatePort {
				override fun create(command: com.ctfind.productioncontrol.notifications.application.CreateNotificationCommand) =
					com.ctfind.productioncontrol.notifications.domain.Notification(
						recipientUserId = command.recipientUserId,
						type = command.type,
						title = command.title,
						createdAt = Instant.now(),
					)
			},
		)
	}
}
