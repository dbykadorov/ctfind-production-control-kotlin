package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.Material
import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
import com.ctfind.productioncontrol.inventory.domain.MovementType
import com.ctfind.productioncontrol.inventory.domain.StockMovement
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class AuthenticatedInventoryActor(
    val userId: UUID,
    val login: String,
    val displayName: String,
    val roleCodes: Set<String>,
) {
    val canManageInventory: Boolean get() = com.ctfind.productioncontrol.inventory.application.canManageInventory(roleCodes)
}

data class CreateMaterialCommand(
    val name: String,
    val unit: MeasurementUnit,
    val actor: AuthenticatedInventoryActor,
)

data class UpdateMaterialCommand(
    val id: UUID,
    val name: String,
    val unit: MeasurementUnit,
    val actor: AuthenticatedInventoryActor,
)

data class DeleteMaterialCommand(
    val id: UUID,
    val actor: AuthenticatedInventoryActor,
)

data class ReceiveStockCommand(
    val materialId: UUID,
    val quantity: BigDecimal,
    val comment: String?,
    val actor: AuthenticatedInventoryActor,
)

data class MaterialListQuery(
    val search: String? = null,
    val page: Int = 0,
    val size: Int = 20,
)

data class MaterialView(
    val id: UUID,
    val name: String,
    val unit: MeasurementUnit,
    val currentStock: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class MaterialsPageResult(
    val items: List<MaterialView>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
)

data class StockMovementView(
    val id: UUID,
    val materialId: UUID,
    val movementType: MovementType,
    val quantity: BigDecimal,
    val comment: String?,
    val actorDisplayName: String,
    val createdAt: Instant,
)

data class StockMovementsPageResult(
    val items: List<StockMovementView>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
)

fun Material.toView(currentStock: BigDecimal) = MaterialView(
    id = id,
    name = name,
    unit = unit,
    currentStock = currentStock,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun StockMovement.toView() = StockMovementView(
    id = id,
    materialId = materialId,
    movementType = movementType,
    quantity = quantity,
    comment = comment,
    actorDisplayName = actorDisplayName,
    createdAt = createdAt,
)

sealed interface InventoryMutationResult<out T> {
    data class Success<T>(val value: T) : InventoryMutationResult<T>
    data object Forbidden : InventoryMutationResult<Nothing>
    data object NotFound : InventoryMutationResult<Nothing>
    data class ValidationFailed(val message: String, val field: String? = null) : InventoryMutationResult<Nothing>
    data class Conflict(val message: String) : InventoryMutationResult<Nothing>
}
