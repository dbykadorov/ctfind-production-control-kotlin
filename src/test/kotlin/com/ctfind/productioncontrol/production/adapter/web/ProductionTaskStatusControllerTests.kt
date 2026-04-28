package com.ctfind.productioncontrol.production.adapter.web

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.production.application.ProductionTaskMutationResult
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import org.springframework.http.HttpStatus
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProductionTaskStatusControllerTests {

	private val taskId = UUID.fromString("60000000-0000-0000-0000-000000000006")
	private val actorId = UUID.fromString("30000000-0000-0000-0000-000000000003")

	@Test
	fun `POST status success returns 200 detail response`() {
		val controller = controllerWith(
			changeStatus = stubChangeStatusUseCase { ProductionTaskMutationResult.Success(Unit) },
			query = stubQueryReturning(sampleDetailView(taskId = taskId, status = ProductionTaskStatus.IN_PROGRESS, version = 2)),
		)

		val response = controller.postStatus(
			id = taskId,
			body = PostProductionTaskStatusRequest(expectedVersion = 1, toStatus = ProductionTaskStatus.IN_PROGRESS),
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)

		assertEquals(HttpStatus.OK, response.statusCode)
		val body = assertIs<ProductionTaskDetailResponse>(response.body)
		assertEquals(ProductionTaskStatus.IN_PROGRESS, body.status)
		assertEquals(2, body.version)
	}

	@Test
	fun `POST status forbidden returns 403`() {
		val controller = controllerWith(
			changeStatus = stubChangeStatusUseCase { ProductionTaskMutationResult.Forbidden },
		)

		val response = controller.postStatus(
			id = taskId,
			body = PostProductionTaskStatusRequest(expectedVersion = 0, toStatus = ProductionTaskStatus.IN_PROGRESS),
			jwt = jwtFor(actorId, setOf("VIEWER")),
		)

		assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
		val err = assertIs<ProductionTaskApiErrorResponse>(response.body)
		assertEquals("forbidden", err.code)
	}

	@Test
	fun `POST status stale version returns 409`() {
		val controller = controllerWith(
			changeStatus = stubChangeStatusUseCase { ProductionTaskMutationResult.StaleVersion },
		)

		val response = controller.postStatus(
			id = taskId,
			body = PostProductionTaskStatusRequest(expectedVersion = 0, toStatus = ProductionTaskStatus.IN_PROGRESS),
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)

		assertEquals(HttpStatus.CONFLICT, response.statusCode)
		val err = assertIs<ProductionTaskApiErrorResponse>(response.body)
		assertEquals("stale_production_task_version", err.code)
	}

	@Test
	fun `POST status invalid transition returns 422`() {
		val controller = controllerWith(
			changeStatus = stubChangeStatusUseCase { ProductionTaskMutationResult.InvalidTransition },
		)

		val response = controller.postStatus(
			id = taskId,
			body = PostProductionTaskStatusRequest(expectedVersion = 0, toStatus = ProductionTaskStatus.COMPLETED),
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)

		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.statusCode)
		val err = assertIs<ProductionTaskApiErrorResponse>(response.body)
		assertEquals("invalid_task_status_transition", err.code)
	}

	@Test
	fun `POST status validation failure returns 400`() {
		val controller = controllerWith(
			changeStatus = stubChangeStatusUseCase {
				ProductionTaskMutationResult.ValidationFailed(
					message = "Block reason is required.",
					errorCode = "validation_failed",
				)
			},
		)

		val response = controller.postStatus(
			id = taskId,
			body = PostProductionTaskStatusRequest(expectedVersion = 0, toStatus = ProductionTaskStatus.BLOCKED),
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)

		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		val err = assertIs<ProductionTaskApiErrorResponse>(response.body)
		assertEquals("validation_failed", err.code)
	}
}
