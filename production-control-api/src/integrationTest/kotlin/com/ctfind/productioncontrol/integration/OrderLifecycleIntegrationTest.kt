package com.ctfind.productioncontrol.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class OrderLifecycleIntegrationTest : IntegrationTestSupport() {

	@Test
	@DisplayName("Order integration: ${ScenarioCoverage.ORDERS}")
	fun `order create read update lifecycle and authorization are wired`() {
		val orderManager = createScenarioUser("order.manager.integration", setOf("ORDER_MANAGER"))
		val nonWriter = createScenarioUser("order.viewer.integration", setOf("WAREHOUSE"))
		val customerId = insertCustomer("Order lifecycle customer")

		var order = createOrderWithItem(token = orderManager.token, customerId = customerId)

		val list = json(getJson("/api/orders", orderManager.token).assertOk())
		assertTrue(list["items"].any { it["id"].asText() == order.id.toString() })
		val detail = json(getJson("/api/orders/${order.id}", orderManager.token).assertOk())
		assertEquals(order.id.toString(), detail["id"].asText())

		val updated = json(
			putJson(
				path = "/api/orders/${order.id}",
				token = orderManager.token,
				body = mapOf(
					"expectedVersion" to order.version,
					"customerId" to customerId,
					"deliveryDate" to LocalDate.now().plusDays(10).toString(),
					"notes" to "updated integration order",
					"items" to listOf(
						mapOf("itemName" to "Updated item", "quantity" to BigDecimal("3.0"), "uom" to "pcs"),
					),
				),
			).assertOk(),
		)
		assertEquals("updated integration order", updated["notes"].asText())
		order = ScenarioOrder(
			id = order.id,
			customerId = customerId,
			itemId = java.util.UUID.fromString(updated["items"][0]["id"].asText()),
			version = updated["version"].asLong(),
		)

		order = changeOrderStatus(order.id, order.version, "IN_WORK", orderManager.token)
		order = changeOrderStatus(order.id, order.version, "READY", orderManager.token)
		order = changeOrderStatus(order.id, order.version, "SHIPPED", orderManager.token)
		assertEquals("SHIPPED", json(getJson("/api/orders/${order.id}", orderManager.token).assertOk())["status"].asText())

		putJson(
			path = "/api/orders/${order.id}",
			token = orderManager.token,
			body = mapOf(
				"expectedVersion" to order.version,
				"customerId" to customerId,
				"deliveryDate" to LocalDate.now().plusDays(11).toString(),
				"items" to listOf(mapOf("itemName" to "Locked", "quantity" to BigDecimal("1.0"), "uom" to "pcs")),
			),
		).assertStatus(400)

		postJson(
			path = "/api/orders",
			token = nonWriter.token,
			body = mapOf(
				"customerId" to customerId,
				"deliveryDate" to LocalDate.now().plusDays(5).toString(),
				"items" to listOf(mapOf("itemName" to "Forbidden", "quantity" to BigDecimal("1.0"), "uom" to "pcs")),
			),
		).assertForbidden()

		val invalidOrder = createOrderWithItem(token = orderManager.token, itemName = "Invalid transition item")
		postJson(
			path = "/api/orders/${invalidOrder.id}/status",
			token = orderManager.token,
			body = mapOf("expectedVersion" to invalidOrder.version, "toStatus" to "READY"),
		).assertStatus(422)
	}
}
