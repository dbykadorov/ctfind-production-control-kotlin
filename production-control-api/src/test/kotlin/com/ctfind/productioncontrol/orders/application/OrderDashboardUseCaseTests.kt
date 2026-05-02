package com.ctfind.productioncontrol.orders.application

import com.ctfind.productioncontrol.orders.OrderTestFixtures
import com.ctfind.productioncontrol.orders.domain.Customer
import com.ctfind.productioncontrol.orders.domain.CustomerOrder
import com.ctfind.productioncontrol.orders.domain.OrderAuditEvent
import com.ctfind.productioncontrol.orders.domain.OrderChangeDiff
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import com.ctfind.productioncontrol.orders.domain.OrderStatusChange
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderDashboardUseCaseTests {

	@Test
	fun `dashboard summary returns counts recent changes and merged trend`() {
		val ports = RecordingDashboardPorts()
		val useCase = OrderDashboardUseCase(ports.orders, ports.customers, ports.traces)

		val summary = useCase.summary(today = LocalDate.parse("2026-04-27"))

		assertEquals(7, summary.totalOrders)
		assertEquals(5, summary.activeOrders)
		assertEquals(2, summary.overdueOrders)
		assertEquals(2, summary.statusCounts[OrderStatus.NEW])
		assertEquals(1, summary.statusCounts[OrderStatus.IN_WORK])
		assertEquals("ORD-000001", summary.recentChanges.single().orderNumber)
		assertEquals("ООО Ромашка", summary.recentChanges.single().customerDisplayName)
		assertEquals(
			listOf(
				OrderTrendPoint(LocalDate.parse("2026-04-26"), created = 3, shipped = 1),
				OrderTrendPoint(LocalDate.parse("2026-04-27"), created = 4, shipped = 2),
			),
			summary.trend,
		)
	}
}

private class RecordingDashboardPorts {
	private val customer = OrderTestFixtures.activeCustomer()
	private val order = OrderTestFixtures.order(status = OrderStatus.IN_WORK)

	val customers = object : CustomerPort {
		override fun findById(id: UUID): Customer? = customer.takeIf { it.id == id }
		override fun search(query: CustomerSearchQuery): List<Customer> = listOf(customer)
		override fun save(customer: Customer): Customer = customer
	}

	val orders = object : CustomerOrderPort {
		override fun findById(id: UUID): CustomerOrder? = order.takeIf { it.id == id }
		override fun save(order: CustomerOrder): CustomerOrder = order
		override fun search(query: OrderListQuery, today: LocalDate): PageResult<CustomerOrder> =
			PageResult(listOf(order), query.page, query.size, 1)

		override fun count(query: OrderListQuery, today: LocalDate): Long = 1
		override fun countByStatus(status: OrderStatus): Long =
			when (status) {
				OrderStatus.NEW -> 2
				OrderStatus.IN_WORK -> 1
				OrderStatus.READY -> 2
				OrderStatus.SHIPPED -> 2
			}

		override fun countAll(): Long = 7
		override fun countActive(): Long = 5
		override fun countOverdue(today: LocalDate): Long = 2
		override fun createdTrend(days: Int, today: LocalDate): List<OrderTrendPoint> =
			listOf(
				OrderTrendPoint(LocalDate.parse("2026-04-26"), created = 3, shipped = 0),
				OrderTrendPoint(LocalDate.parse("2026-04-27"), created = 4, shipped = 0),
			)

		override fun shippedTrend(days: Int, today: LocalDate): List<OrderTrendPoint> =
			listOf(
				OrderTrendPoint(LocalDate.parse("2026-04-26"), created = 0, shipped = 1),
				OrderTrendPoint(LocalDate.parse("2026-04-27"), created = 0, shipped = 2),
			)
	}

	val traces = object : OrderTracePort {
		override fun saveStatusChange(change: OrderStatusChange): OrderStatusChange = change
		override fun saveChangeDiff(diff: OrderChangeDiff): OrderChangeDiff = diff
		override fun findStatusChanges(orderId: UUID): List<OrderStatusChange> = emptyList()
		override fun findChangeDiffs(orderId: UUID): List<OrderChangeDiff> = emptyList()
		override fun recentStatusChanges(limit: Int): List<OrderStatusChange> =
			listOf(
				OrderStatusChange(
					id = UUID.fromString("55555555-5555-5555-5555-555555555555"),
					orderId = OrderTestFixtures.orderId,
					fromStatus = OrderStatus.NEW,
					toStatus = OrderStatus.IN_WORK,
					actorUserId = OrderTestFixtures.actorUserId,
					changedAt = Instant.parse("2026-04-26T19:00:00Z"),
				),
			)
	}

	val audit = object : OrderAuditPort {
		override fun record(event: OrderAuditEvent): OrderAuditEvent = event
	}
}
