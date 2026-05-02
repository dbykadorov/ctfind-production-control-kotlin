package com.ctfind.productioncontrol.orders.adapter.web

import com.ctfind.productioncontrol.orders.application.DashboardSummary
import com.ctfind.productioncontrol.orders.application.OrderDashboardUseCase
import com.ctfind.productioncontrol.orders.application.OrderStatusChangeSummary
import com.ctfind.productioncontrol.orders.application.OrderTrendPoint
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderDashboardControllerTests {

	@Test
	fun `dashboard endpoint returns summary response`() {
		val controller = OrderDashboardController(
			OrderDashboardUseCase {
				DashboardSummary(
					totalOrders = 7,
					activeOrders = 5,
					overdueOrders = 2,
					statusCounts = mapOf(OrderStatus.NEW to 2, OrderStatus.IN_WORK to 1),
					recentChanges = listOf(
						OrderStatusChangeSummary(
							orderId = UUID.fromString("22222222-2222-2222-2222-222222222222"),
							orderNumber = "ORD-000001",
							customerDisplayName = "ООО Ромашка",
							fromStatus = OrderStatus.NEW,
							toStatus = OrderStatus.IN_WORK,
							changedAt = Instant.parse("2026-04-26T19:00:00Z"),
							actorDisplayName = "Manager",
						),
					),
					trend = listOf(OrderTrendPoint(LocalDate.parse("2026-04-27"), created = 4, shipped = 2)),
				)
			},
		)

		val response = controller.summary()

		assertEquals(7, response.totalOrders)
		assertEquals(5, response.activeOrders)
		assertEquals(2, response.overdueOrders)
		assertEquals(2, response.statusCounts[OrderStatus.NEW])
		assertEquals("ORD-000001", response.recentChanges.single().orderNumber)
		assertEquals(4, response.trend.single().created)
	}
}
