package com.ctfind.productioncontrol.orders.domain

import java.math.BigDecimal
import java.util.UUID

data class CustomerOrderItem(
	val id: UUID,
	val lineNo: Int,
	val itemName: String,
	val quantity: BigDecimal,
	val uom: String,
) {
	init {
		require(lineNo > 0) { "order item line number must be positive" }
		require(itemName.isNotBlank()) { "order item name must not be blank" }
		require(quantity > BigDecimal.ZERO) { "order item quantity must be greater than zero" }
		require(uom.isNotBlank()) { "order item uom must not be blank" }
	}
}
