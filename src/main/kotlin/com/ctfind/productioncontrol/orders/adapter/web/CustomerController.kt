package com.ctfind.productioncontrol.orders.adapter.web

import com.ctfind.productioncontrol.orders.application.CustomerQueryUseCase
import com.ctfind.productioncontrol.orders.application.CustomerSearchQuery
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/customers")
class CustomerController(
	private val customerQuery: CustomerQueryUseCase,
) {
	@GetMapping
	fun search(
		@RequestParam(required = false) search: String? = null,
		@RequestParam(required = false, defaultValue = "true") activeOnly: Boolean = true,
		@RequestParam(required = false, defaultValue = "20") limit: Int = 20,
	): CustomerSearchResponse =
		CustomerSearchResponse(
			items = customerQuery.search(
				CustomerSearchQuery(
					search = search,
					activeOnly = activeOnly,
					limit = limit,
				),
			).map { it.toResponse() },
		)
}
