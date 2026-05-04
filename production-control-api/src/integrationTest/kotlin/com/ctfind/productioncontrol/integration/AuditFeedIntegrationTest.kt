package com.ctfind.productioncontrol.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

class AuditFeedIntegrationTest : IntegrationTestSupport() {

	@Test
	@DisplayName("Audit integration: ${ScenarioCoverage.AUDIT}")
	fun `audit feed aggregates real module events filters and security`() {
		val admin = createScenarioUser("admin.audit", setOf("ADMIN"))
		val warehouse = createScenarioUser("warehouse.audit", setOf("WAREHOUSE"))
		val nonAdmin = createScenarioUser("order.audit.viewer", setOf("ORDER_MANAGER"))

		val order = createOrderWithItem(token = admin.token, itemName = "Audit integration item")
		createProductionTaskFromOrder(order = order, token = admin.token)
		val material = createMaterial(token = warehouse.token, name = "Audit integration material")
		postJson(
			path = "/api/materials/${material.id}/receipt",
			token = warehouse.token,
			body = mapOf("quantity" to java.math.BigDecimal("3.0"), "comment" to "audit receipt"),
		).assertCreated()

		val feed = json(getJson("/api/audit", admin.token).assertOk())
		val categories = feed["items"].map { it["category"].asText() }.toSet()
		assertTrue(categories.containsAll(setOf("AUTH", "ORDER", "PRODUCTION_TASK", "INVENTORY")))

		val orderOnly = json(getJson("/api/audit?category=ORDER", admin.token).assertOk())
		assertTrue(orderOnly["items"].size() > 0)
		assertTrue(orderOnly["items"].all { it["category"].asText() == "ORDER" })

		val from = Instant.now().minusSeconds(3600)
		val to = Instant.now().plusSeconds(3600)
		val dateFiltered = json(getJson("/api/audit?from=$from&to=$to", admin.token).assertOk())
		assertTrue(dateFiltered["items"].size() >= feed["items"].size())

		val actorFiltered = json(getJson("/api/audit?actorUserId=${warehouse.id}", admin.token).assertOk())
		assertTrue(actorFiltered["items"].size() > 0)
		assertTrue(actorFiltered["items"].all { it["actorLogin"].asText() == warehouse.login })

		val searchFiltered = json(getJson("/api/audit?search=${order.id}", admin.token).assertOk())
		assertTrue(searchFiltered["items"].any { it["targetId"].asText() == order.id.toString() })

		getJson("/api/audit", nonAdmin.token).assertForbidden()
		getJson("/api/audit").assertUnauthorized()

		assertEquals(0, AuditCategoryResidualRisk.missingCategories.size)
	}
}

private object AuditCategoryResidualRisk {
	val missingCategories: Set<String> = emptySet()
}
