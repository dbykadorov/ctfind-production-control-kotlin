package com.ctfind.productioncontrol.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class WarehouseConsumptionIntegrationTest : IntegrationTestSupport() {

	@Test
	@DisplayName("Warehouse integration: ${ScenarioCoverage.WAREHOUSE}")
	fun `materials bom consumption usage locks and role checks are wired`() {
		val admin = createScenarioUser("admin.warehouse", setOf("ADMIN"))
		val warehouse = createScenarioUser("warehouse.integration", setOf("WAREHOUSE"))
		val unauthorized = createScenarioUser("executor.warehouse", setOf("PRODUCTION_EXECUTOR"))

		val material = createMaterial(token = warehouse.token, name = "Integration steel")
		postJson(
			path = "/api/materials/${material.id}/receipt",
			token = warehouse.token,
			body = mapOf("quantity" to BigDecimal("25.0"), "comment" to "integration receipt"),
		).assertCreated()

		val order = createOrderWithItem(token = admin.token)
		val bom = json(
			postJson(
				path = "/api/orders/${order.id}/bom",
				token = admin.token,
				body = mapOf(
					"materialId" to material.id,
					"quantity" to BigDecimal("10.0"),
					"comment" to "integration bom",
				),
			).assertCreated(),
		)
		val bomLineId = bom["id"].asText()

		postJson(
			path = "/api/materials/${material.id}/consume",
			token = warehouse.token,
			body = mapOf(
				"orderId" to order.id,
				"quantity" to BigDecimal("4.0"),
				"comment" to "integration consumption",
			),
		).assertCreated()

		val usage = json(getJson("/api/orders/${order.id}/material-usage", warehouse.token).assertOk())
		val usageRow = usage["rows"].single()
		assertDecimalEquals("10.0", usageRow["requiredQuantity"].decimalValue())
		assertDecimalEquals("4.0", usageRow["consumedQuantity"].decimalValue())
		assertDecimalEquals("6.0", usageRow["remainingToConsume"].decimalValue())

		postJson(
			path = "/api/materials/${material.id}/consume",
			token = warehouse.token,
			body = mapOf("orderId" to order.id, "quantity" to BigDecimal("100.0")),
		).assertConflict()
		val usageAfterFailure = json(getJson("/api/orders/${order.id}/material-usage", warehouse.token).assertOk())
		assertDecimalEquals("4.0", usageAfterFailure["rows"].single()["consumedQuantity"].decimalValue())

		var shipped = changeOrderStatus(order.id, order.version, "IN_WORK", admin.token)
		shipped = changeOrderStatus(shipped.id, shipped.version, "READY", admin.token)
		shipped = changeOrderStatus(shipped.id, shipped.version, "SHIPPED", admin.token)

		putJson(
			path = "/api/orders/${shipped.id}/bom/$bomLineId",
			token = admin.token,
			body = mapOf("quantity" to BigDecimal("11.0"), "comment" to "locked"),
		).assertConflict()
		postJson(
			path = "/api/materials/${material.id}/consume",
			token = warehouse.token,
			body = mapOf("orderId" to shipped.id, "quantity" to BigDecimal("1.0")),
		).assertConflict()

		createMaterial(token = admin.token, name = "Admin-created integration material")
		postJson(
			path = "/api/materials",
			token = unauthorized.token,
			body = mapOf("name" to "Forbidden material", "unit" to "PIECE"),
		).assertForbidden()
		postJson(
			path = "/api/materials/${material.id}/receipt",
			token = unauthorized.token,
			body = mapOf("quantity" to BigDecimal("1.0")),
		).assertForbidden()
	}

	private fun assertDecimalEquals(expected: String, actual: BigDecimal) {
		assertEquals(0, BigDecimal(expected).compareTo(actual))
	}
}
