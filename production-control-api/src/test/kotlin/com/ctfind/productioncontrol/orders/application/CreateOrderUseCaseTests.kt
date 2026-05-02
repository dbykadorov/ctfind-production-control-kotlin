package com.ctfind.productioncontrol.orders.application

import com.ctfind.productioncontrol.orders.OrderTestFixtures
import com.ctfind.productioncontrol.orders.domain.Customer
import com.ctfind.productioncontrol.orders.domain.CustomerOrder
import com.ctfind.productioncontrol.orders.domain.OrderAuditEvent
import com.ctfind.productioncontrol.orders.domain.OrderChangeDiff
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import com.ctfind.productioncontrol.orders.domain.OrderStatusChange
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CreateOrderUseCaseTests {

	@Test
	fun `create order assigns order number, initial status, trace rows, and audit event`() {
		val recorder = RecordingCreatePorts()
		val useCase = CreateOrderUseCase(
			customers = recorder.customers,
			orders = recorder.orders,
			orderNumbers = recorder.orderNumbers,
			traces = recorder.traces,
			audit = recorder.audit,
		)

		val result = useCase.create(
			CreateOrderCommand(
				customerId = OrderTestFixtures.customerId,
				deliveryDate = LocalDate.parse("2026-05-15"),
				notes = "New order",
				items = listOf(OrderItemCommand("Столешница", BigDecimal("2"), "шт")),
				actor = AuthenticatedOrderActor(
					userId = OrderTestFixtures.actorUserId,
					login = "manager",
					displayName = "Manager",
					roleCodes = setOf(ORDER_MANAGER_ROLE_CODE),
				),
			),
		)

		val success = assertIs<OrderMutationResult.Success<OrderDetailView>>(result)
		assertEquals("ORD-000123", success.value.orderNumber)
		assertEquals(OrderStatus.NEW, success.value.status)
		assertEquals(1, recorder.statusChanges.size)
		assertEquals(1, recorder.changeDiffs.size)
		assertEquals("ORDER_CREATED", recorder.auditEvents.single().eventType)
	}

	@Test
	fun `create order is forbidden for read only users`() {
		val recorder = RecordingCreatePorts()
		val useCase = CreateOrderUseCase(recorder.customers, recorder.orders, recorder.orderNumbers, recorder.traces, recorder.audit)

		val result = useCase.create(
			CreateOrderCommand(
				customerId = OrderTestFixtures.customerId,
				deliveryDate = LocalDate.parse("2026-05-15"),
				items = listOf(OrderItemCommand("Столешница", BigDecimal.ONE, "шт")),
				actor = AuthenticatedOrderActor(
					userId = OrderTestFixtures.actorUserId,
					login = "viewer",
					displayName = "Viewer",
					roleCodes = setOf("EXECUTOR"),
				),
			),
		)

		assertEquals(OrderMutationResult.Forbidden, result)
		assertEquals(0, recorder.savedOrders.size)
	}
}

private class RecordingCreatePorts {
	val savedOrders = mutableListOf<CustomerOrder>()
	val statusChanges = mutableListOf<OrderStatusChange>()
	val changeDiffs = mutableListOf<OrderChangeDiff>()
	val auditEvents = mutableListOf<OrderAuditEvent>()
	private val customer = OrderTestFixtures.activeCustomer()

	val customers = object : CustomerPort {
		override fun findById(id: UUID): Customer? = customer.takeIf { it.id == id }
		override fun search(query: CustomerSearchQuery): List<Customer> = listOf(customer)
		override fun save(customer: Customer): Customer = customer
	}

	val orderNumbers = object : OrderNumberPort {
		override fun nextOrderNumber(): String = "ORD-000123"
	}

	val orders = object : CustomerOrderPort {
		override fun findById(id: UUID): CustomerOrder? = savedOrders.firstOrNull { it.id == id }
		override fun save(order: CustomerOrder): CustomerOrder = order.also { savedOrders.add(it) }
		override fun search(query: OrderListQuery, today: LocalDate): PageResult<CustomerOrder> =
			PageResult(savedOrders, query.page, query.size, savedOrders.size.toLong())

		override fun count(query: OrderListQuery, today: LocalDate): Long = savedOrders.size.toLong()
		override fun countByStatus(status: OrderStatus): Long = savedOrders.count { it.status == status }.toLong()
		override fun countAll(): Long = savedOrders.size.toLong()
		override fun countActive(): Long = savedOrders.count { it.status != OrderStatus.SHIPPED }.toLong()
		override fun countOverdue(today: LocalDate): Long = 0
		override fun createdTrend(days: Int, today: LocalDate): List<OrderTrendPoint> = emptyList()
		override fun shippedTrend(days: Int, today: LocalDate): List<OrderTrendPoint> = emptyList()
	}

	val traces = object : OrderTracePort {
		override fun saveStatusChange(change: OrderStatusChange): OrderStatusChange = change.also { statusChanges.add(it) }
		override fun saveChangeDiff(diff: OrderChangeDiff): OrderChangeDiff = diff.also { changeDiffs.add(it) }
		override fun findStatusChanges(orderId: UUID): List<OrderStatusChange> = statusChanges.filter { it.orderId == orderId }
		override fun findChangeDiffs(orderId: UUID): List<OrderChangeDiff> = changeDiffs.filter { it.orderId == orderId }
		override fun recentStatusChanges(limit: Int): List<OrderStatusChange> = statusChanges.take(limit)
	}

	val audit = object : OrderAuditPort {
		override fun record(event: OrderAuditEvent): OrderAuditEvent = event.also { auditEvents.add(it) }
	}
}
