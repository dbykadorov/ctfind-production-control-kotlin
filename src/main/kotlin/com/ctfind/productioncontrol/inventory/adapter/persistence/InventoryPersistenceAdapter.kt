package com.ctfind.productioncontrol.inventory.adapter.persistence

import com.ctfind.productioncontrol.inventory.application.InventoryAuditPort
import com.ctfind.productioncontrol.inventory.application.MaterialListQuery
import com.ctfind.productioncontrol.inventory.application.MaterialPort
import com.ctfind.productioncontrol.inventory.application.OrderMaterialRequirementPort
import com.ctfind.productioncontrol.inventory.application.StockMovementPort
import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import com.ctfind.productioncontrol.inventory.domain.Material
import com.ctfind.productioncontrol.inventory.domain.OrderMaterialRequirement
import com.ctfind.productioncontrol.inventory.domain.MovementType
import com.ctfind.productioncontrol.inventory.domain.StockMovement
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class InventoryPersistenceAdapter(
    private val materialRepo: MaterialJpaRepository,
    private val movementRepo: StockMovementJpaRepository,
    private val auditRepo: InventoryAuditEventJpaRepository,
    private val requirementRepo: OrderMaterialRequirementJpaRepository,
) : MaterialPort, StockMovementPort, InventoryAuditPort, OrderMaterialRequirementPort {

    // MaterialPort
    override fun findById(id: UUID): Material? = materialRepo.findById(id).orElse(null)?.toDomain()
    override fun findByIdForUpdate(id: UUID): Material? = materialRepo.findByIdForUpdate(id)?.toDomain()

    override fun findAll(query: MaterialListQuery): List<Material> {
        val pageable = PageRequest.of(query.page, query.size, Sort.by("name"))
        return if (query.search.isNullOrBlank()) {
            materialRepo.findAll(pageable).content
        } else {
            materialRepo.findAllBySearchTerm(query.search, pageable).content
        }.map { it.toDomain() }
    }

    override fun count(query: MaterialListQuery): Long =
        if (query.search.isNullOrBlank()) materialRepo.count()
        else materialRepo.countBySearchTerm(query.search)

    override fun save(material: Material): Material = materialRepo.save(material.toEntity()).toDomain()

    override fun deleteById(id: UUID) = materialRepo.deleteById(id)

    override fun existsByNameIgnoreCase(name: String): Boolean = materialRepo.existsByNameIgnoreCase(name)

    override fun existsByNameIgnoreCaseAndIdNot(name: String, id: UUID): Boolean =
        materialRepo.existsByNameIgnoreCaseAndIdNot(name, id)

    override fun hasMovements(materialId: UUID): Boolean = movementRepo.existsByMaterialId(materialId)

    // StockMovementPort
    override fun save(movement: StockMovement): StockMovement = movementRepo.save(movement.toEntity()).toDomain()

    override fun findByMaterialId(materialId: UUID, page: Int, size: Int): List<StockMovement> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return movementRepo.findByMaterialIdOrderByCreatedAtDesc(materialId, pageable).content.map { it.toDomain() }
    }

    override fun countByMaterialId(materialId: UUID): Long = movementRepo.countByMaterialId(materialId)

    override fun sumQuantityByMaterialId(materialId: UUID): BigDecimal =
        movementRepo.sumQuantityByMaterialId(materialId)

    override fun sumQuantityByMaterialIdAndType(materialId: UUID, movementType: MovementType): BigDecimal =
        movementRepo.sumQuantityByMaterialIdAndType(materialId, movementType)

    override fun hasConsumption(orderId: UUID, materialId: UUID): Boolean =
        movementRepo.existsByOrderIdAndMaterialIdAndMovementType(orderId, materialId, MovementType.CONSUMPTION)

    override fun sumConsumedQuantity(orderId: UUID, materialId: UUID): BigDecimal =
        movementRepo.sumQuantityByOrderIdAndMaterialIdAndType(orderId, materialId, MovementType.CONSUMPTION)

    override fun sumConsumedByOrder(orderId: UUID): Map<UUID, BigDecimal> =
        movementRepo.sumQuantityByOrderIdGroupedByMaterialId(orderId, MovementType.CONSUMPTION)
            .associate { row ->
                val materialId = row[0] as UUID
                val quantity = row[1] as BigDecimal
                materialId to quantity
            }

    // InventoryAuditPort
    override fun record(event: InventoryAuditEvent): InventoryAuditEvent =
        auditRepo.save(event.toEntity()).toDomain()

    // OrderMaterialRequirementPort
    override fun save(requirement: OrderMaterialRequirement): OrderMaterialRequirement =
        requirementRepo.save(requirement.toEntity()).toDomain()

    override fun findByLineId(id: UUID): OrderMaterialRequirement? =
        requirementRepo.findById(id).orElse(null)?.toDomain()

    override fun findByLineIdAndOrderId(id: UUID, orderId: UUID): OrderMaterialRequirement? =
        requirementRepo.findById(id).orElse(null)
            ?.takeIf { it.orderId == orderId }
            ?.toDomain()

    override fun findByOrderIdOrderByCreatedAtDesc(orderId: UUID): List<OrderMaterialRequirement> =
        requirementRepo.findByOrderIdOrderByCreatedAtDesc(orderId).map { it.toDomain() }

    override fun findByOrderIdAndMaterialId(orderId: UUID, materialId: UUID): OrderMaterialRequirement? =
        requirementRepo.findByOrderIdAndMaterialId(orderId, materialId)?.toDomain()

    override fun existsByOrderIdAndMaterialId(orderId: UUID, materialId: UUID): Boolean =
        requirementRepo.existsByOrderIdAndMaterialId(orderId, materialId)

    override fun existsInActiveOrder(materialId: UUID): Boolean =
        requirementRepo.existsInActiveOrder(materialId, OrderStatus.SHIPPED)

    override fun deleteByMaterialIdInShippedOrders(materialId: UUID): Int =
        requirementRepo.deleteByMaterialIdInOrdersWithStatus(materialId, OrderStatus.SHIPPED)

    override fun deleteLineById(id: UUID) = requirementRepo.deleteById(id)
}

