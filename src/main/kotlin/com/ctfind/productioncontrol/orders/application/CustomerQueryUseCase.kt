package com.ctfind.productioncontrol.orders.application

import org.springframework.stereotype.Service

@Service
class CustomerQueryUseCase(
	private val customers: CustomerPort,
) {
	fun search(query: CustomerSearchQuery): List<CustomerSummary> =
		customers.search(query).map {
			CustomerSummary(
				id = it.id,
				displayName = it.displayName,
				status = it.status,
				contactPerson = it.contactPerson,
				phone = it.phone,
				email = it.email,
			)
		}
}
