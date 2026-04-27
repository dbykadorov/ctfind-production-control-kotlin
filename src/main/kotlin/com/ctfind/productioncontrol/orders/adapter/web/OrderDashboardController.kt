package com.ctfind.productioncontrol.orders.adapter.web

import com.ctfind.productioncontrol.orders.application.OrderDashboardUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/orders/dashboard")
class OrderDashboardController(
	private val dashboard: OrderDashboardUseCase,
) {
	@GetMapping
	fun summary(): DashboardSummaryResponse =
		dashboard.summary().toResponse()
}
