package com.ctfind.productioncontrol.production.adapter.persistence

import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface ProductionTaskJpaRepository : JpaRepository<ProductionTaskEntity, UUID> {
	fun existsByOrderItemIdAndPurposeIgnoreCase(orderItemId: UUID, purpose: String): Boolean
	fun findByStatus(status: ProductionTaskStatus): List<ProductionTaskEntity>
	fun findByExecutorUserId(executorUserId: UUID): List<ProductionTaskEntity>

	@Query(
		"SELECT t FROM ProductionTaskEntity t " +
			"WHERE t.plannedFinishDate IS NOT NULL " +
			"AND t.plannedFinishDate < :today " +
			"AND t.status <> com.ctfind.productioncontrol.production.domain.ProductionTaskStatus.COMPLETED",
	)
	fun findOverdue(@Param("today") today: LocalDate): List<ProductionTaskEntity>
}

interface ProductionTaskHistoryEventJpaRepository : JpaRepository<ProductionTaskHistoryEventEntity, UUID> {
	fun findByTaskIdOrderByEventAtAsc(taskId: UUID): List<ProductionTaskHistoryEventEntity>
}

interface ProductionTaskAuditEventJpaRepository : JpaRepository<ProductionTaskAuditEventEntity, UUID>
