package com.ctfind.productioncontrol.integration

import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ScenarioUser(
	val id: UUID,
	val login: String,
	val password: String,
	val roleCodes: Set<String>,
	val token: String,
)

data class ScenarioOrder(
	val id: UUID,
	val customerId: UUID,
	val itemId: UUID,
	val version: Long,
)

data class ScenarioTask(
	val id: UUID,
	val version: Long,
)

data class ScenarioMaterial(
	val id: UUID,
	val versionlessName: String,
)

fun IntegrationTestSupport.createScenarioUser(
	login: String,
	roleCodes: Set<String>,
	password: String = "Password-123",
): ScenarioUser {
	val result = postJson(
		path = "/api/users",
		token = adminToken(),
		body = mapOf(
			"login" to login,
			"displayName" to login.replace('.', ' ').replaceFirstChar { it.uppercase() },
			"initialPassword" to password,
			"roleCodes" to roleCodes,
		),
	).assertCreated()
	val body = json(result)
	return ScenarioUser(
		id = UUID.fromString(body["id"].asText()),
		login = login,
		password = password,
		roleCodes = roleCodes,
		token = this.login(login, password),
	)
}

fun IntegrationTestSupport.insertCustomer(name: String = "Integration Customer"): UUID {
	val id = UUID.randomUUID()
	val now = Timestamp.from(Instant.now())
	jdbc.update(
		"""
		INSERT INTO customer (id, display_name, status, contact_person, phone, email, created_at, updated_at)
		VALUES (?, ?, 'ACTIVE', ?, ?, ?, ?, ?)
		""".trimIndent(),
		id,
		name,
		"Integration Contact",
		"+7 000 000-00-00",
		"integration@example.test",
		now,
		now,
	)
	return id
}

fun IntegrationTestSupport.createOrderWithItem(
	token: String = adminToken(),
	customerId: UUID = insertCustomer(),
	itemName: String = "Integration Item",
	quantity: String = "5.0",
	uom: String = "pcs",
	deliveryDate: LocalDate = LocalDate.now().plusDays(7),
): ScenarioOrder {
	val result = postJson(
		path = "/api/orders",
		token = token,
		body = mapOf(
			"customerId" to customerId,
			"deliveryDate" to deliveryDate.toString(),
			"notes" to "integration order",
			"items" to listOf(
				mapOf(
					"itemName" to itemName,
					"quantity" to BigDecimal(quantity),
					"uom" to uom,
				),
			),
		),
	).assertCreated()
	val body = json(result)
	return ScenarioOrder(
		id = UUID.fromString(body["id"].asText()),
		customerId = customerId,
		itemId = UUID.fromString(body["items"][0]["id"].asText()),
		version = body["version"].asLong(),
	)
}

fun IntegrationTestSupport.changeOrderStatus(
	orderId: UUID,
	expectedVersion: Long,
	toStatus: String,
	token: String = adminToken(),
): ScenarioOrder {
	val result = postJson(
		path = "/api/orders/$orderId/status",
		token = token,
		body = mapOf(
			"expectedVersion" to expectedVersion,
			"toStatus" to toStatus,
			"note" to "integration status change",
		),
	).assertOk()
	val body = json(result)
	return ScenarioOrder(
		id = UUID.fromString(body["id"].asText()),
		customerId = UUID.fromString(body["customer"]["id"].asText()),
		itemId = UUID.fromString(body["items"][0]["id"].asText()),
		version = body["version"].asLong(),
	)
}

fun IntegrationTestSupport.createProductionTaskFromOrder(
	order: ScenarioOrder,
	token: String = adminToken(),
	executorUserId: UUID? = null,
	plannedFinishDate: LocalDate? = null,
): ScenarioTask {
	val draft = mutableMapOf<String, Any?>(
		"orderItemId" to order.itemId,
		"purpose" to "Integration production task",
		"quantity" to BigDecimal("2.0"),
		"uom" to "pcs",
	)
	if (executorUserId != null) draft["executorUserId"] = executorUserId
	if (plannedFinishDate != null) draft["plannedFinishDate"] = plannedFinishDate.toString()
	val result = postJson(
		path = "/api/production-tasks/from-order",
		token = token,
		body = mapOf(
			"orderId" to order.id,
			"tasks" to listOf(draft),
		),
	).assertCreated()
	val item = json(result)["items"][0]
	return ScenarioTask(
		id = UUID.fromString(item["id"].asText()),
		version = item["version"].asLong(),
	)
}

fun IntegrationTestSupport.assignProductionTask(
	task: ScenarioTask,
	executorUserId: UUID,
	token: String = adminToken(),
	plannedStartDate: LocalDate = LocalDate.now(),
	plannedFinishDate: LocalDate = LocalDate.now().plusDays(3),
): ScenarioTask {
	val result = putJson(
		path = "/api/production-tasks/${task.id}/assignment",
		token = token,
		body = mapOf(
			"expectedVersion" to task.version,
			"executorUserId" to executorUserId,
			"plannedStartDate" to plannedStartDate.toString(),
			"plannedFinishDate" to plannedFinishDate.toString(),
			"note" to "integration assignment",
		),
	).assertOk()
	val body = json(result)
	return ScenarioTask(task.id, body["version"].asLong())
}

fun IntegrationTestSupport.changeProductionTaskStatus(
	task: ScenarioTask,
	toStatus: String,
	token: String,
	reason: String? = null,
): ScenarioTask {
	val body = mutableMapOf<String, Any?>(
		"expectedVersion" to task.version,
		"toStatus" to toStatus,
		"note" to "integration status change",
	)
	if (reason != null) body["reason"] = reason
	val result = postJson(
		path = "/api/production-tasks/${task.id}/status",
		token = token,
		body = body,
	).assertOk()
	return ScenarioTask(task.id, json(result)["version"].asLong())
}

fun IntegrationTestSupport.createMaterial(
	token: String,
	name: String = "Integration material ${UUID.randomUUID()}",
	unit: String = "PIECE",
): ScenarioMaterial {
	val result = postJson(
		path = "/api/materials",
		token = token,
		body = mapOf("name" to name, "unit" to unit),
	).assertCreated()
	val body = json(result)
	return ScenarioMaterial(UUID.fromString(body["id"].asText()), body["name"].asText())
}