// --- Mapping extensions ---

private fun MaterialEntity.toDomain() = Material(
    id = id,
    name = name,
    unit = unit,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun Material.toEntity() = MaterialEntity(
    id = id,
    name = name,
    unit = unit,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun StockMovementEntity.toDomain() = StockMovement(
    id = id,
    materialId = materialId,
    movementType = movementType,
    quantity = quantity,
    comment = comment,
    orderId = orderId,
    actorUserId = actorUserId,
    actorDisplayName = actorDisplayName,
    createdAt = createdAt,
)

private fun StockMovement.toEntity() = StockMovementEntity(
    id = id,
    materialId = materialId,
    movementType = movementType,
    quantity = quantity,
    comment = comment,
    orderId = orderId,
    actorUserId = actorUserId,
    actorDisplayName = actorDisplayName,
    createdAt = createdAt,
)

private fun OrderMaterialRequirementEntity.toDomain() = OrderMaterialRequirement(
    id = id,
    orderId = orderId,
    materialId = materialId,
    quantity = quantity,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun OrderMaterialRequirement.toEntity() = OrderMaterialRequirementEntity(
    id = id,
    orderId = orderId,
    materialId = materialId,
    quantity = quantity,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun InventoryAuditEventEntity.toDomain() = InventoryAuditEvent(
    id = id,
    eventType = eventType,
    actorUserId = actorUserId,
    targetId = requireNotNull(targetId),
    eventAt = eventAt,
    summary = summary,
    metadata = metadata,
)

private fun InventoryAuditEvent.toEntity() = InventoryAuditEventEntity(
    id = id,
    eventType = eventType,
    actorUserId = actorUserId,
    targetId = targetId,
    eventAt = eventAt,
    summary = summary,
    metadata = metadata,
)
