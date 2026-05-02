package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import com.ctfind.productioncontrol.production.domain.ProductionTask
import com.ctfind.productioncontrol.production.domain.ProductionTaskAuditEvent
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEvent
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEventType
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CreateProductionTasksFromOrderUseCaseTests {

	private val orderId = UUID.fromString("10000000-0000-0000-0000-000000000001")
	private val itemId = UUID.fromString("20000000-0000-0000-0000-000000000002")
	private val actorId = UUID.fromString("30000000-0000-0000-0000-000000000003")

	private val orderSummary = ProductionTaskOrderSummary(
		id = orderId,
		orderNumber = "ORD-1",
		customerDisplayName = "Acme",
		status = OrderStatus.IN_WORK,
		deliveryDate = LocalDate.parse("2026-05-01"),
	)

	private val itemSummary = ProductionTaskOrderItemSummary(
		id = itemId,
		lineNo = 1,
		itemName = "Panel",
		quantity = BigDecimal("10"),
		uom = "pcs",
	)

	@Test
	fun `forbidden when user cannot create`() {
		val uc = useCase()
		val r = uc.execute(
			CreateProductionTasksFromOrderCommand(
				orderId = orderId,
				tasks = listOf(
					CreateProductionTaskDraft(
						orderItemId = itemId,
						purpose = "Cut",
						quantity = BigDecimal.ONE,
						uom = "pcs",
					),
				),
				actorUserId = actorId,
				roleCodes = emptySet(),
			),
		)
		assertEquals(ProductionTaskMutationResult.Forbidden, r)
	}

	@Test
	fun `validation failed on duplicate purpose in same request`() {
		val uc = useCase()
		val r = uc.execute(
			CreateProductionTasksFromOrderCommand(
				orderId = orderId,
				tasks = listOf(
					CreateProductionTaskDraft(orderItemId = itemId, purpose = "Cut", quantity = BigDecimal.ONE, uom = "pcs"),
					CreateProductionTaskDraft(orderItemId = itemId, purpose = "cut", quantity = BigDecimal.ONE, uom = "pcs"),
				),
				actorUserId = actorId,
				roleCodes = setOf(ADMIN_ROLE_CODE),
			),
		)
		val v = assertIs<ProductionTaskMutationResult.ValidationFailed>(r)
		assertTrue(v.message.contains("Duplicate", ignoreCase = true))
	}

	@Test
	fun `validation failed when planned finish is before planned start`() {
		val uc = useCase()
		val r = uc.execute(
			CreateProductionTasksFromOrderCommand(
				orderId = orderId,
				tasks = listOf(
					CreateProductionTaskDraft(
						orderItemId = itemId,
						purpose = "Раскрой",
						quantity = BigDecimal.ONE,
						uom = "pcs",
						plannedStartDate = LocalDate.parse("2026-05-02"),
						plannedFinishDate = LocalDate.parse("2026-05-01"),
					),
				),
				actorUserId = actorId,
				roleCodes = setOf(ADMIN_ROLE_CODE),
			),
		)
		val v = assertIs<ProductionTaskMutationResult.ValidationFailed>(r)
		assertTrue(v.message.contains("Planned finish", ignoreCase = true))
	}

	@Test
	fun `validation does not write partial data when one of drafts is invalid`() {
		var saveCount = 0
		val uc = useCase(
			onSave = { saveCount += 1 },
		)
		val r = uc.execute(
			CreateProductionTasksFromOrderCommand(
				orderId = orderId,
				tasks = listOf(
					CreateProductionTaskDraft(orderItemId = itemId, purpose = "Cut", quantity = BigDecimal.ONE, uom = "pcs"),
					CreateProductionTaskDraft(orderItemId = itemId, purpose = "Assemble", quantity = BigDecimal.ZERO, uom = "pcs"),
				),
				actorUserId = actorId,
				roleCodes = setOf(ADMIN_ROLE_CODE),
			),
		)
		assertIs<ProductionTaskMutationResult.ValidationFailed>(r)
		assertEquals(0, saveCount)
	}

	@Test
	fun `success creates task with history and audit`() {
		var historyType: ProductionTaskHistoryEventType? = null
		var auditCount = 0
		val uc = useCase(
			onHistory = { e -> historyType = e.eventType; e },
			onAudit = { auditCount++ },
		)
		val r = uc.execute(
			CreateProductionTasksFromOrderCommand(
				orderId = orderId,
				tasks = listOf(
					CreateProductionTaskDraft(
						orderItemId = itemId,
						purpose = "Раскрой",
						quantity = BigDecimal("2"),
						uom = "pcs",
					),
				),
				actorUserId = actorId,
				roleCodes = setOf(ADMIN_ROLE_CODE),
			),
		)
		val ok = assertIs<ProductionTaskMutationResult.Success<List<CreatedProductionTaskSummary>>>(r)
		assertEquals(1, ok.value.size)
		assertEquals(ProductionTaskStatus.NOT_STARTED, ok.value.single().status)
		assertEquals(ProductionTaskHistoryEventType.CREATED, historyType)
		assertEquals(1, auditCount)
	}

	private fun useCase(
		onHistory: (ProductionTaskHistoryEvent) -> ProductionTaskHistoryEvent = { it },
		onAudit: () -> Unit = {},
		onSave: (ProductionTask) -> Unit = {},
	): CreateProductionTasksFromOrderUseCase {
		val stored = mutableListOf<ProductionTask>()
		val auditPort = object : ProductionTaskAuditPort {
			override fun record(event: ProductionTaskAuditEvent): ProductionTaskAuditEvent {
				onAudit()
				return event
			}
		}
		return CreateProductionTasksFromOrderUseCase(
			tasks = object : ProductionTaskPort {
				override fun findById(id: UUID): ProductionTask? = stored.find { it.id == id }
				override fun save(task: ProductionTask): ProductionTask {
					onSave(task)
					stored.removeIf { it.id == task.id }
					stored.add(task)
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
			orderSource = object : ProductionOrderSourcePort {
				override fun findOrderSource(oId: UUID) = orderSummary.takeIf { oId == orderId }
				override fun findOrderItemSource(oId: UUID, oi: UUID) = itemSummary.takeIf { oId == orderId && oi == itemId }
			},
			executors = object : ProductionExecutorPort {
				override fun findExecutor(id: UUID) = null
				override fun searchExecutors(search: String?, limit: Int) = emptyList<ProductionTaskExecutorSummary>()
			},
			numbers = object : ProductionTaskNumberPort {
				override fun nextTaskNumber(): String = "PT-UNIT-001"
			},
			traces = object : ProductionTaskTracePort {
				override fun saveHistoryEvent(event: ProductionTaskHistoryEvent) = onHistory(event)
				override fun findHistoryEvents(taskId: UUID): List<ProductionTaskHistoryEvent> = emptyList()
			},
			audit = ProductionTaskAuditService(auditPort),
		)
	}
}
