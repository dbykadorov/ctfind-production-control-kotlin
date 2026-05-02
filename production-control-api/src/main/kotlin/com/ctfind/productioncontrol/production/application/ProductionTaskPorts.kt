package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.production.domain.ProductionTask
import com.ctfind.productioncontrol.production.domain.ProductionTaskAuditEvent
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEvent
import java.time.LocalDate
import java.util.UUID

interface ProductionTaskPort {
	fun findById(id: UUID): ProductionTask?
	fun save(task: ProductionTask): ProductionTask
	fun search(query: ProductionTaskListQuery, currentUserId: UUID?, roleCodes: Set<String>): ProductionTaskPageResult<ProductionTask>
	fun existsByOrderItemIdAndPurpose(orderItemId: UUID, purpose: String): Boolean
	fun findOverdue(today: LocalDate): List<ProductionTask>
}

interface ProductionTaskTracePort {
	fun saveHistoryEvent(event: ProductionTaskHistoryEvent): ProductionTaskHistoryEvent
	fun findHistoryEvents(taskId: UUID): List<ProductionTaskHistoryEvent>
}

interface ProductionTaskAuditPort {
	fun record(event: ProductionTaskAuditEvent): ProductionTaskAuditEvent
}

interface ProductionTaskNumberPort {
	fun nextTaskNumber(): String
}

interface ProductionOrderSourcePort {
	fun findOrderSource(orderId: UUID): ProductionTaskOrderSummary?
	fun findOrderItemSource(orderId: UUID, orderItemId: UUID): ProductionTaskOrderItemSummary?
}

interface ProductionExecutorPort {
	fun findExecutor(id: UUID): ProductionTaskExecutorSummary?
	fun searchExecutors(search: String?, limit: Int): List<ProductionTaskExecutorSummary>
}

interface ProductionActorLookupPort {
	fun displayName(userId: UUID): String?
}
