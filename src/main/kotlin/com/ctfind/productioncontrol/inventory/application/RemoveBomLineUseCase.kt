package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RemoveBomLineUseCase private constructor(
    private val materials: MaterialPort? = null,
    private val requirements: OrderMaterialRequirementPort? = null,
    private val movements: StockMovementPort? = null,
    private val orders: OrderLookupPort? = null,
    private val audit: InventoryAuditPort? = null,
    private val handler: ((RemoveBomLineCommand) -> InventoryMutationResult<Unit>)? = null,
) {
    @Autowired
    constructor(
        materials: MaterialPort,
        requirements: OrderMaterialRequirementPort,
        movements: StockMovementPort,
        orders: OrderLookupPort,
        audit: InventoryAuditPort,
    ) : this(materials, requirements, movements, orders, audit, null)

    constructor(handler: (RemoveBomLineCommand) -> InventoryMutationResult<Unit>) :
        this(null, null, null, null, null, handler)

    fun remove(command: RemoveBomLineCommand): InventoryMutationResult<Unit> {
        handler?.let { return it(command) }
        val materials = requireNotNull(materials)
        val requirements = requireNotNull(requirements)
        val movements = requireNotNull(movements)
        val orders = requireNotNull(orders)
        val audit = requireNotNull(audit)

        if (!canEditBom(command.actor.roleCodes)) {
            return InventoryMutationResult.Forbidden
        }

        val order = orders.findOrderSummary(command.orderId) ?: return InventoryMutationResult.OrderNotFound
        if (order.shipped) {
            return InventoryMutationResult.OrderLocked
        }

        val existing = requirements.findByLineIdAndOrderId(command.lineId, command.orderId)
            ?: return InventoryMutationResult.BomLineNotFound
        val material = materials.findById(existing.materialId) ?: return InventoryMutationResult.MaterialNotFound

        if (movements.hasConsumption(command.orderId, existing.materialId)) {
            return InventoryMutationResult.Conflict("Cannot delete BOM line with consumption history")
        }

        requirements.deleteLineById(existing.id)

        audit.record(
            InventoryAuditEvent(
                eventType = "BOM_LINE_REMOVED",
                actorUserId = command.actor.userId,
                targetId = material.id,
                summary = "Удалена строка BOM «${material.name}» в заказе #${order.orderNumber}",
                metadata = """{"orderId":"${order.id}","materialId":"${material.id}","quantity":"${existing.quantity}"}""",
            ),
        )

        return InventoryMutationResult.Success(Unit)
    }
}
