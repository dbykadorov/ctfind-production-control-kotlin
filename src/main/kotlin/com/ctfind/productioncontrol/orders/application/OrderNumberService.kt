package com.ctfind.productioncontrol.orders.application

import org.springframework.stereotype.Service

@Service
class OrderNumberService(
	private val orderNumbers: OrderNumberPort,
) {
	fun nextOrderNumber(): String =
		orderNumbers.nextOrderNumber()
}
