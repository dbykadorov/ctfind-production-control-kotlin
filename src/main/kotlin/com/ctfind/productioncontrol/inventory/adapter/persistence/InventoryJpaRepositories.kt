package com.ctfind.productioncontrol.inventory.adapter.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.util.UUID

interface MaterialJpaRepository : JpaRepository<MaterialEntity, UUID> {
    fun existsByNameIgnoreCase(name: String): Boolean
    fun existsByNameIgnoreCaseAndIdNot(name: String, id: UUID): Boolean

    @Query("SELECT m FROM MaterialEntity m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    fun findAllBySearchTerm(@Param("search") search: String, pageable: Pageable): Page<MaterialEntity>

    @Query("SELECT COUNT(m) FROM MaterialEntity m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    fun countBySearchTerm(@Param("search") search: String): Long
}

interface StockMovementJpaRepository : JpaRepository<StockMovementEntity, UUID> {
    fun findByMaterialIdOrderByCreatedAtDesc(materialId: UUID, pageable: Pageable): Page<StockMovementEntity>
    fun countByMaterialId(materialId: UUID): Long
    fun existsByMaterialId(materialId: UUID): Boolean

    @Query("SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovementEntity m WHERE m.materialId = :materialId")
    fun sumQuantityByMaterialId(@Param("materialId") materialId: UUID): BigDecimal
}

interface InventoryAuditEventJpaRepository : JpaRepository<InventoryAuditEventEntity, UUID>
