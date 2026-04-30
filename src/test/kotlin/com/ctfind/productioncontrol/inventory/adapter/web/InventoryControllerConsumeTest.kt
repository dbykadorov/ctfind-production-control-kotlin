package com.ctfind.productioncontrol.inventory.adapter.web

import com.ctfind.productioncontrol.inventory.application.ConsumeStockUseCase
import com.ctfind.productioncontrol.inventory.application.CreateMaterialUseCase
import com.ctfind.productioncontrol.inventory.application.DeleteMaterialUseCase
import com.ctfind.productioncontrol.inventory.application.InventoryMutationResult
import com.ctfind.productioncontrol.inventory.application.InventoryQueryUseCase
import com.ctfind.productioncontrol.inventory.application.ReceiveStockUseCase
import com.ctfind.productioncontrol.inventory.application.StockMovementView
import com.ctfind.productioncontrol.inventory.application.UpdateMaterialUseCase
import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
import com.ctfind.productioncontrol.inventory.domain.MovementType
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class InventoryControllerConsumeTest {
    @Test
    fun `POST consume returns 201 on success`() {
        val controller = controllerWithConsume(
            consume = ConsumeStockUseCase {
                InventoryMutationResult.Success(
                    StockMovementView(
                        id = UUID.randomUUID(),
                        materialId = UUID.randomUUID(),
                        materialName = "Фанера",
                        materialUnit = MeasurementUnit.SQUARE_METER,
                        movementType = MovementType.CONSUMPTION,
                        orderId = UUID.randomUUID(),
                        orderNumber = "ORD-1",
                        quantity = BigDecimal("5"),
                        comment = "Выдано",
                        actorDisplayName = "Tester",
                        createdAt = Instant.now(),
                    ),
                )
            },
        )

        val response = controller.consume(
            id = UUID.randomUUID(),
            request = ConsumeRequest(orderId = UUID.randomUUID(), quantity = BigDecimal("5"), comment = "ok"),
            jwt = jwtFor(setOf("WAREHOUSE")),
        )

        assertEquals(HttpStatus.CREATED, response.statusCode)
    }

    @Test
    fun `POST consume returns 409 with available for insufficient stock`() {
        val controller = controllerWithConsume(
            consume = ConsumeStockUseCase { InventoryMutationResult.InsufficientStock(BigDecimal("3")) },
        )

        val response = controller.consume(
            id = UUID.randomUUID(),
            request = ConsumeRequest(orderId = UUID.randomUUID(), quantity = BigDecimal("5"), comment = null),
            jwt = jwtFor(setOf("WAREHOUSE")),
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        val body = assertInstanceOf(InventoryApiError::class.java, response.body)
        assertEquals(BigDecimal("3"), body.available)
    }

    @Test
    fun `POST consume returns 403 when forbidden`() {
        val controller = controllerWithConsume(
            consume = ConsumeStockUseCase { InventoryMutationResult.Forbidden },
        )

        val response = controller.consume(
            id = UUID.randomUUID(),
            request = ConsumeRequest(orderId = UUID.randomUUID(), quantity = BigDecimal("5"), comment = null),
            jwt = jwtFor(setOf("ORDER_MANAGER")),
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }
}

private fun controllerWithConsume(consume: ConsumeStockUseCase): InventoryController =
    InventoryController(
        query = mockk<InventoryQueryUseCase>(relaxed = true),
        createMaterial = CreateMaterialUseCase { InventoryMutationResult.Forbidden },
        updateMaterial = UpdateMaterialUseCase { InventoryMutationResult.Forbidden },
        deleteMaterial = DeleteMaterialUseCase { InventoryMutationResult.Forbidden },
        receiveStock = ReceiveStockUseCase { InventoryMutationResult.Forbidden },
        consumeStock = consume,
    )

private fun jwtFor(roles: Set<String>): Jwt =
    Jwt.withTokenValue("tok")
        .header("alg", "none")
        .subject("user1")
        .claim("userId", UUID.randomUUID().toString())
        .claim("displayName", "Tester")
        .claim("roles", roles.toList())
        .build()
