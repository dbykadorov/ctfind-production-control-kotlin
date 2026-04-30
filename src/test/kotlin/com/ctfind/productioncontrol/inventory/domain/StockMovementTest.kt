package com.ctfind.productioncontrol.inventory.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class StockMovementTest {

    @Test
    fun `RECEIPT movement with orderId fails`() {
        assertThrows<IllegalArgumentException> {
            StockMovement(
                materialId = UUID.randomUUID(),
                movementType = MovementType.RECEIPT,
                quantity = BigDecimal("1"),
                comment = null,
                orderId = UUID.randomUUID(),
                actorUserId = UUID.randomUUID(),
                actorDisplayName = "Tester",
                createdAt = Instant.now(),
            )
        }
    }

    @Test
    fun `CONSUMPTION movement without orderId fails`() {
        assertThrows<IllegalArgumentException> {
            StockMovement(
                materialId = UUID.randomUUID(),
                movementType = MovementType.CONSUMPTION,
                quantity = BigDecimal("1"),
                comment = null,
                orderId = null,
                actorUserId = UUID.randomUUID(),
                actorDisplayName = "Tester",
                createdAt = Instant.now(),
            )
        }
    }

    @Test
    fun `quantity must be greater than zero`() {
        assertThrows<IllegalArgumentException> {
            StockMovement(
                materialId = UUID.randomUUID(),
                movementType = MovementType.RECEIPT,
                quantity = BigDecimal.ZERO,
                comment = null,
                orderId = null,
                actorUserId = UUID.randomUUID(),
                actorDisplayName = "Tester",
                createdAt = Instant.now(),
            )
        }
    }
}
