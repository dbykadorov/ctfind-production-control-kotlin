package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import com.ctfind.productioncontrol.inventory.domain.Material
import com.ctfind.productioncontrol.inventory.domain.OrderMaterialRequirement
import com.ctfind.productioncontrol.inventory.domain.MovementType
import com.ctfind.productioncontrol.inventory.domain.StockMovement
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import java.math.BigDecimal
import java.util.UUID

interface MaterialPort {
    fun findById(id: UUID): Material?
    fun findByIdForUpdate(id: UUID): Material?
    fun findAll(query: MaterialListQuery): List<Material>
    fun count(query: MaterialListQuery): Long
    fun save(material: Material): Material
    fun deleteById(id: UUID)
    fun existsByNameIgnoreCase(name: String): Boolean
    fun existsByNameIgnoreCaseAndIdNot(name: String, id: UUID): Boolean
    fun hasMovements(materialId: UUID): Boolean
}

interface StockMovementPort {
    fun save(movement: StockMovement): StockMovement
    fun findByMaterialId(materialId: UUID, page: Int, size: Int): List<StockMovement>
    fun countByMaterialId(materialId: UUID): Long
    fun sumQuantityByMaterialId(materialId: UUID): BigDecimal
    fun sumQuantityByMaterialIdAndType(materialId: UUID, movementType: MovementType): BigDecimal
    fun hasConsumption(orderId: UUID, materialId: UUID): Boolean
    fun sumConsumedQuantity(orderId: UUID, materialId: UUID): BigDecimal
    fun sumConsumedByOrder(orderId: UUID): Map<UUID, BigDecimal>
}

interface OrderMaterialRequirementPort {
    fun save(requirement: OrderMaterialRequirement): OrderMaterialRequirement
    fun findByLineId(id: UUID): OrderMaterialRequirement?
    fun findByLineIdAndOrderId(id: UUID, orderId: UUID): OrderMaterialRequirement?
    fun findByOrderIdOrderByCreatedAtDesc(orderId: UUID): List<OrderMaterialRequirement>
    fun findByOrderIdAndMaterialId(orderId: UUID, materialId: UUID): OrderMaterialRequirement?
    fun existsByOrderIdAndMaterialId(orderId: UUID, materialId: UUID): Boolean
    fun existsInActiveOrder(materialId: UUID): Boolean
    fun deleteByMaterialIdInShippedOrders(materialId: UUID): Int
    fun deleteLineById(id: UUID)
}

data class InventoryOrderSummary(
    val id: UUID,
    val orderNumber: String,
    val customerName: String,
    val status: OrderStatus,
) {
    val shipped: Boolean
        get() = status == OrderStatus.SHIPPED
}

data class ActiveOrderSearchQuery(
    val search: String? = null,
    val limit: Int = 20,
)

interface OrderLookupPort {
    fun findOrderSummary(orderId: UUID): InventoryOrderSummary?
    fun searchActiveOrdersForConsumption(query: ActiveOrderSearchQuery): List<InventoryOrderSummary>
}

interface InventoryAuditPort {
    fun record(event: InventoryAuditEvent): InventoryAuditEvent
}
