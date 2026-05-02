package com.ctfind.productioncontrol.orders.adapter.web

import com.ctfind.productioncontrol.orders.OrderTestFixtures
import com.ctfind.productioncontrol.orders.application.CreateOrderUseCase
import com.ctfind.productioncontrol.orders.application.OrderMutationResult
import com.ctfind.productioncontrol.orders.application.ORDER_MANAGER_ROLE_CODE
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderControllerCreateTests {

	@Test
	fun `create endpoint returns created order for order manager`() {
		val controller = OrderController(
			orderQuery = oneOrderQueryUseCase(),
			createOrder = CreateOrderUseCase { command ->
				OrderMutationResult.Success(
					com.ctfind.productioncontrol.orders.application.OrderDetailView(
						id = OrderTestFixtures.orderId,
						orderNumber = "ORD-000123",
						customer = com.ctfind.productioncontrol.orders.application.CustomerSummary(
							id = command.customerId,
							displayName = "ООО Ромашка",
							status = com.ctfind.productioncontrol.orders.domain.CustomerStatus.ACTIVE,
						),
						deliveryDate = command.deliveryDate,
						status = OrderStatus.NEW,
						notes = command.notes,
						items = emptyList(),
						history = emptyList(),
						changeDiffs = emptyList(),
						createdAt = Instant.parse("2026-04-26T18:00:00Z"),
						updatedAt = Instant.parse("2026-04-26T18:00:00Z"),
						version = 0,
						overdue = false,
					),
				)
			},
		)

		val response = controller.create(
			CreateOrderRequest(
				customerId = OrderTestFixtures.customerId,
				deliveryDate = LocalDate.parse("2026-05-15"),
				notes = "New",
				items = listOf(OrderItemRequest("Столешница", BigDecimal.ONE, "шт")),
			),
			jwtWithRoles(ORDER_MANAGER_ROLE_CODE),
		)

		assertEquals(HttpStatus.CREATED, response.statusCode)
		assertEquals("ORD-000123", (response.body as OrderDetailResponse).orderNumber)
	}

	@Test
	fun `create endpoint maps forbidden result to 403`() {
		val controller = OrderController(
			orderQuery = oneOrderQueryUseCase(),
			createOrder = CreateOrderUseCase { OrderMutationResult.Forbidden },
		)

		val response = controller.create(
			CreateOrderRequest(
				customerId = OrderTestFixtures.customerId,
				deliveryDate = LocalDate.parse("2026-05-15"),
				items = listOf(OrderItemRequest("Столешница", BigDecimal.ONE, "шт")),
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
