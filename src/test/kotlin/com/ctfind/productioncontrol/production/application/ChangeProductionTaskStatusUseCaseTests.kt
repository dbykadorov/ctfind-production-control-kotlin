package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.production.domain.ProductionTask
import com.ctfind.productioncontrol.production.domain.ProductionTaskAuditEvent
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEvent
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEventType
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChangeProductionTaskStatusUseCaseTests {

	private val taskId = UUID.fromString("60000000-0000-0000-0000-000000000006")
	private val orderId = UUID.fromString("10000000-0000-0000-0000-000000000001")
	private val itemId = UUID.fromString("20000000-0000-0000-0000-000000000002")
	private val supervisorId = UUID.fromString("70000000-0000-0000-0000-000000000007")
	private val executorId = UUID.fromString("80000000-0000-0000-0000-000000000008")
	private val otherExecutorId = UUID.fromString("90000000-0000-0000-0000-000000000009")

	@Test
	fun `supervisor can update any task status`() {
		val historyCaptured = mutableListOf<ProductionTaskHistoryEvent>()
		val auditCaptured = mutableListOf<ProductionTaskAuditEvent>()
		val uc = useCase(
			initialTask = sampleTask(status = ProductionTaskStatus.NOT_STARTED, executorUserId = executorId),
			onHistory = { historyCaptured += it },
			onAudit = { auditCaptured += it },
		)

		val r = uc.execute(
			command(toStatus = ProductionTaskStatus.IN_PROGRESS, roleCodes = setOf(PRODUCTION_SUPERVISOR_ROLE_CODE)),
		)

		assertIs<ProductionTaskMutationResult.Success<Unit>>(r)
		assertEquals(ProductionTaskHistoryEventType.STATUS_CHANGED, historyCaptured.single().eventType)
		assertEquals("PRODUCTION_TASK_STATUS_IN_PROGRESS", auditCaptured.single().eventType)
	}

	@Test
	fun `executor can update only assigned task`() {
		val ucMine = useCase(
			initialTask = sampleTask(status = ProductionTaskStatus.NOT_STARTED, executorUserId = executorId),
		)
		val rMine = ucMine.execute(
			command(
				toStatus = ProductionTaskStatus.IN_PROGRESS,
				actorUserId = executorId,
				roleCodes = setOf(PRODUCTION_EXECUTOR_ROLE_CODE),
			),
		)
		assertIs<ProductionTaskMutationResult.Success<Unit>>(rMine)

		val ucOther = useCase(
			initialTask = sampleTask(status = ProductionTaskStatus.NOT_STARTED, executorUserId = otherExecutorId),
		)
		val rOther = ucOther.execute(
			command(
				toStatus = ProductionTaskStatus.IN_PROGRESS,
				actorUserId = executorId,
				roleCodes = setOf(PRODUCTION_EXECUTOR_ROLE_CODE),
			),
		)
		assertEquals(ProductionTaskMutationResult.Forbidden, rOther)
	}

	@Test
	fun `completed task rejects status change`() {
		val uc = useCase(initialTask = sampleTask(status = ProductionTaskStatus.COMPLETED, executorUserId = executorId))
		val r = uc.execute(command(toStatus = ProductionTaskStatus.IN_PROGRESS))
		assertIs<ProductionTaskMutationResult.ValidationFailed>(r)
	}

	@Test
	fun `block requires reason`() {
		val uc = useCase(initialTask = sampleTask(status = ProductionTaskStatus.IN_PROGRESS, executorUserId = executorId))
		val r = uc.execute(command(toStatus = ProductionTaskStatus.BLOCKED, reason = null))
		val v = assertIs<ProductionTaskMutationResult.ValidationFailed>(r)
		assertTrue(v.message.contains("reason", ignoreCase = true))
	}

	@Test
	fun `block stores previous active status and reason and emits BLOCKED event`() {
		val historyCaptured = mutableListOf<ProductionTaskHistoryEvent>()
		val saved = mutableListOf<ProductionTask>()
		val uc = useCase(
			initialTask = sampleTask(status = ProductionTaskStatus.IN_PROGRESS, executorUserId = executorId),
			onHistory = { historyCaptured += it },
			onSave = { saved += it },
		)

		val r = uc.execute(command(toStatus = ProductionTaskStatus.BLOCKED, reason = "Нет материала"))

		assertIs<ProductionTaskMutationResult.Success<Unit>>(r)
		val updated = saved.last()
		assertEquals(ProductionTaskStatus.BLOCKED, updated.status)
		assertEquals(ProductionTaskStatus.IN_PROGRESS, updated.previousActiveStatus)
		assertEquals("Нет материала", updated.blockedReason)
		assertEquals(ProductionTaskHistoryEventType.BLOCKED, historyCaptured.single().eventType)
		assertEquals("Нет материала", historyCaptured.single().reason)
	}

	@Test
	fun `unblock restores previous active status and clears blocked fields`() {
		val historyCaptured = mutableListOf<ProductionTaskHistoryEvent>()
		val saved = mutableListOf<ProductionTask>()
		val uc = useCase(
			initialTask = sampleTask(
				status = ProductionTaskStatus.BLOCKED,
				previousActiveStatus = ProductionTaskStatus.IN_PROGRESS,
				blockedReason = "Нет материала",
				executorUserId = executorId,
			),
			onHistory = { historyCaptured += it },
			onSave = { saved += it },
		)

		val r = uc.execute(command(toStatus = ProductionTaskStatus.IN_PROGRESS))

		assertIs<ProductionTaskMutationResult.Success<Unit>>(r)
		val updated = saved.last()
		assertEquals(ProductionTaskStatus.IN_PROGRESS, updated.status)
		assertEquals(null, updated.previousActiveStatus)
		assertEquals(null, updated.blockedReason)
		assertEquals(ProductionTaskHistoryEventType.UNBLOCKED, historyCaptured.single().eventType)
	}

	@Test
	fun `unblock to non-previous-status returns invalid transition`() {
		val uc = useCase(
			initialTask = sampleTask(
				status = ProductionTaskStatus.BLOCKED,
				previousActiveStatus = ProductionTaskStatus.NOT_STARTED,
				blockedReason = "X",
				executorUserId = executorId,
			),
		)

		val r = uc.execute(command(toStatus = ProductionTaskStatus.IN_PROGRESS))

		assertEquals(ProductionTaskMutationResult.InvalidTransition, r)
	}

	@Test
	fun `direct NOT_STARTED to COMPLETED is invalid transition`() {
		val uc = useCase(initialTask = sampleTask(status = ProductionTaskStatus.NOT_STARTED, executorUserId = executorId))
		val r = uc.execute(command(toStatus = ProductionTaskStatus.COMPLETED))
		assertEquals(ProductionTaskMutationResult.InvalidTransition, r)
	}

	@Test
	fun `same-status transition is rejected as validation`() {
		val uc = useCase(initialTask = sampleTask(status = ProductionTaskStatus.IN_PROGRESS, executorUserId = executorId))
		val r = uc.execute(command(toStatus = ProductionTaskStatus.IN_PROGRESS))
		assertIs<ProductionTaskMutationResult.ValidationFailed>(r)
	}

	@Test
	fun `stale version rejects status change`() {
		val uc = useCase(initialTask = sampleTask(status = ProductionTaskStatus.NOT_STARTED, executorUserId = executorId, version = 5))
		val r = uc.execute(command(toStatus = ProductionTaskStatus.IN_PROGRESS, expectedVersion = 4))
		assertEquals(ProductionTaskMutationResult.StaleVersion, r)
	}

	@Test
	fun `complete from in_progress emits COMPLETED event`() {
		val historyCaptured = mutableListOf<ProductionTaskHistoryEvent>()
		val auditCaptured = mutableListOf<ProductionTaskAuditEvent>()
		val uc = useCase(
			initialTask = sampleTask(status = ProductionTaskStatus.IN_PROGRESS, executorUserId = executorId),
			onHistory = { historyCaptured += it },
			onAudit = { auditCaptured += it },
		)

		val r = uc.execute(command(toStatus = ProductionTaskStatus.COMPLETED))

		assertIs<ProductionTaskMutationResult.Success<Unit>>(r)
		assertEquals(ProductionTaskHistoryEventType.COMPLETED, historyCaptured.single().eventType)
		assertEquals("PRODUCTION_TASK_STATUS_COMPLETED", auditCaptured.single().eventType)
	}

	private fun command(
		toStatus: ProductionTaskStatus,
		expectedVersion: Long = 0,
		reason: String? = null,
		note: String? = null,
		actorUserId: UUID = supervisorId,
		roleCodes: Set<String> = setOf(ADMIN_ROLE_CODE),
	): ChangeProductionTaskStatusCommand =
		ChangeProductionTaskStatusCommand(
			taskId = taskId,
			expectedVersion = expectedVersion,
			toStatus = toStatus,
			reason = reason,
			note = note,
			actorUserId = actorUserId,
			roleCodes = roleCodes,
		)

	private fun sampleTask(
		status: ProductionTaskStatus,
		executorUserId: UUID?,
		previousActiveStatus: ProductionTaskStatus? = null,
		blockedReason: String? = null,
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
			previousActiveStatus = previousActiveStatus,
			executorUserId = executorUserId,
			blockedReason = blockedReason,
			createdByUserId = supervisorId,
			createdAt = Instant.parse("2026-04-27T10:00:00Z"),
			updatedAt = Instant.parse("2026-04-27T10:00:00Z"),
			version = version,
		)

	private fun useCase(
		initialTask: ProductionTask?,
		onHistory: (ProductionTaskHistoryEvent) -> Unit = {},
		onAudit: (ProductionTaskAuditEvent) -> Unit = {},
		onSave: (ProductionTask) -> Unit = {},
	): ChangeProductionTaskStatusUseCase {
		val store = mutableListOf<ProductionTask>().apply {
			if (initialTask != null) add(initialTask)
		}
		val auditPort = object : ProductionTaskAuditPort {
			override fun record(event: ProductionTaskAuditEvent): ProductionTaskAuditEvent {
				onAudit(event)
				return event
			}
		}
		return ChangeProductionTaskStatusUseCase(
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
			},
			traces = object : ProductionTaskTracePort {
				override fun saveHistoryEvent(event: ProductionTaskHistoryEvent): ProductionTaskHistoryEvent {
					onHistory(event)
					return event
				}

				override fun findHistoryEvents(taskId: UUID): List<ProductionTaskHistoryEvent> = emptyList()
			},
			audit = ProductionTaskAuditService(auditPort),
		)
	}
}
