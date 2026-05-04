package com.ctfind.productioncontrol.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ProductionTaskLifecycleIntegrationTest : IntegrationTestSupport() {

	@Test
	@DisplayName("Production task integration: ${ScenarioCoverage.PRODUCTION_TASKS}")
	fun `order task assignment lifecycle history visibility and conflict are wired`() {
		val admin = createScenarioUser("admin.production", setOf("ADMIN"))
		val supervisor = createScenarioUser("supervisor.production", setOf("PRODUCTION_SUPERVISOR"))
		val executor = createScenarioUser("executor.production.1", setOf("PRODUCTION_EXECUTOR"))
		val otherExecutor = createScenarioUser("executor.production.2", setOf("PRODUCTION_EXECUTOR"))

		val order = createOrderWithItem(token = admin.token)
		var task = createProductionTaskFromOrder(order = order, token = admin.token)

		val supervisorList = json(getJson("/api/production-tasks", supervisor.token).assertOk())
		assertTrue(supervisorList["items"].any { it["id"].asText() == task.id.toString() })

		task = assignProductionTask(task = task, executorUserId = executor.id, token = admin.token)

		putJson(
			path = "/api/production-tasks/${task.id}/assignment",
			token = admin.token,
			body = mapOf(
				"expectedVersion" to 0,
				"executorUserId" to executor.id,
				"plannedStartDate" to java.time.LocalDate.now().toString(),
				"plannedFinishDate" to java.time.LocalDate.now().plusDays(4).toString(),
			),
		).assertConflict()

		val executorList = json(getJson("/api/production-tasks", executor.token).assertOk())
		assertEquals(listOf(task.id.toString()), executorList["items"].map { it["id"].asText() })

		var current = changeProductionTaskStatus(task, "IN_PROGRESS", executor.token)
		current = changeProductionTaskStatus(current, "BLOCKED", executor.token, reason = "Waiting material")
		current = changeProductionTaskStatus(current, "IN_PROGRESS", executor.token)
		current = changeProductionTaskStatus(current, "COMPLETED", executor.token)

		val detail = json(getJson("/api/production-tasks/${current.id}", supervisor.token).assertOk())
		assertEquals("COMPLETED", detail["status"].asText())
		val historyTypes = detail["history"].map { it["type"].asText() }.toSet()
		assertTrue(historyTypes.containsAll(setOf("CREATED", "ASSIGNED", "STATUS_CHANGED", "BLOCKED", "UNBLOCKED", "COMPLETED")))

		val otherOrder = createOrderWithItem(token = admin.token, itemName = "Other integration item")
		val otherTask = assignProductionTask(
			task = createProductionTaskFromOrder(order = otherOrder, token = admin.token),
			executorUserId = otherExecutor.id,
			token = admin.token,
		)
		getJson("/api/production-tasks/${otherTask.id}", executor.token).assertForbidden()
		postJson(
			path = "/api/production-tasks/${otherTask.id}/status",
			token = executor.token,
			body = mapOf("expectedVersion" to otherTask.version, "toStatus" to "IN_PROGRESS"),
		).assertForbidden()

		getJson("/api/production-tasks/${current.id}").assertUnauthorized()
		postJson(
			path = "/api/production-tasks/${current.id}/status",
			body = mapOf("expectedVersion" to current.version, "toStatus" to "IN_PROGRESS"),
		).assertUnauthorized()
	}
}
