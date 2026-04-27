package com.ctfind.productioncontrol.production.adapter.persistence

import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProductionTaskJpaRepository : JpaRepository<ProductionTaskEntity, UUID> {
	fun existsByOrderItemIdAndPurposeIgnoreCase(orderItemId: UUID, purpose: String): Boolean
	fun findByStatus(status: ProductionTaskStatus): List<ProductionTaskEntity>
	fun findByExecutorUserId(executorUserId: UUID): List<ProductionTaskEntity>
}

interface ProductionTaskHistoryEventJpaRepository : JpaRepository<ProductionTaskHistoryEventEntity, UUID> {
	fun findByTaskIdOrderByEventAtAsc(taskId: UUID): List<ProductionTaskHistoryEventEntity>
}

interface ProductionTaskAuditEventJpaRepository : JpaRepository<ProductionTaskAuditEventEntity, UUID>
