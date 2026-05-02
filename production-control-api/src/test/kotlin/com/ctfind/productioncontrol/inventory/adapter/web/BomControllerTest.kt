package com.ctfind.productioncontrol.inventory.adapter.web

import com.ctfind.productioncontrol.inventory.application.AddBomLineUseCase
import com.ctfind.productioncontrol.inventory.application.BomLineView
import com.ctfind.productioncontrol.inventory.application.InventoryMutationResult
import com.ctfind.productioncontrol.inventory.application.InventoryQueryUseCase
import com.ctfind.productioncontrol.inventory.application.RemoveBomLineUseCase
import com.ctfind.productioncontrol.inventory.application.UpdateBomLineUseCase
import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class BomControllerTest {
    private val orderId = UUID.randomUUID()
    private val lineId = UUID.randomUUID()
    private val materialId = UUID.randomUUID()

    @Test
    fun `POST add returns 201 on success`() {
        val controller = BomController(
            query = mockk<InventoryQueryUseCase>(relaxed = true),
            addBomLine = AddBomLineUseCase {
                InventoryMutationResult.Success(
                    BomLineView(
                        id = lineId,
                        orderId = orderId,
                        materialId = materialId,
                        materialName = "Фанера",
                        materialUnit = MeasurementUnit.SQUARE_METER,
                        quantity = BigDecimal("10"),
                        comment = null,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now(),
                    ),
                )
            },
            updateBomLine = UpdateBomLineUseCase { InventoryMutationResult.BomLineNotFound },
            removeBomLine = RemoveBomLineUseCase { InventoryMutationResult.BomLineNotFound },
        )

        val response = controller.add(
            orderId = orderId,
            body = BomLineCreateRequest(materialId, BigDecimal("10"), null),
            jwt = jwtFor(setOf("ORDER_MANAGER")),
        )

        assertEquals(HttpStatus.CREATED, response.statusCode)
    }

    @Test
    fun `POST add returns 403 when forbidden`() {
        val controller = BomController(
            query = mockk<InventoryQueryUseCase>(relaxed = true),
            addBomLine = AddBomLineUseCase { InventoryMutationResult.Forbidden },
            updateBomLine = UpdateBomLineUseCase { InventoryMutationResult.BomLineNotFound },
            removeBomLine = RemoveBomLineUseCase { InventoryMutationResult.BomLineNotFound },
        )

        val response = controller.add(
            orderId = orderId,
            body = BomLineCreateRequest(materialId, BigDecimal("10"), null),
            jwt = jwtFor(setOf("ORDER_MANAGER")),
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `PUT update returns 404 for missing line`() {
        val controller = BomController(
            query = mockk<InventoryQueryUseCase>(relaxed = true),
            addBomLine = AddBomLineUseCase { InventoryMutationResult.Forbidden },
            updateBomLine = UpdateBomLineUseCase { InventoryMutationResult.BomLineNotFound },
            removeBomLine = RemoveBomLineUseCase { InventoryMutationResult.BomLineNotFound },
        )

        val response = controller.update(
            orderId = orderId,
            lineId = lineId,
            body = BomLineUpdateRequest(quantity = BigDecimal("12"), comment = "upd"),
            jwt = jwtFor(setOf("ORDER_MANAGER")),
        )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `DELETE returns 409 for line with consumption`() {
        val controller = BomController(
            query = mockk<InventoryQueryUseCase>(relaxed = true),
            addBomLine = AddBomLineUseCase { InventoryMutationResult.Forbidden },
            updateBomLine = UpdateBomLineUseCase { InventoryMutationResult.BomLineNotFound },
            removeBomLine = RemoveBomLineUseCase { InventoryMutationResult.Conflict("has consumption") },
        )

        val response = controller.remove(
            orderId = orderId,
            lineId = lineId,
            jwt = jwtFor(setOf("ORDER_MANAGER")),
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertInstanceOf(InventoryApiError::class.java, response.body)
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
