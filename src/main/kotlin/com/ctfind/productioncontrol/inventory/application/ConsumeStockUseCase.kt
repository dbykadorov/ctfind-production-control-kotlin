package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import com.ctfind.productioncontrol.inventory.domain.MovementType
import com.ctfind.productioncontrol.inventory.domain.StockMovement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class ConsumeStockUseCase private constructor(
    private val materials: MaterialPort? = null,
    private val requirements: OrderMaterialRequirementPort? = null,
    private val movements: StockMovementPort? = null,
    private val orders: OrderLookupPort? = null,
    private val audit: InventoryAuditPort? = null,
    private val handler: ((ConsumeStockCommand) -> InventoryMutationResult<StockMovementView>)? = null,
) {
    @Autowired
    constructor(
        materials: MaterialPort,
        requirements: OrderMaterialRequirementPort,
        movements: StockMovementPort,
        orders: OrderLookupPort,
        audit: InventoryAuditPort,
    ) : this(materials, requirements, movements, orders, audit, null)

    constructor(handler: (ConsumeStockCommand) -> InventoryMutationResult<StockMovementView>) :
        this(null, null, null, null, null, handler)

    @Transactional
    fun consume(command: ConsumeStockCommand): InventoryMutationResult<StockMovementView> {
        handler?.let { return it(command) }
        val materials = requireNotNull(materials)
        val requirements = requireNotNull(requirements)
        val movements = requireNotNull(movements)
        val orders = requireNotNull(orders)
        val audit = requireNotNull(audit)

        if (!canConsumeStock(command.actor.roleCodes)) {
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

        val material = materials.findByIdForUpdate(command.materialId) ?: return InventoryMutationResult.MaterialNotFound
        val order = orders.findOrderSummary(command.orderId) ?: return InventoryMutationResult.OrderNotFound
        if (order.shipped) {
            return InventoryMutationResult.OrderLocked
        }
        if (!requirements.existsByOrderIdAndMaterialId(command.orderId, command.materialId)) {
            return InventoryMutationResult.MaterialNotInBom
        }

        val available = movements.sumQuantityByMaterialId(command.materialId)
        if (command.quantity > available) {
            return InventoryMutationResult.InsufficientStock(available)
        }

        val movement = StockMovement(
            id = UUID.randomUUID(),
            materialId = command.materialId,
            movementType = MovementType.CONSUMPTION,
            quantity = command.quantity,
            comment = command.comment?.trim()?.ifBlank { null },
            orderId = command.orderId,
            actorUserId = command.actor.userId,
            actorDisplayName = command.actor.displayName,
            createdAt = Instant.now(),
        )
        val saved = movements.save(movement)

        audit.record(
            InventoryAuditEvent(
                eventType = "STOCK_CONSUMPTION",
                actorUserId = command.actor.userId,
                targetId = material.id,
                summary = "Списание ${command.quantity} ${material.unit.name.lowercase()} материала «${material.name}» на заказ #${order.orderNumber}",
                metadata = """
                    {"orderId":"${order.id}","orderNumber":"${order.orderNumber}",
                    "materialId":"${material.id}","quantity":"${command.quantity}"}
                """.trimIndent().replace("\n", ""),
            ),
        )

        return InventoryMutationResult.Success(
            saved.toView().copy(
                materialName = material.name,
                materialUnit = material.unit,
                orderNumber = order.orderNumber,
            ),
        )
    }
}
