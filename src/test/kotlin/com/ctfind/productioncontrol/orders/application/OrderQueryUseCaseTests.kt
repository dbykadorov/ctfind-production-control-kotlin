package com.ctfind.productioncontrol.orders.application

import com.ctfind.productioncontrol.orders.OrderTestFixtures
import com.ctfind.productioncontrol.orders.domain.Customer
import com.ctfind.productioncontrol.orders.domain.CustomerOrder
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrderQueryUseCaseTests {

	@Test
	fun `list maps orders with customer summaries and overdue flag`() {
		val customer = OrderTestFixtures.activeCustomer()
		val order = OrderTestFixtures.order(status = OrderStatus.IN_WORK)
		val useCase = OrderQueryUseCase(
			orders = fakeOrderPort(listOf(order)),
			customers = fakeCustomerPort(customer),
			traces = emptyTracePort(),
		)

		val result = useCase.list(OrderListQuery(overdueOnly = true), today = LocalDate.parse("2026-06-01"))

		assertEquals(1, result.items.size)
		assertEquals("ORD-000001", result.items.single().orderNumber)
		assertEquals(customer.displayName, result.items.single().customer.displayName)
		assertTrue(result.items.single().overdue)
	}

	@Test
	fun `detail returns order items and empty history when no trace rows exist`() {
		val customer = OrderTestFixtures.activeCustomer()
		val order = OrderTestFixtures.order()
		val useCase = OrderQueryUseCase(
			orders = fakeOrderPort(listOf(order)),
			customers = fakeCustomerPort(customer),
			traces = emptyTracePort(),
		)

		val result = useCase.detail(order.id, today = LocalDate.parse("2026-04-26"))

		assertEquals(order.id, result?.id)
		assertEquals(1, result?.items?.size)
		assertEquals(emptyList(), result?.history)
		assertEquals(emptyList(), result?.changeDiffs)
	}
}

private fun fakeCustomerPort(customer: Customer): CustomerPort =
	object : CustomerPort {
		override fun findById(id: UUID): Customer? = customer.takeIf { it.id == id }
		override fun search(query: CustomerSearchQuery): List<Customer> = listOf(customer)
		override fun save(customer: Customer): Customer = customer
	}

private fun fakeOrderPort(rows: List<CustomerOrder>): CustomerOrderPort =
	object : CustomerOrderPort {
		override fun findById(id: UUID): CustomerOrder? = rows.firstOrNull { it.id == id }
		override fun save(order: CustomerOrder): CustomerOrder = order
		override fun search(query: OrderListQuery, today: LocalDate): PageResult<CustomerOrder> =
			PageResult(rows, query.page, query.size, rows.size.toLong())

		override fun count(query: OrderListQuery, today: LocalDate): Long = rows.size.toLong()
		override fun countByStatus(status: OrderStatus): Long = rows.count { it.status == status }.toLong()
		override fun countAll(): Long = rows.size.toLong()
		override fun countActive(): Long = rows.count { it.status != OrderStatus.SHIPPED }.toLong()
		override fun countOverdue(today: LocalDate): Long = rows.count { it.deliveryDate.isBefore(today) }.toLong()
		override fun createdTrend(days: Int, today: LocalDate): List<OrderTrendPoint> = emptyList()
		override fun shippedTrend(days: Int, today: LocalDate): List<OrderTrendPoint> = emptyList()
	}

private fun emptyTracePort(): OrderTracePort =
	object : OrderTracePort {
		override fun saveStatusChange(change: com.ctfind.productioncontrol.orders.domain.OrderStatusChange) = change
		override fun saveChangeDiff(diff: com.ctfind.productioncontrol.orders.domain.OrderChangeDiff) = diff
		override fun findStatusChanges(orderId: UUID) = emptyList<com.ctfind.productioncontrol.orders.domain.OrderStatusChange>()
		override fun findChangeDiffs(orderId: UUID) = emptyList<com.ctfind.productioncontrol.orders.domain.OrderChangeDiff>()
		override fun recentStatusChanges(limit: Int) = emptyList<com.ctfind.productioncontrol.orders.domain.OrderStatusChange>()
	}
