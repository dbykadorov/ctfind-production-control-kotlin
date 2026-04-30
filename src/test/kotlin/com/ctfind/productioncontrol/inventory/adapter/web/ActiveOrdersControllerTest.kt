package com.ctfind.productioncontrol.inventory.adapter.web

import com.ctfind.productioncontrol.inventory.application.ActiveOrderSearchQuery
import com.ctfind.productioncontrol.inventory.application.InventoryOrderSummary
import com.ctfind.productioncontrol.inventory.application.OrderLookupPort
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

class ActiveOrdersControllerTest {
    private val orderLookup = mockk<OrderLookupPort>()
    private val controller = ActiveOrdersController(orderLookup)

    @Test
    fun `returns 403 for non-warehouse role`() {
        val response = controller.list(
            search = null,
            limit = 20,
            jwt = jwtFor(setOf("ORDER_MANAGER")),
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `returns 200 with filtered active orders for warehouse`() {
        every { orderLookup.searchActiveOrdersForConsumption(any()) } returns listOf(
            InventoryOrderSummary(
                id = UUID.randomUUID(),
                orderNumber = "ORD-001",
                customerName = "Acme",
                status = OrderStatus.IN_WORK,
            ),
        )

        val response = controller.list(
            search = "ORD",
            limit = 10,
            jwt = jwtFor(setOf("WAREHOUSE")),
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        verify {
            orderLookup.searchActiveOrdersForConsumption(
                ActiveOrderSearchQuery(search = "ORD", limit = 10),
            )
        }
    }
}

private fun jwtFor(roles: Set<String>): Jwt =
    Jwt.withTokenValue("tok")
        .header("alg", "none")
        .subject("user1")
        .claim("userId", UUID.randomUUID().toString())
        .claim("displayName", "Tester")
        .claim("roles", roles.toList())
        .build()
