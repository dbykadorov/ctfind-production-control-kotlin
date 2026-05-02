package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import com.ctfind.productioncontrol.inventory.domain.Material
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class CreateMaterialUseCase private constructor(
    private val materials: MaterialPort? = null,
    private val audit: InventoryAuditPort? = null,
    private val handler: ((CreateMaterialCommand) -> InventoryMutationResult<MaterialView>)? = null,
) {
    @Autowired
    constructor(
        materials: MaterialPort,
        audit: InventoryAuditPort,
    ) : this(materials, audit, null)

    constructor(handler: (CreateMaterialCommand) -> InventoryMutationResult<MaterialView>) : this(null, null, handler)

    fun create(command: CreateMaterialCommand): InventoryMutationResult<MaterialView> {
        handler?.let { return it(command) }
        val materials = requireNotNull(materials)
        val audit = requireNotNull(audit)

        if (!command.actor.canManageInventory)
            return InventoryMutationResult.Forbidden

        val trimmedName = command.name.trim()
        if (trimmedName.isBlank())
            return InventoryMutationResult.ValidationFailed("Name must not be blank", "name")

        if (materials.existsByNameIgnoreCase(trimmedName))
            return InventoryMutationResult.Conflict("Material with this name already exists")

        val now = Instant.now()
        val material = Material(
            id = UUID.randomUUID(),
            name = trimmedName,
            unit = command.unit,
            createdAt = now,
            updatedAt = now,
        )
        val saved = materials.save(material)

        audit.record(
            InventoryAuditEvent(
                eventType = "MATERIAL_CREATED",
                actorUserId = command.actor.userId,
                targetId = saved.id,
                summary = "Создан материал «${saved.name}» (${saved.unit})",
            ),
        )

        return InventoryMutationResult.Success(saved.toView(currentStock = java.math.BigDecimal.ZERO))
    }
}
