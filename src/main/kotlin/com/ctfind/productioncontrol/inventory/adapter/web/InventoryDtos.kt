package com.ctfind.productioncontrol.inventory.adapter.web

import com.ctfind.productioncontrol.inventory.application.MaterialView
import com.ctfind.productioncontrol.inventory.application.MaterialsPageResult
import com.ctfind.productioncontrol.inventory.application.StockMovementView
import com.ctfind.productioncontrol.inventory.application.StockMovementsPageResult
import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
import com.ctfind.productioncontrol.inventory.domain.MovementType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CreateMaterialRequest(
    val name: String,
    val unit: MeasurementUnit,
)

data class UpdateMaterialRequest(
    val name: String,
    val unit: MeasurementUnit,
)

data class StockReceiptRequest(
    val quantity: BigDecimal,
    val comment: String? = null,
)

data class MaterialResponse(
    val id: UUID,
    val name: String,
    val unit: MeasurementUnit,
    val currentStock: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class MaterialsPageResponse(
    val items: List<MaterialResponse>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
)

data class StockMovementResponse(
    val id: UUID,
    val materialId: UUID,
    val movementType: MovementType,
    val quantity: BigDecimal,
    val comment: String?,
    val actorDisplayName: String,
    val createdAt: Instant,
)

data class StockMovementsPageResponse(
    val items: List<StockMovementResponse>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
)

fun MaterialView.toResponse() = MaterialResponse(
    id = id,
    name = name,
    unit = unit,
    currentStock = currentStock,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun MaterialsPageResult.toResponse() = MaterialsPageResponse(
    items = items.map { it.toResponse() },
    page = page,
    size = size,
    totalItems = totalItems,
    totalPages = totalPages,
)

fun StockMovementView.toResponse() = StockMovementResponse(
    id = id,
    materialId = materialId,
    movementType = movementType,
    quantity = quantity,
    comment = comment,
    actorDisplayName = actorDisplayName,
    createdAt = createdAt,
)

fun StockMovementsPageResult.toResponse() = StockMovementsPageResponse(
    items = items.map { it.toResponse() },
    page = page,
    size = size,
    totalItems = totalItems,
    totalPages = totalPages,
)
