package com.ctfind.productioncontrol.orders.application

import com.ctfind.productioncontrol.orders.OrderTestFixtures
import com.ctfind.productioncontrol.orders.domain.Customer
import com.ctfind.productioncontrol.orders.domain.CustomerOrder
import com.ctfind.productioncontrol.orders.domain.OrderAuditEvent
import com.ctfind.productioncontrol.orders.domain.OrderChangeDiff
import com.ctfind.productioncontrol.orders.domain.OrderChangeType
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import com.ctfind.productioncontrol.orders.domain.OrderStatusChange
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UpdateOrderUseCaseTests {

	@Test
	fun `update order replaces editable fields, records diffs, and increments version`() {
		val recorder = RecordingUpdatePorts()
		val useCase = UpdateOrderUseCase(
			customers = recorder.customers,
			orders = recorder.orders,
			traces = recorder.traces,
			audit = recorder.audit,
			diffService = OrderDiffService(),
		)

		val result = useCase.update(
			UpdateOrderCommand(
				orderId = OrderTestFixtures.orderId,
				expectedVersion = 0,
				customerId = OrderTestFixtures.customerId,
				deliveryDate = LocalDate.parse("2026-05-20"),
				notes = "Updated order",
				items = listOf(OrderItemCommand("Фасад", BigDecimal("3"), "шт")),
				actor = writeActor(),
			),
		)

		val success = assertIs<OrderMutationResult.Success<OrderDetailView>>(result)
		assertEquals(LocalDate.parse("2026-05-20"), success.value.deliveryDate)
		assertEquals("Updated order", success.value.notes)
		assertEquals(1, success.value.version)
		assertEquals("Фасад", recorder.savedOrders.single().items.single().itemName)
		assertEquals(OrderChangeType.UPDATED, recorder.changeDiffs.single().changeType)
		assertEquals(
			listOf("delivery_date", "notes", "items"),
			recorder.changeDiffs.single().fieldDiffs.map { it.fieldName },
		)
		assertEquals("ORDER_UPDATED", recorder.auditEvents.single().eventType)
	}

	@Test
	fun `update order rejects stale version without saving`() {
		val recorder = RecordingUpdatePorts(existingOrder = OrderTestFixtures.order(version = 3))
		val useCase = UpdateOrderUseCase(recorder.customers, recorder.orders, recorder.traces, recorder.audit, OrderDiffService())

		val result = useCase.update(
			UpdateOrderCommand(
				orderId = OrderTestFixtures.orderId,
				expectedVersion = 2,
				customerId = OrderTestFixtures.customerId,
				deliveryDate = LocalDate.parse("2026-05-20"),
				items = listOf(OrderItemCommand("Фасад", BigDecimal.ONE, "шт")),
				actor = writeActor(),
			),
		)

		assertEquals(OrderMutationResult.StaleVersion, result)
		assertEquals(0, recorder.savedOrders.size)
	}

	@Test
	fun `update order rejects shipped orders as read only`() {
		val recorder = RecordingUpdatePorts(existingOrder = OrderTestFixtures.order(status = OrderStatus.SHIPPED))
		val useCase = UpdateOrderUseCase(recorder.customers, recorder.orders, recorder.traces, recorder.audit, OrderDiffService())

		val result = useCase.update(
			UpdateOrderCommand(
				orderId = OrderTestFixtures.orderId,
				expectedVersion = 0,
				customerId = OrderTestFixtures.customerId,
				deliveryDate = LocalDate.parse("2026-05-20"),
				items = listOf(OrderItemCommand("Фасад", BigDecimal.ONE, "шт")),
				actor = writeActor(),
			),
		)

		assertIs<OrderMutationResult.ValidationFailed>(result)
		assertEquals(0, recorder.savedOrders.size)
	}

	@Test
	fun `update order is forbidden for read only users`() {
		val recorder = RecordingUpdatePorts()
		val useCase = UpdateOrderUseCase(recorder.customers, recorder.orders, recorder.traces, recorder.audit, OrderDiffService())

		val result = useCase.update(
			UpdateOrderCommand(
				orderId = OrderTestFixtures.orderId,
				expectedVersion = 0,
				customerId = OrderTestFixtures.customerId,
				deliveryDate = LocalDate.parse("2026-05-20"),
				items = listOf(OrderItemCommand("Фасад", BigDecimal.ONE, "шт")),
				actor = writeActor(roleCodes = setOf("EXECUTOR")),
			),
		)

		assertEquals(OrderMutationResult.Forbidden, result)
		assertEquals(0, recorder.savedOrders.size)
	}
}

private fun writeActor(roleCodes: Set<String> = setOf(ORDER_MANAGER_ROLE_CODE)): AuthenticatedOrderActor =
	AuthenticatedOrderActor(
		userId = OrderTestFixtures.actorUserId,
		login = "manager",
		displayName = "Manager",
		roleCodes = roleCodes,
	)

private class RecordingUpdatePorts(
	private val existingOrder: CustomerOrder = OrderTestFixtures.order(),
	private val customer: Customer = OrderTestFixtures.activeCustomer(),
) {
	val savedOrders = mutableListOf<CustomerOrder>()
	val changeDiffs = mutableListOf<OrderChangeDiff>()
	val auditEvents = mutableListOf<OrderAuditEvent>()

	val customers = object : CustomerPort {
		override fun findById(id: UUID): Customer? = customer.takeIf { it.id == id }
		override fun search(query: CustomerSearchQuery): List<Customer> = listOf(customer)
		override fun save(customer: Customer): Customer = customer
	}

	val orders = object : CustomerOrderPort {
		override fun findById(id: UUID): CustomerOrder? = existingOrder.takeIf { it.id == id }
		override fun save(order: CustomerOrder): CustomerOrder = order.also { savedOrders.add(it) }
		override fun search(query: OrderListQuery, today: LocalDate): PageResult<CustomerOrder> =
			PageResult(listOf(existingOrder), query.page, query.size, 1)

		override fun count(query: OrderListQuery, today: LocalDate): Long = 1
		override fun countByStatus(status: OrderStatus): Long = if (status == existingOrder.status) 1 else 0
		override fun countAll(): Long = 1
		override fun countActive(): Long = if (existingOrder.status == OrderStatus.SHIPPED) 0 else 1
		override fun countOverdue(today: LocalDate): Long = 0
		override fun createdTrend(days: Int, today: LocalDate): List<OrderTrendPoint> = emptyList()
		override fun shippedTrend(days: Int, today: LocalDate): List<OrderTrendPoint> = emptyList()
	}

	val traces = object : OrderTracePort {
		override fun saveStatusChange(change: OrderStatusChange): OrderStatusChange = change
		override fun saveChangeDiff(diff: OrderChangeDiff): OrderChangeDiff = diff.also { changeDiffs.add(it) }
		override fun findStatusChanges(orderId: UUID): List<OrderStatusChange> = emptyList()
		override fun findChangeDiffs(orderId: UUID): List<OrderChangeDiff> = changeDiffs.filter { it.orderId == orderId }
		override fun recentStatusChanges(limit: Int): List<OrderStatusChange> = emptyList()
	}

	val audit = object : OrderAuditPort {
		override fun record(event: OrderAuditEvent): OrderAuditEvent = event.also { auditEvents.add(it) }
	}
}
