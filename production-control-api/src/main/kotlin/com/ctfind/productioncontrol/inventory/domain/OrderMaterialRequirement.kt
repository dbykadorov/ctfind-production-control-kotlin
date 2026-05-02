package com.ctfind.productioncontrol.inventory.domain

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class OrderMaterialRequirement(
    val id: UUID = UUID.randomUUID(),
    val orderId: UUID,
    val materialId: UUID,
    val quantity: BigDecimal,
    val comment: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(quantity > BigDecimal.ZERO) { "BOM line quantity must be greater than zero" }
        require(comment == null || comment.length <= 500) { "comment must be 500 chars or less" }
    }
}
