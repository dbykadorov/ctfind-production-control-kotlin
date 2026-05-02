package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant

@Service
class UpdateBomLineUseCase private constructor(
    private val materials: MaterialPort? = null,
    private val requirements: OrderMaterialRequirementPort? = null,
    private val orders: OrderLookupPort? = null,
    private val audit: InventoryAuditPort? = null,
    private val handler: ((UpdateBomLineCommand) -> InventoryMutationResult<BomLineView>)? = null,
) {
    @Autowired
    constructor(
        materials: MaterialPort,
        requirements: OrderMaterialRequirementPort,
        orders: OrderLookupPort,
        audit: InventoryAuditPort,
    ) : this(materials, requirements, orders, audit, null)

    constructor(handler: (UpdateBomLineCommand) -> InventoryMutationResult<BomLineView>) :
        this(null, null, null, null, handler)

    fun update(command: UpdateBomLineCommand): InventoryMutationResult<BomLineView> {
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

        val existing = requirements.findByLineIdAndOrderId(command.lineId, command.orderId)
            ?: return InventoryMutationResult.BomLineNotFound
        val material = materials.findById(existing.materialId) ?: return InventoryMutationResult.MaterialNotFound

        val beforeQuantity = existing.quantity
        val beforeComment = existing.comment
        val saved = requirements.save(
            existing.copy(
                quantity = command.quantity,
                comment = command.comment?.trim()?.ifBlank { null },
                updatedAt = Instant.now(),
            ),
        )

        audit.record(
            InventoryAuditEvent(
                eventType = "BOM_LINE_UPDATED",
                actorUserId = command.actor.userId,
                targetId = material.id,
                summary = "Изменена строка BOM «${material.name}» в заказе #${order.orderNumber}",
                metadata = """
                    {"orderId":"${order.id}","materialId":"${material.id}",
                    "before":{"quantity":"$beforeQuantity","comment":${beforeComment?.let { "\"$it\"" } ?: "null"}},
                    "after":{"quantity":"${saved.quantity}","comment":${saved.comment?.let { "\"$it\"" } ?: "null"}}}
                """.trimIndent().replace("\n", ""),
            ),
        )

        return InventoryMutationResult.Success(saved.toView(material.name, material.unit))
    }
}
