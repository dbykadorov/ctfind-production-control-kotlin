package com.ctfind.productioncontrol.inventory.adapter.web

import com.ctfind.productioncontrol.inventory.application.BomLineView
import com.ctfind.productioncontrol.inventory.application.InventoryOrderSummary
import com.ctfind.productioncontrol.inventory.application.MaterialView
import com.ctfind.productioncontrol.inventory.application.MaterialUsageRowView
import com.ctfind.productioncontrol.inventory.application.MaterialUsageView
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

data class ConsumeRequest(
    val orderId: UUID,
    val quantity: BigDecimal,
    val comment: String? = null,
)

data class BomLineCreateRequest(
    val materialId: UUID,
    val quantity: BigDecimal,
    val comment: String? = null,
)

data class BomLineUpdateRequest(
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
    val materialName: String? = null,
    val materialUnit: MeasurementUnit? = null,
    val movementType: MovementType,
    val orderId: UUID? = null,
    val orderNumber: String? = null,
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

data class BomLineResponse(
    val id: UUID,
    val orderId: UUID,
    val materialId: UUID,
    val materialName: String,
    val materialUnit: MeasurementUnit,
    val quantity: BigDecimal,
    val comment: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class BomLineListResponse(
    val items: List<BomLineResponse>,
)

data class MaterialUsageRowResponse(
    val materialId: UUID,
    val materialName: String,
    val materialUnit: MeasurementUnit,
    val requiredQuantity: BigDecimal,
    val consumedQuantity: BigDecimal,
    val remainingToConsume: BigDecimal,
    val overconsumption: BigDecimal,
)

data class MaterialUsageResponse(
    val orderId: UUID,
    val rows: List<MaterialUsageRowResponse>,
)

data class InventoryOrderSummaryResponse(
    val id: UUID,
    val orderNumber: String,
    val customerName: String,
    val status: String,
)

data class InventoryOrderListResponse(
    val items: List<InventoryOrderSummaryResponse>,
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
    materialName = materialName,
    materialUnit = materialUnit,
    movementType = movementType,
    orderId = orderId,
    orderNumber = orderNumber,
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

fun BomLineView.toResponse() = BomLineResponse(
    id = id,
    orderId = orderId,
    materialId = materialId,
    materialName = materialName,
    materialUnit = materialUnit,
    quantity = quantity,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun MaterialUsageRowView.toResponse() = MaterialUsageRowResponse(
    materialId = materialId,
    materialName = materialName,
    materialUnit = materialUnit,
    requiredQuantity = requiredQuantity,
    consumedQuantity = consumedQuantity,
    remainingToConsume = remainingToConsume,
    overconsumption = overconsumption,
)

fun MaterialUsageView.toResponse() = MaterialUsageResponse(
    orderId = orderId,
    rows = rows.map { it.toResponse() },
)

fun InventoryOrderSummary.toResponse() = InventoryOrderSummaryResponse(
    id = id,
    orderNumber = orderNumber,
    customerName = customerName,
    status = status.name,
)
