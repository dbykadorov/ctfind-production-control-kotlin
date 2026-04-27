package com.ctfind.productioncontrol.orders.domain

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CustomerOrder(
	val id: UUID,
	val orderNumber: String,
	val customerId: UUID,
	val deliveryDate: LocalDate,
	val status: OrderStatus,
	val notes: String? = null,
	val items: List<CustomerOrderItem>,
	val createdByUserId: UUID,
	val createdAt: Instant,
	val updatedAt: Instant,
	val version: Long,
) {
	init {
		require(orderNumber.isNotBlank()) { "order number must not be blank" }
		require(items.isNotEmpty()) { "order must contain at least one item" }
		require(items.map { it.lineNo }.toSet().size == items.size) { "order item line numbers must be unique" }
		require(version >= 0) { "order version must not be negative" }
	}

	val shipped: Boolean get() = status == OrderStatus.SHIPPED
}
