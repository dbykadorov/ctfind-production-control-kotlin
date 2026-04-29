package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DeleteMaterialUseCase private constructor(
    private val materials: MaterialPort? = null,
    private val movements: StockMovementPort? = null,
    private val audit: InventoryAuditPort? = null,
    private val handler: ((DeleteMaterialCommand) -> InventoryMutationResult<Unit>)? = null,
) {
    @Autowired
    constructor(
        materials: MaterialPort,
        movements: StockMovementPort,
        audit: InventoryAuditPort,
    ) : this(materials, movements, audit, null)

    constructor(handler: (DeleteMaterialCommand) -> InventoryMutationResult<Unit>) : this(null, null, null, handler)

    fun delete(command: DeleteMaterialCommand): InventoryMutationResult<Unit> {
        handler?.let { return it(command) }
        val materials = requireNotNull(materials)
        val audit = requireNotNull(audit)

        if (!command.actor.canManageInventory)
            return InventoryMutationResult.Forbidden

        val existing = materials.findById(command.id)
            ?: return InventoryMutationResult.NotFound

        if (materials.hasMovements(command.id))
            return InventoryMutationResult.Conflict("Cannot delete material with movement history")

        materials.deleteById(command.id)

        audit.record(
            InventoryAuditEvent(
                eventType = "MATERIAL_DELETED",
                actorUserId = command.actor.userId,
                targetId = existing.id,
                summary = "Удалён материал «${existing.name}»",
            ),
        )

        return InventoryMutationResult.Success(Unit)
    }
}
