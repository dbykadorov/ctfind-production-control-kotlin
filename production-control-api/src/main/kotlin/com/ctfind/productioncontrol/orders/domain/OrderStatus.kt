package com.ctfind.productioncontrol.orders.domain

enum class OrderStatus(val label: String) {
	NEW("новый"),
	IN_WORK("в работе"),
	READY("готов"),
	SHIPPED("отгружен"),
}

fun OrderStatus.nextForward(): OrderStatus? =
	when (this) {
		OrderStatus.NEW -> OrderStatus.IN_WORK
		OrderStatus.IN_WORK -> OrderStatus.READY
		OrderStatus.READY -> OrderStatus.SHIPPED
		OrderStatus.SHIPPED -> null
	}
