package com.ctfind.productioncontrol.integration

import com.ctfind.productioncontrol.production.application.OverdueTaskNotificationJob
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class NotificationsIntegrationTest : IntegrationTestSupport() {

	@Autowired
	lateinit var overdueJob: OverdueTaskNotificationJob

	@Test
	@DisplayName("Notifications integration: ${ScenarioCoverage.NOTIFICATIONS}")
	fun `production events notifications read state and user isolation are wired`() {
		val admin = createScenarioUser("admin.notifications", setOf("ADMIN"))
		val executor = createScenarioUser("executor.notifications.1", setOf("PRODUCTION_EXECUTOR"))
		val otherExecutor = createScenarioUser("executor.notifications.2", setOf("PRODUCTION_EXECUTOR"))

		val assignedTask = assignProductionTask(
			task = createProductionTaskFromOrder(createOrderWithItem(token = admin.token), token = admin.token),
			executorUserId = executor.id,
			token = admin.token,
		)

		var executorNotifications = json(getJson("/api/notifications", executor.token).assertOk())
		assertTrue(executorNotifications["items"].any { it["type"].asText() == "TASK_ASSIGNED" })
		val assignedNotificationId = executorNotifications["items"].first { it["type"].asText() == "TASK_ASSIGNED" }["id"].asText()
		val unreadBefore = json(getJson("/api/notifications/unread-count", executor.token).assertOk())["count"].asLong()
		assertTrue(unreadBefore >= 1)

		changeProductionTaskStatus(assignedTask, "IN_PROGRESS", executor.token)
		val adminNotifications = json(getJson("/api/notifications", admin.token).assertOk())
		assertTrue(adminNotifications["items"].any { it["type"].asText() == "STATUS_CHANGED" })

		val overdueTask = assignProductionTask(
			task = createProductionTaskFromOrder(createOrderWithItem(token = admin.token, itemName = "Overdue task item"), token = admin.token),
			executorUserId = executor.id,
			token = admin.token,
			plannedStartDate = LocalDate.now().minusDays(2),
			plannedFinishDate = LocalDate.now().minusDays(1),
		)
		overdueJob.checkOverdueTasks(LocalDate.now())
		overdueJob.checkOverdueTasks(LocalDate.now())
		executorNotifications = json(getJson("/api/notifications", executor.token).assertOk())
		val overdueNotifications = executorNotifications["items"].filter {
			it["type"].asText() == "TASK_OVERDUE" && it["targetEntityId"].asText() == overdueTask.id.toString()
		}
		assertEquals(1, overdueNotifications.size)

		val unreadBeforeRead = json(getJson("/api/notifications/unread-count", executor.token).assertOk())["count"].asLong()
		val readOnce = json(patchJson("/api/notifications/$assignedNotificationId/read", executor.token).assertOk())
		assertTrue(readOnce["read"].asBoolean())
		assertFalse(readOnce["readAt"].asText().isBlank())
		val readTwice = json(patchJson("/api/notifications/$assignedNotificationId/read", executor.token).assertOk())
		assertTrue(readTwice["read"].asBoolean())
		assertFalse(readTwice["readAt"].asText().isBlank())
		val unreadAfterOneRead = json(getJson("/api/notifications/unread-count", executor.token).assertOk())["count"].asLong()
		assertEquals(unreadBeforeRead - 1, unreadAfterOneRead)

		patchJson("/api/notifications/$assignedNotificationId/read", otherExecutor.token).assertStatus(404)
		val otherNotifications = json(getJson("/api/notifications", otherExecutor.token).assertOk())
		assertFalse(otherNotifications["items"].any { it["id"].asText() == assignedNotificationId })

		json(postJson("/api/notifications/mark-all-read", emptyMap<String, String>(), executor.token).assertOk())
		assertEquals(0, json(getJson("/api/notifications/unread-count", executor.token).assertOk())["count"].asLong())
	}
}
