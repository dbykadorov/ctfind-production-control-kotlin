package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import com.ctfind.productioncontrol.inventory.domain.OrderMaterialRequirement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class AddBomLineUseCase private constructor(
    private val materials: MaterialPort? = null,
    private val requirements: OrderMaterialRequirementPort? = null,
    private val orders: OrderLookupPort? = null,
    private val audit: InventoryAuditPort? = null,
    private val handler: ((AddBomLineCommand) -> InventoryMutationResult<BomLineView>)? = null,
) {
    @Autowired
    constructor(
        materials: MaterialPort,
        requirements: OrderMaterialRequirementPort,
        orders: OrderLookupPort,
        audit: InventoryAuditPort,
    ) : this(materials, requirements, orders, audit, null)

    constructor(handler: (AddBomLineCommand) -> InventoryMutationResult<BomLineView>) :
        this(null, null, null, null, handler)

    fun add(command: AddBomLineCommand): InventoryMutationResult<BomLineView> {
        handler?.let { return it(command) }
        val materials = requireNotNull(materials)
        val requirements = requireNotNull(requirements)
        val orders = requireNotNull(orders)
        val audit = requireNotNull(audit)

        if (!canEditBom(command.actor.roleCodes)) {
            return InventoryMutationResult.Forbidden
        }

        if (command.quantity <= BigDecimal.ZERO) {
            return InventoryMutationResult.ValidationFailed(
                message = "Quantity must be greater than zero",
                field = "quantity",
            )
        }
        if (command.comment != null && command.comment.length > 500) {
            return InventoryMutationResult.ValidationFailed(
                message = "Comment must be 500 chars or less",
                field = "comment",
            )
        }

        val order = orders.findOrderSummary(command.orderId) ?: return InventoryMutationResult.OrderNotFound
        if (order.shipped) {
            return InventoryMutationResult.OrderLocked
        }

        val material = materials.findById(command.materialId) ?: return InventoryMutationResult.MaterialNotFound

        if (requirements.existsByOrderIdAndMaterialId(command.orderId, command.materialId)) {
            return InventoryMutationResult.Conflict("Material already exists in order BOM")
        }

        val now = Instant.now()
        val line = OrderMaterialRequirement(
            id = UUID.randomUUID(),
            orderId = command.orderId,
            materialId = command.materialId,
            quantity = command.quantity,
            comment = command.comment?.trim()?.ifBlank { null },
            createdAt = now,
            updatedAt = now,
        )
        val saved = requirements.save(line)

        audit.record(
            InventoryAuditEvent(
                eventType = "BOM_LINE_ADDED",
                actorUserId = command.actor.userId,
                targetId = material.id,
                summary = "Добавлен материал «${material.name}» ${saved.quantity} в заказ #${order.orderNumber}",
                metadata = """{"orderId":"${order.id}","materialId":"${material.id}","quantity":"${saved.quantity}"}""",
            ),
        )

        return InventoryMutationResult.Success(
            saved.toView(
                materialName = material.name,
                materialUnit = material.unit,
            ),
        )
    }
}
