package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import com.ctfind.productioncontrol.inventory.domain.MovementType
import com.ctfind.productioncontrol.inventory.domain.StockMovement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class ReceiveStockUseCase private constructor(
    private val materials: MaterialPort? = null,
    private val movements: StockMovementPort? = null,
    private val audit: InventoryAuditPort? = null,
    private val handler: ((ReceiveStockCommand) -> InventoryMutationResult<StockMovementView>)? = null,
) {
    @Autowired
    constructor(
        materials: MaterialPort,
        movements: StockMovementPort,
        audit: InventoryAuditPort,
    ) : this(materials, movements, audit, null)

    constructor(handler: (ReceiveStockCommand) -> InventoryMutationResult<StockMovementView>) : this(null, null, null, handler)

    fun receive(command: ReceiveStockCommand): InventoryMutationResult<StockMovementView> {
        handler?.let { return it(command) }
        val materials = requireNotNull(materials)
        val movements = requireNotNull(movements)
        val audit = requireNotNull(audit)

        if (!command.actor.canManageInventory)
            return InventoryMutationResult.Forbidden

        val material = materials.findById(command.materialId)
            ?: return InventoryMutationResult.NotFound

        if (command.quantity <= BigDecimal.ZERO)
            return InventoryMutationResult.ValidationFailed("Quantity must be greater than zero", "quantity")

        val movement = StockMovement(
            id = UUID.randomUUID(),
            materialId = command.materialId,
            movementType = MovementType.RECEIPT,
            quantity = command.quantity,
            comment = command.comment,
            actorUserId = command.actor.userId,
            actorDisplayName = command.actor.displayName,
            createdAt = Instant.now(),
        )
        val saved = movements.save(movement)

        val summaryComment = command.comment?.let { " ($it)" } ?: ""
        audit.record(
            InventoryAuditEvent(
                eventType = "STOCK_RECEIPT",
                actorUserId = command.actor.userId,
                targetId = command.materialId,
                summary = "Приход ${command.quantity} ${material.unit.name.lowercase()} материала «${material.name}»$summaryComment",
            ),
        )

        return InventoryMutationResult.Success(saved.toView())
    }
}
