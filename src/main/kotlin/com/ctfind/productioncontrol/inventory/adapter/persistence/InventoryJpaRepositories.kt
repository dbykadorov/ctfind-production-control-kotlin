package com.ctfind.productioncontrol.inventory.adapter.persistence

import com.ctfind.productioncontrol.inventory.domain.MovementType
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.util.UUID

interface MaterialJpaRepository : JpaRepository<MaterialEntity, UUID> {
    fun existsByNameIgnoreCase(name: String): Boolean
    fun existsByNameIgnoreCaseAndIdNot(name: String, id: UUID): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MaterialEntity m WHERE m.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): MaterialEntity?

    @Query("SELECT m FROM MaterialEntity m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    fun findAllBySearchTerm(@Param("search") search: String, pageable: Pageable): Page<MaterialEntity>

    @Query("SELECT COUNT(m) FROM MaterialEntity m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    fun countBySearchTerm(@Param("search") search: String): Long
}

interface StockMovementJpaRepository : JpaRepository<StockMovementEntity, UUID> {
    fun findByMaterialIdOrderByCreatedAtDesc(materialId: UUID, pageable: Pageable): Page<StockMovementEntity>
    fun countByMaterialId(materialId: UUID): Long
    fun existsByMaterialId(materialId: UUID): Boolean
    fun existsByOrderIdAndMaterialIdAndMovementType(orderId: UUID, materialId: UUID, movementType: MovementType): Boolean

    @Query(
        value = """
            SELECT COALESCE(
                SUM(CASE
                    WHEN movement_type = 'RECEIPT' THEN quantity
                    ELSE -quantity
                END), 0
            )
            FROM stock_movement
            WHERE material_id = :materialId
        """,
        nativeQuery = true,
    )
    fun sumQuantityByMaterialId(@Param("materialId") materialId: UUID): BigDecimal

    @Query(
        "SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovementEntity m " +
            "WHERE m.materialId = :materialId AND m.movementType = :movementType",
    )
    fun sumQuantityByMaterialIdAndType(
        @Param("materialId") materialId: UUID,
        @Param("movementType") movementType: MovementType,
    ): BigDecimal

    @Query(
        "SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovementEntity m " +
            "WHERE m.orderId = :orderId AND m.materialId = :materialId AND m.movementType = :movementType",
    )
    fun sumQuantityByOrderIdAndMaterialIdAndType(
        @Param("orderId") orderId: UUID,
        @Param("materialId") materialId: UUID,
        @Param("movementType") movementType: MovementType,
    ): BigDecimal

    @Query(
        "SELECT m.materialId, COALESCE(SUM(m.quantity), 0) FROM StockMovementEntity m " +
            "WHERE m.orderId = :orderId AND m.movementType = :movementType GROUP BY m.materialId",
    )
    fun sumQuantityByOrderIdGroupedByMaterialId(
        @Param("orderId") orderId: UUID,
        @Param("movementType") movementType: MovementType,
    ): List<Array<Any>>
}

interface InventoryAuditEventJpaRepository : JpaRepository<InventoryAuditEventEntity, UUID>

interface OrderMaterialRequirementJpaRepository : JpaRepository<OrderMaterialRequirementEntity, UUID> {
    @Query("SELECT DISTINCT omr.orderId FROM OrderMaterialRequirementEntity omr")
    fun findDistinctOrderIds(): List<UUID>

    fun findByOrderIdOrderByCreatedAtDesc(orderId: UUID): List<OrderMaterialRequirementEntity>
    fun findByOrderIdAndMaterialId(orderId: UUID, materialId: UUID): OrderMaterialRequirementEntity?
    fun existsByOrderIdAndMaterialId(orderId: UUID, materialId: UUID): Boolean
    fun existsByMaterialId(materialId: UUID): Boolean

    @Query(
        "SELECT CASE WHEN COUNT(omr) > 0 THEN true ELSE false END FROM OrderMaterialRequirementEntity omr " +
            "WHERE omr.materialId = :materialId AND EXISTS (" +
            "SELECT 1 FROM CustomerOrderEntity o WHERE o.id = omr.orderId AND o.status <> :shippedStatus" +
            ")",
    )
    fun existsInActiveOrder(
        @Param("materialId") materialId: UUID,
        @Param("shippedStatus") shippedStatus: OrderStatus,
    ): Boolean

    @Modifying
    @Query(
        "DELETE FROM OrderMaterialRequirementEntity omr " +
            "WHERE omr.materialId = :materialId AND EXISTS (" +
            "SELECT 1 FROM CustomerOrderEntity o WHERE o.id = omr.orderId AND o.status = :shippedStatus" +
            ")",
    )
    fun deleteByMaterialIdInOrdersWithStatus(
        @Param("materialId") materialId: UUID,
        @Param("shippedStatus") shippedStatus: OrderStatus,
    ): Int
}
