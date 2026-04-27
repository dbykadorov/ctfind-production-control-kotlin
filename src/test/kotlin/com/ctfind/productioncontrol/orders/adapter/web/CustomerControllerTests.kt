package com.ctfind.productioncontrol.orders.adapter.web

import com.ctfind.productioncontrol.orders.OrderTestFixtures
import com.ctfind.productioncontrol.orders.application.CustomerQueryUseCase
import com.ctfind.productioncontrol.orders.application.CustomerSearchQuery
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomerControllerTests {

	@Test
	fun `customers endpoint returns active customer search results`() {
		val controller = CustomerController(
			customerQuery = CustomerQueryUseCase(
				customers = object : com.ctfind.productioncontrol.orders.application.CustomerPort {
					override fun findById(id: java.util.UUID) = OrderTestFixtures.activeCustomer().takeIf { it.id == id }
					override fun search(query: CustomerSearchQuery) = listOf(OrderTestFixtures.activeCustomer())
					override fun save(customer: com.ctfind.productioncontrol.orders.domain.Customer) = customer
				},
			),
		)

		val response = controller.search(search = "ром", activeOnly = true, limit = 5)

		assertEquals(1, response.items.size)
		assertEquals("ООО Ромашка", response.items.single().displayName)
	}
}
