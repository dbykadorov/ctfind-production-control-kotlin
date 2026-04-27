package com.ctfind.productioncontrol.orders.adapter.web

import com.ctfind.productioncontrol.orders.OrderTestFixtures
import com.ctfind.productioncontrol.orders.application.ChangeOrderStatusUseCase
import com.ctfind.productioncontrol.orders.application.CustomerSummary
import com.ctfind.productioncontrol.orders.application.OrderDetailView
import com.ctfind.productioncontrol.orders.application.OrderMutationResult
import com.ctfind.productioncontrol.orders.application.ORDER_MANAGER_ROLE_CODE
import com.ctfind.productioncontrol.orders.domain.CustomerStatus
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderControllerStatusTests {

	@Test
	fun `status endpoint returns updated order for order manager`() {
		val controller = OrderController(
			orderQuery = oneOrderQueryUseCase(),
			changeOrderStatus = ChangeOrderStatusUseCase { command ->
				OrderMutationResult.Success(
					OrderDetailView(
						id = command.orderId,
						orderNumber = "ORD-000001",
						customer = CustomerSummary(OrderTestFixtures.customerId, "ООО Ромашка", CustomerStatus.ACTIVE),
						deliveryDate = java.time.LocalDate.parse("2026-05-15"),
						status = command.toStatus,
						notes = null,
						items = emptyList(),
						history = emptyList(),
						changeDiffs = emptyList(),
						createdAt = Instant.parse("2026-04-26T18:00:00Z"),
						updatedAt = Instant.parse("2026-04-26T19:00:00Z"),
						version = command.expectedVersion + 1,
						overdue = false,
					),
				)
			},
		)

		val response = controller.changeStatus(
			OrderTestFixtures.orderId,
			ChangeOrderStatusRequest(expectedVersion = 0, toStatus = OrderStatus.IN_WORK, note = "Started"),
			statusJwtWithRoles(ORDER_MANAGER_ROLE_CODE),
		)

		assertEquals(HttpStatus.OK, response.statusCode)
		assertEquals(OrderStatus.IN_WORK, (response.body as OrderDetailResponse).status)
	}

	@Test
	fun `status endpoint maps stale version to 409`() {
		val controller = OrderController(
			orderQuery = oneOrderQueryUseCase(),
			changeOrderStatus = ChangeOrderStatusUseCase { OrderMutationResult.StaleVersion },
		)

		val response = controller.changeStatus(
			OrderTestFixtures.orderId,
			ChangeOrderStatusRequest(expectedVersion = 0, toStatus = OrderStatus.IN_WORK),
			statusJwtWithRoles(ORDER_MANAGER_ROLE_CODE),
		)

		assertEquals(HttpStatus.CONFLICT, response.statusCode)
	}

	@Test
	fun `status endpoint maps invalid transition to 422`() {
		val controller = OrderController(
			orderQuery = oneOrderQueryUseCase(),
			changeOrderStatus = ChangeOrderStatusUseCase { OrderMutationResult.InvalidTransition },
		)

		val response = controller.changeStatus(
			OrderTestFixtures.orderId,
			ChangeOrderStatusRequest(expectedVersion = 0, toStatus = OrderStatus.READY),
			statusJwtWithRoles(ORDER_MANAGER_ROLE_CODE),
		)

		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.statusCode)
	}

	@Test
	fun `status endpoint maps forbidden result to 403`() {
		val controller = OrderController(
			orderQuery = oneOrderQueryUseCase(),
			changeOrderStatus = ChangeOrderStatusUseCase { OrderMutationResult.Forbidden },
		)

		val response = controller.changeStatus(
			OrderTestFixtures.orderId,
			ChangeOrderStatusRequest(expectedVersion = 0, toStatus = OrderStatus.IN_WORK),
			statusJwtWithRoles("EXECUTOR"),
		)

		assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
	}
}

private fun statusJwtWithRoles(vararg roles: String): Jwt =
	Jwt.withTokenValue("jwt")
		.header("alg", "HS256")
		.subject("manager")
		.claim("userId", OrderTestFixtures.actorUserId.toString())
		.claim("displayName", "Manager")
		.claim("roles", roles.toList())
		.issuedAt(Instant.parse("2026-04-26T18:00:00Z"))
		.expiresAt(Instant.parse("2026-04-27T02:00:00Z"))
		.build()
