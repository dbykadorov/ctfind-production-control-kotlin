package com.ctfind.productioncontrol.orders.application

import com.ctfind.productioncontrol.orders.domain.OrderStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class OrderDashboardUseCase private constructor(
	private val orders: CustomerOrderPort? = null,
	private val customers: CustomerPort? = null,
	private val traces: OrderTracePort? = null,
	private val handler: ((LocalDate) -> DashboardSummary)? = null,
) {
	@Autowired
	constructor(
		orders: CustomerOrderPort,
		customers: CustomerPort,
		traces: OrderTracePort,
	) : this(orders, customers, traces, null)

	constructor(handler: (LocalDate) -> DashboardSummary) : this(null, null, null, handler)

	fun summary(today: LocalDate = LocalDate.now()): DashboardSummary {
		handler?.let { return it(today) }
		val orders = requireNotNull(orders)
		val customers = requireNotNull(customers)
		val traces = requireNotNull(traces)

		return DashboardSummary(
			totalOrders = orders.countAll(),
			activeOrders = orders.countActive(),
			overdueOrders = orders.countOverdue(today),
			statusCounts = OrderStatus.entries.associateWith { orders.countByStatus(it) },
			recentChanges = traces.recentStatusChanges(limit = 10).mapNotNull { change ->
				val order = orders.findById(change.orderId) ?: return@mapNotNull null
				val customer = customers.findById(order.customerId)
				OrderStatusChangeSummary(
					orderId = order.id,
					orderNumber = order.orderNumber,
					customerDisplayName = customer?.displayName ?: "Unknown customer",
					fromStatus = change.fromStatus,
					toStatus = change.toStatus,
					changedAt = change.changedAt,
					actorDisplayName = change.actorUserId.toString(),
				)
			},
			trend = mergeTrendPoints(
				created = orders.createdTrend(days = 60, today = today),
				shipped = orders.shippedTrend(days = 60, today = today),
			),
		)
	}
}

private fun mergeTrendPoints(
	created: List<OrderTrendPoint>,
	shipped: List<OrderTrendPoint>,
): List<OrderTrendPoint> {
	val dates = (created.map { it.date } + shipped.map { it.date }).toSortedSet()
	return dates.map { date ->
		OrderTrendPoint(
			date = date,
			created = created.firstOrNull { it.date == date }?.created ?: 0,
			shipped = shipped.firstOrNull { it.date == date }?.shipped ?: 0,
		)
	}
}
