package com.ctfind.productioncontrol.inventory.domain

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class MovementType {
    RECEIPT,
}

data class StockMovement(
    val id: UUID = UUID.randomUUID(),
    val materialId: UUID,
    val movementType: MovementType,
    val quantity: BigDecimal,
    val comment: String?,
    val actorUserId: UUID,
    val actorDisplayName: String,
    val createdAt: Instant,
) {
    init {
        require(quantity > BigDecimal.ZERO) { "quantity must be greater than zero" }
    }
}
