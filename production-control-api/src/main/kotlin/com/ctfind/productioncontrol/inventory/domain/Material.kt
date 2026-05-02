package com.ctfind.productioncontrol.inventory.domain

import java.time.Instant
import java.util.UUID

enum class MeasurementUnit {
    PIECE, KILOGRAM, METER, LITER, SQUARE_METER, CUBIC_METER,
}

data class Material(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val unit: MeasurementUnit,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(name.isNotBlank()) { "material name must not be blank" }
    }
}
