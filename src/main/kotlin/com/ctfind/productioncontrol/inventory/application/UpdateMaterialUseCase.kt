package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant

@Service
class UpdateMaterialUseCase private constructor(
    private val materials: MaterialPort? = null,
    private val movements: StockMovementPort? = null,
    private val audit: InventoryAuditPort? = null,
    private val handler: ((UpdateMaterialCommand) -> InventoryMutationResult<MaterialView>)? = null,
) {
    @Autowired
    constructor(
        materials: MaterialPort,
        movements: StockMovementPort,
        audit: InventoryAuditPort,
    ) : this(materials, movements, audit, null)

    constructor(handler: (UpdateMaterialCommand) -> InventoryMutationResult<MaterialView>) : this(null, null, null, handler)

    fun update(command: UpdateMaterialCommand): InventoryMutationResult<MaterialView> {
        handler?.let { return it(command) }
        val materials = requireNotNull(materials)
        val movements = requireNotNull(movements)
        val audit = requireNotNull(audit)

        if (!command.actor.canManageInventory)
            return InventoryMutationResult.Forbidden

        val existing = materials.findById(command.id)
            ?: return InventoryMutationResult.NotFound

        val trimmedName = command.name.trim()
        if (trimmedName.isBlank())
            return InventoryMutationResult.ValidationFailed("Name must not be blank", "name")

        if (materials.existsByNameIgnoreCaseAndIdNot(trimmedName, command.id))
            return InventoryMutationResult.Conflict("Material with this name already exists")

        val updated = existing.copy(
            name = trimmedName,
            unit = command.unit,
            updatedAt = Instant.now(),
        )
        val saved = materials.save(updated)
        val currentStock = movements.sumQuantityByMaterialId(saved.id)

        audit.record(
            InventoryAuditEvent(
                eventType = "MATERIAL_UPDATED",
                actorUserId = command.actor.userId,
                targetId = saved.id,
                summary = "Обновлён материал «${saved.name}»",
            ),
        )

        return InventoryMutationResult.Success(saved.toView(currentStock))
    }
}
