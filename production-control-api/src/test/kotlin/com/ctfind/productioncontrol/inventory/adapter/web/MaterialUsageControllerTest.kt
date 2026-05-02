package com.ctfind.productioncontrol.inventory.adapter.web

import com.ctfind.productioncontrol.inventory.application.InventoryQueryUseCase
import com.ctfind.productioncontrol.inventory.application.MaterialUsageRowView
import com.ctfind.productioncontrol.inventory.application.MaterialUsageView
import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import java.math.BigDecimal
import java.util.UUID

class MaterialUsageControllerTest {
    private val query = mockk<InventoryQueryUseCase>()
    private val controller = MaterialUsageController(query)

    @Test
    fun `returns 403 for user without visibility permissions`() {
        val response = controller.getUsage(
            orderId = UUID.randomUUID(),
            jwt = jwtFor(setOf("PRODUCTION_EXECUTOR")),
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `returns 404 when order not found`() {
        val orderId = UUID.randomUUID()
        every { query.getMaterialUsage(orderId) } returns null

        val response = controller.getUsage(
            orderId = orderId,
            jwt = jwtFor(setOf("ORDER_MANAGER")),
        )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `returns 200 with usage payload`() {
        val orderId = UUID.randomUUID()
        every { query.getMaterialUsage(orderId) } returns MaterialUsageView(
            orderId = orderId,
            rows = listOf(
                MaterialUsageRowView(
                    materialId = UUID.randomUUID(),
                    materialName = "Фанера",
                    materialUnit = MeasurementUnit.SQUARE_METER,
                    requiredQuantity = BigDecimal("10"),
                    consumedQuantity = BigDecimal("4"),
                    remainingToConsume = BigDecimal("6"),
                    overconsumption = BigDecimal.ZERO,
                ),
            ),
        )

        val response = controller.getUsage(
            orderId = orderId,
            jwt = jwtFor(setOf("ORDER_MANAGER")),
        )

        assertEquals(HttpStatus.OK, response.statusCode)
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
