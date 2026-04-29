package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.production.domain.ProductionTask
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

class ProductionTaskQueryUseCaseTests {

	private val orderId = UUID.fromString("10000000-0000-0000-0000-000000000001")
	private val itemId = UUID.fromString("20000000-0000-0000-0000-000000000002")
	private val executorId = UUID.fromString("30000000-0000-0000-0000-000000000003")
	private val supervisorId = UUID.fromString("40000000-0000-0000-0000-000000000004")
	private val now = Instant.parse("2026-04-27T12:00:00Z")

	private val taskAssignedToExecutor = sampleTask(executorUserId = executorId)
	private val taskUnassigned = sampleTask(executorUserId = null, id = UUID.fromString("50000000-0000-0000-0000-000000000005"))

	private val orderSummary = ProductionTaskOrderSummary(
		id = orderId,
		orderNumber = "ORD-1",
		customerDisplayName = "Acme",
		status = com.ctfind.productioncontrol.orders.domain.OrderStatus.IN_WORK,
		deliveryDate = LocalDate.parse("2026-05-01"),
	)

	private val itemSummary = ProductionTaskOrderItemSummary(
		id = itemId,
		lineNo = 1,
		itemName = "Panel",
		quantity = BigDecimal("2"),
		uom = "pcs",
	)

	@Test
	fun `supervisor sees all tasks in list`() {
		val uc = useCase(tasks = listOf(taskAssignedToExecutor, taskUnassigned))
		val actor = AuthenticatedProductionActor(
			userId = supervisorId,
			login = "sup",
			displayName = "S",
			roleCodes = setOf(PRODUCTION_SUPERVISOR_ROLE_CODE),
		)
		val page = uc.list(ProductionTaskListQuery(), actor)
		assertEquals(2, page.items.size)
	}

	@Test
	fun `executor sees only assigned tasks`() {
		val uc = useCase(tasks = listOf(taskAssignedToExecutor, taskUnassigned))
		val actor = AuthenticatedProductionActor(
			userId = executorId,
			login = "ex",
			displayName = "E",
			roleCodes = setOf(PRODUCTION_EXECUTOR_ROLE_CODE),
		)
		val page = uc.list(ProductionTaskListQuery(), actor)
		assertEquals(1, page.items.size)
		assertEquals(taskAssignedToExecutor.taskNumber, page.items.single().taskNumber)
	}

	@Test
	fun `detail forbidden for executor on unassigned task`() {
		val uc = useCase(tasks = listOf(taskUnassigned))
		val actor = AuthenticatedProductionActor(
			userId = executorId,
			login = "ex",
			displayName = "E",
			roleCodes = setOf(PRODUCTION_EXECUTOR_ROLE_CODE),
		)
		val r = uc.detail(taskUnassigned.id, actor)
		assertEquals(ProductionTaskDetailQueryResult.Forbidden, r)
	}

	@Test
	fun `detail found for supervisor`() {
		val uc = useCase(tasks = listOf(taskUnassigned), history = emptyList())
		val actor = AuthenticatedProductionActor(
			userId = supervisorId,
			login = "sup",
			displayName = "S",
			roleCodes = setOf(PRODUCTION_SUPERVISOR_ROLE_CODE),
		)
		val r = uc.detail(taskUnassigned.id, actor)
		val found = assertIs<ProductionTaskDetailQueryResult.Found>(r)
		assertEquals(taskUnassigned.taskNumber, found.detail.row.taskNumber)
	}

	@Test
	fun `detail not found`() {
		val uc = useCase(tasks = emptyList())
		val actor = AuthenticatedProductionActor(
			userId = supervisorId,
			login = "sup",
			displayName = "S",
			roleCodes = setOf(ADMIN_ROLE_CODE),
		)
		assertEquals(ProductionTaskDetailQueryResult.NotFound, uc.detail(UUID.randomUUID(), actor))
	}

	private fun useCase(
		tasks: List<ProductionTask>,
		history: List<ProductionTaskHistoryEvent> = emptyList(),
	): ProductionTaskQueryUseCase {
		val taskMap = tasks.associateBy { it.id }
		val oid = orderId
		val iid = itemId
		return ProductionTaskQueryUseCase(
			tasks = object : ProductionTaskPort {
				override fun findById(id: UUID): ProductionTask? = taskMap[id]
				override fun save(task: ProductionTask): ProductionTask = task
				override fun search(
					query: ProductionTaskListQuery,
					currentUserId: UUID?,
					roleCodes: Set<String>,
				): ProductionTaskPageResult<ProductionTask> {
					val visible = tasks.filter { t ->
						canViewAllProductionTasks(roleCodes) ||
							(currentUserId != null && t.executorUserId == currentUserId)
					}
					return ProductionTaskPageResult(
						items = visible,
						page = query.page,
						size = query.size,
						totalItems = visible.size.toLong(),
					)
				}
				override fun existsByOrderItemIdAndPurpose(orderItemId: UUID, purpose: String): Boolean = false
				override fun findOverdue(today: java.time.LocalDate): List<ProductionTask> = emptyList()
			},
			orderSource = object : ProductionOrderSourcePort {
				override fun findOrderSource(oId: UUID) = orderSummary.takeIf { oId == oid }
				override fun findOrderItemSource(oId: UUID, orderItemId: UUID) =
					itemSummary.takeIf { oId == oid && orderItemId == iid }
			},
			executors = object : ProductionExecutorPort {
				override fun findExecutor(id: UUID): ProductionTaskExecutorSummary? =
					ProductionTaskExecutorSummary(id, "Exec", "exec").takeIf { id == executorId }
				override fun searchExecutors(search: String?, limit: Int): List<ProductionTaskExecutorSummary> = emptyList()
			},
			history = ProductionTaskHistoryUseCase(
				traces = object : ProductionTaskTracePort {
					override fun saveHistoryEvent(event: ProductionTaskHistoryEvent): ProductionTaskHistoryEvent = event
					override fun findHistoryEvents(taskId: UUID): List<ProductionTaskHistoryEvent> =
						history.filter { it.taskId == taskId }
				},
				actorLookup = object : ProductionActorLookupPort {
					override fun displayName(userId: UUID): String? =
						when (userId) {
							supervisorId -> "Supervisor"
							else -> userId.toString()
						}
				},
			),
		)
	}

	private fun sampleTask(
		executorUserId: UUID?,
		id: UUID = UUID.fromString("60000000-0000-0000-0000-000000000006"),
	): ProductionTask =
		ProductionTask(
			id = id,
			taskNumber = "PT-000001",
			orderId = orderId,
			orderItemId = itemId,
			purpose = "Cut",
			itemName = "Panel",
			quantity = BigDecimal.ONE,
			uom = "pcs",
			status = ProductionTaskStatus.NOT_STARTED,
			previousActiveStatus = null,
			executorUserId = executorUserId,
			plannedStartDate = null,
			plannedFinishDate = null,
			blockedReason = null,
			createdByUserId = supervisorId,
			createdAt = now,
			updatedAt = now,
			version = 0,
		)
}