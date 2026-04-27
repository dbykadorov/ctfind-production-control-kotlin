package com.ctfind.productioncontrol.orders.adapter.web

import com.ctfind.productioncontrol.orders.OrderTestFixtures
import com.ctfind.productioncontrol.orders.application.CustomerSummary
import com.ctfind.productioncontrol.orders.application.OrderDetailView
import com.ctfind.productioncontrol.orders.application.OrderMutationResult
import com.ctfind.productioncontrol.orders.application.ORDER_MANAGER_ROLE_CODE
import com.ctfind.productioncontrol.orders.application.UpdateOrderUseCase
import com.ctfind.productioncontrol.orders.domain.CustomerStatus
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderControllerUpdateTests {

	@Test
	fun `update endpoint returns saved order for order manager`() {
		val controller = OrderController(
			orderQuery = oneOrderQueryUseCase(),
			updateOrder = UpdateOrderUseCase { command ->
				OrderMutationResult.Success(
					OrderDetailView(
						id = command.orderId,
						orderNumber = "ORD-000001",
						customer = CustomerSummary(command.customerId, "ООО Ромашка", CustomerStatus.ACTIVE),
						deliveryDate = command.deliveryDate,
						status = OrderStatus.NEW,
						notes = command.notes,
						items = emptyList(),
						history = emptyList(),
						changeDiffs = emptyList(),
						createdAt = Instant.parse("2026-04-26T18:00:00Z"),
						updatedAt = Instant.parse("2026-04-26T18:30:00Z"),
						version = command.expectedVersion + 1,
						overdue = false,
					),
				)
			},
		)

		val response = controller.update(
			OrderTestFixtures.orderId,
			UpdateOrderRequest(
				expectedVersion = 0,
				customerId = OrderTestFixtures.customerId,
				deliveryDate = LocalDate.parse("2026-05-20"),
				notes = "Updated",
				items = listOf(OrderItemRequest("Фасад", BigDecimal.ONE, "шт")),
			),
			jwtWithRoles(ORDER_MANAGER_ROLE_CODE),
		)

		assertEquals(HttpStatus.OK, response.statusCode)
		assertEquals(1, (response.body as OrderDetailResponse).version)
	}

	@Test
	fun `update endpoint maps stale version to 409`() {
		val controller = OrderController(
			orderQuery = oneOrderQueryUseCase(),
			updateOrder = UpdateOrderUseCase { OrderMutationResult.StaleVersion },
		)

		val response = controller.update(
			OrderTestFixtures.orderId,
			UpdateOrderRequest(
				expectedVersion = 0,
				customerId = OrderTestFixtures.customerId,
				deliveryDate = LocalDate.parse("2026-05-20"),
				items = listOf(OrderItemRequest("Фасад", BigDecimal.ONE, "шт")),
			),
			jwtWithRoles(ORDER_MANAGER_ROLE_CODE),
		)

		assertEquals(HttpStatus.CONFLICT, response.statusCode)
	}

	@Test
	fun `update endpoint maps forbidden result to 403`() {
		val controller = OrderController(
			orderQuery = oneOrderQueryUseCase(),
			updateOrder = UpdateOrderUseCase { OrderMutationResult.Forbidden },
		)

		val response = controller.update(
			OrderTestFixtures.orderId,
			UpdateOrderRequest(
				expectedVersion = 0,
				customerId = OrderTestFixtures.customerId,
				deliveryDate = LocalDate.parse("2026-05-20"),
				items = listOf(OrderItemRequest("Фасад", BigDecimal.ONE, "шт")),
			),
			jwtWithRoles("EXECUTOR"),
		)

		assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
	}
}

private fun jwtWithRoles(vararg roles: String): Jwt =
	Jwt.withTokenValue("jwt")
		.header("alg", "HS256")
		.subject("manager")
		.claim("userId", OrderTestFixtures.actorUserId.toString())
		.claim("displayName", "Manager")
		.claim("roles", roles.toList())
		.issuedAt(Instant.parse("2026-04-26T18:00:00Z"))
		.expiresAt(Instant.parse("2026-04-27T02:00:00Z"))
		.build()
