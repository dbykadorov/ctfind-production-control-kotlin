package com.ctfind.productioncontrol.inventory.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class OrderMaterialRequirementTest {
    @Test
    fun `quantity must be greater than zero`() {
        assertThrows<IllegalArgumentException> {
            OrderMaterialRequirement(
                orderId = UUID.randomUUID(),
                materialId = UUID.randomUUID(),
                quantity = BigDecimal.ZERO,
                comment = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        }
    }

    @Test
    fun `comment must be less than or equal to 500 chars`() {
        val comment = "a".repeat(501)
        assertThrows<IllegalArgumentException> {
            OrderMaterialRequirement(
                orderId = UUID.randomUUID(),
                materialId = UUID.randomUUID(),
                quantity = BigDecimal("1"),
                comment = comment,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        }
    }
}
