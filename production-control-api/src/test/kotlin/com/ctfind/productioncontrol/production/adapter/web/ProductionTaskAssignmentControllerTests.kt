package com.ctfind.productioncontrol.production.adapter.web

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.production.application.ProductionTaskExecutorSummary
import com.ctfind.productioncontrol.production.application.ProductionTaskMutationResult
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProductionTaskAssignmentControllerTests {

	private val taskId = UUID.fromString("60000000-0000-0000-0000-000000000006")
	private val actorId = UUID.fromString("30000000-0000-0000-0000-000000000003")
	private val executorId = UUID.fromString("80000000-0000-0000-0000-000000000008")

	@Test
	fun `PUT assignment success returns 200 detail response`() {
		val controller = controllerWith(
			assign = stubAssignUseCase { ProductionTaskMutationResult.Success(Unit) },
			query = stubQueryReturning(sampleDetailView(taskId = taskId, version = 1)),
		)

		val response = controller.putAssignment(
			id = taskId,
			body = sampleAssignmentRequest(),
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)

		assertEquals(HttpStatus.OK, response.statusCode)
		val body = assertIs<ProductionTaskDetailResponse>(response.body)
		assertEquals(taskId, body.id)
		assertEquals(1, body.version)
	}

	@Test
	fun `PUT assignment forbidden returns 403`() {
		val controller = controllerWith(
			assign = stubAssignUseCase { ProductionTaskMutationResult.Forbidden },
		)

		val response = controller.putAssignment(taskId, sampleAssignmentRequest(), jwtFor(actorId, setOf("VIEWER")))

		assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
		val err = assertIs<ProductionTaskApiErrorResponse>(response.body)
		assertEquals("forbidden", err.code)
	}

	@Test
	fun `PUT assignment stale version returns 409`() {
		val controller = controllerWith(
			assign = stubAssignUseCase { ProductionTaskMutationResult.StaleVersion },
		)

		val response = controller.putAssignment(taskId, sampleAssignmentRequest(), jwtFor(actorId, setOf(ADMIN_ROLE_CODE)))

		assertEquals(HttpStatus.CONFLICT, response.statusCode)
		val err = assertIs<ProductionTaskApiErrorResponse>(response.body)
		assertEquals("stale_production_task_version", err.code)
	}

	@Test
	fun `PUT assignment validation failure returns 400`() {
		val controller = controllerWith(
			assign = stubAssignUseCase {
				ProductionTaskMutationResult.ValidationFailed(
					message = "Executor is not active.",
					errorCode = "validation_failed",
					details = mapOf("executorUserId" to executorId.toString()),
				)
			},
		)

		val response = controller.putAssignment(taskId, sampleAssignmentRequest(), jwtFor(actorId, setOf(ADMIN_ROLE_CODE)))

		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		val err = assertIs<ProductionTaskApiErrorResponse>(response.body)
		assertEquals("validation_failed", err.code)
		assertEquals(executorId.toString(), err.details["executorUserId"])
	}

	@Test
	fun `GET assignees forbidden returns 403 for users without permission`() {
		val controller = controllerWith(
			assignees = stubAssigneeQuery(emptyList()),
		)

		val response = controller.listAssignees(search = null, limit = 20, jwt = jwtFor(actorId, setOf("VIEWER")))

		assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
		val err = assertIs<ProductionTaskApiErrorResponse>(response.body)
		assertEquals("forbidden", err.code)
	}

	@Test
	fun `GET assignees returns 200 with mapped items for authorized user`() {
		val controller = controllerWith(
			assignees = stubAssigneeQuery(
				listOf(
					ProductionTaskExecutorSummary(executorId, "Иван Исполнитель", "worker1"),
				),
			),
		)

		val response = controller.listAssignees(search = "ив", limit = 20, jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)))

		assertEquals(HttpStatus.OK, response.statusCode)
		val body = assertIs<ProductionTaskAssigneesResponse>(response.body)
		assertEquals(1, body.items.size)
		val item = body.items.single()
		assertEquals(executorId, item.id)
		assertEquals("Иван Исполнитель", item.displayName)
		assertEquals("worker1", item.login)
	}

	private fun sampleAssignmentRequest(): PutProductionTaskAssignmentRequest =
		PutProductionTaskAssignmentRequest(
			expectedVersion = 0,
			executorUserId = executorId,
			plannedStartDate = LocalDate.parse("2026-05-01"),
			plannedFinishDate = LocalDate.parse("2026-05-03"),
			note = null,
		)
}
