package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import com.ctfind.productioncontrol.inventory.domain.Material
import com.ctfind.productioncontrol.inventory.domain.StockMovement
import java.math.BigDecimal
import java.util.UUID

interface MaterialPort {
    fun findById(id: UUID): Material?
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
}

interface InventoryAuditPort {
    fun record(event: InventoryAuditEvent): InventoryAuditEvent
}
