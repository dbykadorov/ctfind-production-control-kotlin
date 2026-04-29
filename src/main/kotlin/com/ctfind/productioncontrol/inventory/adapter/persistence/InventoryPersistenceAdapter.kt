package com.ctfind.productioncontrol.inventory.adapter.persistence

import com.ctfind.productioncontrol.inventory.application.InventoryAuditPort
import com.ctfind.productioncontrol.inventory.application.MaterialListQuery
import com.ctfind.productioncontrol.inventory.application.MaterialPort
import com.ctfind.productioncontrol.inventory.application.StockMovementPort
import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import com.ctfind.productioncontrol.inventory.domain.Material
import com.ctfind.productioncontrol.inventory.domain.StockMovement
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
) : MaterialPort, StockMovementPort, InventoryAuditPort {

    // MaterialPort
    override fun findById(id: UUID): Material? = materialRepo.findById(id).orElse(null)?.toDomain()

    override fun findAll(query: MaterialListQuery): List<Material> {
        val pageable = PageRequest.of(query.page, query.size, Sort.by("name"))
        return materialRepo.findAllBySearchTerm(query.search, pageable).content.map { it.toDomain() }
    }

    override fun count(query: MaterialListQuery): Long = materialRepo.countBySearchTerm(query.search)

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

    // InventoryAuditPort
    override fun record(event: InventoryAuditEvent): InventoryAuditEvent =
        auditRepo.save(event.toEntity()).toDomain()
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
    actorUserId = actorUserId,
    actorDisplayName = actorDisplayName,
    createdAt = createdAt,
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
