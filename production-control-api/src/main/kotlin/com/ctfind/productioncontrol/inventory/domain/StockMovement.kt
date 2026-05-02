package com.ctfind.productioncontrol.inventory.domain

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class MovementType {
    RECEIPT,
    CONSUMPTION,
}

data class StockMovement(
    val id: UUID = UUID.randomUUID(),
    val materialId: UUID,
    val movementType: MovementType,
    val quantity: BigDecimal,
    val comment: String?,
    val orderId: UUID?,
    val actorUserId: UUID,
    val actorDisplayName: String,
    val createdAt: Instant,
) {
    init {
        require(quantity > BigDecimal.ZERO) { "quantity must be greater than zero" }
        when (movementType) {
            MovementType.RECEIPT -> require(orderId == null) {
                "RECEIPT movement must not reference order"
            }

            MovementType.CONSUMPTION -> requireNotNull(orderId) {
                "CONSUMPTION movement must reference order"
            }
        }
    }
}
