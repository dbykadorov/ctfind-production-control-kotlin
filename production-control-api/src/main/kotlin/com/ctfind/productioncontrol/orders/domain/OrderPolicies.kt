package com.ctfind.productioncontrol.orders.domain

class InvalidOrderStatusTransition(
	val fromStatus: OrderStatus,
	val toStatus: OrderStatus,
) : RuntimeException("Only direct forward order status transitions are allowed")

object OrderStatusPolicy {
	fun assertDirectForward(fromStatus: OrderStatus, toStatus: OrderStatus) {
		if (fromStatus.nextForward() != toStatus)
			throw InvalidOrderStatusTransition(fromStatus, toStatus)
	}

	fun isDirectForward(fromStatus: OrderStatus, toStatus: OrderStatus): Boolean =
		fromStatus.nextForward() == toStatus
}

object OrderEditPolicy {
	fun canRegularEdit(order: CustomerOrder): Boolean =
		!order.shipped

	fun assertRegularEditable(order: CustomerOrder) {
		require(canRegularEdit(order)) { "shipped orders are read-only for regular edits" }
	}
}
