package com.ctfind.productioncontrol.orders.application

import com.ctfind.productioncontrol.orders.OrderTestFixtures
import com.ctfind.productioncontrol.orders.domain.Customer
import com.ctfind.productioncontrol.orders.domain.CustomerOrder
import com.ctfind.productioncontrol.orders.domain.OrderAuditEvent
import com.ctfind.productioncontrol.orders.domain.OrderChangeDiff
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import com.ctfind.productioncontrol.orders.domain.OrderStatusChange
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ChangeOrderStatusUseCaseTests {

	@Test
	fun `change status applies direct forward transition and records history plus audit`() {
		val recorder = RecordingStatusPorts()
		val useCase = ChangeOrderStatusUseCase(recorder.customers, recorder.orders, recorder.traces, recorder.audit)

		val result = useCase.changeStatus(
			ChangeOrderStatusCommand(
				orderId = OrderTestFixtures.orderId,
				expectedVersion = 0,
				toStatus = OrderStatus.IN_WORK,
				note = "Started",
				actor = statusActor(),
			),
		)

		val success = assertIs<OrderMutationResult.Success<OrderDetailView>>(result)
		assertEquals(OrderStatus.IN_WORK, success.value.status)
		assertEquals(1, success.value.version)
		assertEquals(OrderStatus.NEW, recorder.statusChanges.single().fromStatus)
		assertEquals(OrderStatus.IN_WORK, recorder.statusChanges.single().toStatus)
		assertEquals("ORDER_STATUS_CHANGED", recorder.auditEvents.single().eventType)
	}

	@Test
	fun `change status rejects stale versions without saving`() {
		val recorder = RecordingStatusPorts(existingOrder = OrderTestFixtures.order(version = 2))
		val useCase = ChangeOrderStatusUseCase(recorder.customers, recorder.orders, recorder.traces, recorder.audit)

		val result = useCase.changeStatus(
			ChangeOrderStatusCommand(
				orderId = OrderTestFixtures.orderId,
				expectedVersion = 1,
				toStatus = OrderStatus.IN_WORK,
				actor = statusActor(),
			),
		)

		assertEquals(OrderMutationResult.StaleVersion, result)
		assertEquals(0, recorder.savedOrders.size)
	}

	@Test
	fun `change status rejects skipped transitions`() {
		val recorder = RecordingStatusPorts()
		val useCase = ChangeOrderStatusUseCase(recorder.customers, recorder.orders, recorder.traces, recorder.audit)

		val result = useCase.changeStatus(
			ChangeOrderStatusCommand(
				orderId = OrderTestFixtures.orderId,
				expectedVersion = 0,
				toStatus = OrderStatus.READY,
				actor = statusActor(),
			),
		)

		assertEquals(OrderMutationResult.InvalidTransition, result)
		assertEquals(0, recorder.savedOrders.size)
	}

	@Test
	fun `change status is forbidden for read only users`() {
		val recorder = RecordingStatusPorts()
		val useCase = ChangeOrderStatusUseCase(recorder.customers, recorder.orders, recorder.traces, recorder.audit)

		val result = useCase.changeStatus(
			ChangeOrderStatusCommand(
				orderId = OrderTestFixtures.orderId,
				expectedVersion = 0,
				toStatus = OrderStatus.IN_WORK,
				actor = statusActor(roleCodes = setOf("EXECUTOR")),
			),
		)

		assertEquals(OrderMutationResult.Forbidden, result)
		assertEquals(0, recorder.savedOrders.size)
	}
}

private fun statusActor(roleCodes: Set<String> = setOf(ORDER_MANAGER_ROLE_CODE)): AuthenticatedOrderActor =
	AuthenticatedOrderActor(
		userId = OrderTestFixtures.actorUserId,
		login = "manager",
		displayName = "Manager",
		roleCodes = roleCodes,
	)

private class RecordingStatusPorts(
	private val existingOrder: CustomerOrder = OrderTestFixtures.order(),
	private val customer: Customer = OrderTestFixtures.activeCustomer(),
) {
	val savedOrders = mutableListOf<CustomerOrder>()
	val statusChanges = mutableListOf<OrderStatusChange>()
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
