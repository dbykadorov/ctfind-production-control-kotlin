package com.ctfind.productioncontrol.orders.adapter.web

import com.ctfind.productioncontrol.orders.OrderTestFixtures
import com.ctfind.productioncontrol.orders.application.OrderListQuery
import com.ctfind.productioncontrol.orders.application.OrderQueryUseCase
import com.ctfind.productioncontrol.orders.application.PageResult
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderControllerQueryTests {

	@Test
	fun `orders endpoint returns paged order list`() {
		val controller = OrderController(orderQuery = oneOrderQueryUseCase())

		val response = controller.list(search = "ORD", status = null, page = 0, size = 20)

		assertEquals(1, response.items.size)
		assertEquals("ORD-000001", response.items.single().orderNumber)
		assertEquals(OrderStatus.NEW, response.items.single().status)
	}

	@Test
	fun `detail endpoint returns not found for missing order`() {
		val controller = OrderController(orderQuery = oneOrderQueryUseCase())

		val response = controller.detail(UUID.randomUUID())

		assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
	}
}

fun oneOrderQueryUseCase(): OrderQueryUseCase {
	val customer = OrderTestFixtures.activeCustomer()
	val order = OrderTestFixtures.order()
	return OrderQueryUseCase(
		orders = object : com.ctfind.productioncontrol.orders.application.CustomerOrderPort {
			override fun findById(id: UUID) = order.takeIf { it.id == id }
			override fun save(order: com.ctfind.productioncontrol.orders.domain.CustomerOrder) = order
			override fun search(query: OrderListQuery, today: LocalDate) =
				PageResult(listOf(order), query.page, query.size, 1)

			override fun count(query: OrderListQuery, today: LocalDate) = 1L
			override fun countByStatus(status: OrderStatus) = if (status == order.status) 1L else 0L
			override fun countAll() = 1L
			override fun countActive() = 1L
			override fun countOverdue(today: LocalDate) = 0L
			override fun createdTrend(days: Int, today: LocalDate) = emptyList<com.ctfind.productioncontrol.orders.application.OrderTrendPoint>()
			override fun shippedTrend(days: Int, today: LocalDate) = emptyList<com.ctfind.productioncontrol.orders.application.OrderTrendPoint>()
		},
		customers = object : com.ctfind.productioncontrol.orders.application.CustomerPort {
			override fun findById(id: UUID) = customer.takeIf { it.id == id }
			override fun search(query: com.ctfind.productioncontrol.orders.application.CustomerSearchQuery) = listOf(customer)
			override fun save(customer: com.ctfind.productioncontrol.orders.domain.Customer) = customer
		},
		traces = object : com.ctfind.productioncontrol.orders.application.OrderTracePort {
			override fun saveStatusChange(change: com.ctfind.productioncontrol.orders.domain.OrderStatusChange) = change
			override fun saveChangeDiff(diff: com.ctfind.productioncontrol.orders.domain.OrderChangeDiff) = diff
			override fun findStatusChanges(orderId: UUID) = emptyList<com.ctfind.productioncontrol.orders.domain.OrderStatusChange>()
			override fun findChangeDiffs(orderId: UUID) = emptyList<com.ctfind.productioncontrol.orders.domain.OrderChangeDiff>()
			override fun recentStatusChanges(limit: Int) = emptyList<com.ctfind.productioncontrol.orders.domain.OrderStatusChange>()
		},
	)
}
