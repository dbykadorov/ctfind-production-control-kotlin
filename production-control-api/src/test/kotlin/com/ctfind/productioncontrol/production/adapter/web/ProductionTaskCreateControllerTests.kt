package com.ctfind.productioncontrol.production.adapter.web

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.production.application.CreateProductionTasksFromOrderCommand
import com.ctfind.productioncontrol.production.application.CreatedProductionTaskSummary
import com.ctfind.productioncontrol.production.application.ProductionTaskMutationResult
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ProductionTaskCreateControllerTests {

	private val orderId = UUID.fromString("10000000-0000-0000-0000-000000000001")
	private val itemId = UUID.fromString("20000000-0000-0000-0000-000000000002")
	private val actorId = UUID.fromString("30000000-0000-0000-0000-000000000003")
	private val createdTaskId = UUID.fromString("60000000-0000-0000-0000-000000000006")

	@Test
	fun `success returns 201 with created task summaries`() {
		val controller = controllerWith(
			create = stubCreateUseCase {
				ProductionTaskMutationResult.Success(
					listOf(
						CreatedProductionTaskSummary(
							id = createdTaskId,
							taskNumber = "PT-000001",
							status = ProductionTaskStatus.NOT_STARTED,
							version = 0,
						),
					),
				)
			},
		)

		val response = controller.createFromOrder(sampleRequest(), jwtFor(actorId, setOf(ADMIN_ROLE_CODE)))

		assertEquals(HttpStatus.CREATED, response.statusCode)
		val body = assertIs<CreateProductionTasksFromOrderResponse>(response.body)
		assertEquals(1, body.items.size)
		assertEquals("PT-000001", body.items.single().taskNumber)
	}

	@Test
	fun `forbidden returns 403`() {
		val controller = controllerWith(
			create = stubCreateUseCase { ProductionTaskMutationResult.Forbidden },
		)

		val response = controller.createFromOrder(sampleRequest(), jwtFor(actorId, setOf("VIEWER")))

		assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
		val err = assertIs<ProductionTaskApiErrorResponse>(response.body)
		assertEquals("forbidden", err.code)
	}

	@Test
	fun `validation_failed returns 400 with error code and details`() {
		val controller = controllerWith(
			create = stubCreateUseCase {
				ProductionTaskMutationResult.ValidationFailed(
					message = "Order item not found.",
					errorCode = "order_item_not_found",
					details = mapOf("orderItemId" to itemId.toString()),
				)
			},
		)

		val response = controller.createFromOrder(sampleRequest(), jwtFor(actorId, setOf(ADMIN_ROLE_CODE)))

		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		val err = assertIs<ProductionTaskApiErrorResponse>(response.body)
		assertEquals("order_item_not_found", err.code)
		assertEquals(itemId.toString(), err.details["orderItemId"])
	}

	@Test
	fun `command is built from request body and jwt actor`() {
		var captured: CreateProductionTasksFromOrderCommand? = null
		val controller = controllerWith(
			create = stubCreateUseCase { cmd ->
				captured = cmd
				ProductionTaskMutationResult.Success(emptyList())
			},
		)

		controller.createFromOrder(sampleRequest(), jwtFor(actorId, setOf(ADMIN_ROLE_CODE)))

		val cmd = assertNotNull(captured)
		assertEquals(orderId, cmd.orderId)
		assertEquals(actorId, cmd.actorUserId)
		assertEquals(setOf(ADMIN_ROLE_CODE), cmd.roleCodes)
		assertEquals(itemId, cmd.tasks.single().orderItemId)
		assertEquals("Раскрой", cmd.tasks.single().purpose)
	}

	private fun sampleRequest(): CreateProductionTasksFromOrderRequest =
		CreateProductionTasksFromOrderRequest(
			orderId = orderId,
			tasks = listOf(
				CreateProductionTaskFromOrderItemRequest(
					orderItemId = itemId,
					purpose = "Раскрой",
					quantity = BigDecimal("2"),
					uom = "шт",
					executorUserId = null,
					plannedStartDate = LocalDate.parse("2026-05-01"),
					plannedFinishDate = LocalDate.parse("2026-05-03"),
				),
			),
		)
}
