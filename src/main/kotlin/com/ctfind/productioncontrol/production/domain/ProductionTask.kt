package com.ctfind.productioncontrol.production.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ProductionTask(
	val id: UUID = UUID.randomUUID(),
	val taskNumber: String,
	val orderId: UUID,
	val orderItemId: UUID?,
	val purpose: String,
	val itemName: String,
	val quantity: BigDecimal,
	val uom: String,
	val status: ProductionTaskStatus = ProductionTaskStatus.NOT_STARTED,
	val previousActiveStatus: ProductionTaskStatus? = null,
	val executorUserId: UUID? = null,
	val plannedStartDate: LocalDate? = null,
	val plannedFinishDate: LocalDate? = null,
	val blockedReason: String? = null,
	val createdByUserId: UUID,
	val createdAt: Instant,
	val updatedAt: Instant,
	val version: Long = 0,
) {
	init {
		require(taskNumber.isNotBlank()) { "taskNumber must not be blank" }
		require(purpose.isNotBlank()) { "purpose must not be blank" }
		require(itemName.isNotBlank()) { "itemName must not be blank" }
		require(quantity > BigDecimal.ZERO) { "quantity must be greater than zero" }
		require(uom.isNotBlank()) { "uom must not be blank" }
		if (plannedStartDate != null && plannedFinishDate != null) {
			require(!plannedFinishDate.isBefore(plannedStartDate)) {
				"plannedFinishDate must not be before plannedStartDate"
			}
		}
	}
}

data class ProductionTaskHistoryEvent(
	val id: UUID = UUID.randomUUID(),
	val taskId: UUID,
	val eventType: ProductionTaskHistoryEventType,
	val actorUserId: UUID,
	val eventAt: Instant,
	val fromStatus: ProductionTaskStatus? = null,
	val toStatus: ProductionTaskStatus? = null,
	val previousExecutorUserId: UUID? = null,
	val newExecutorUserId: UUID? = null,
	val plannedStartDateBefore: LocalDate? = null,
	val plannedStartDateAfter: LocalDate? = null,
	val plannedFinishDateBefore: LocalDate? = null,
	val plannedFinishDateAfter: LocalDate? = null,
	val reason: String? = null,
	val note: String? = null,
)

data class ProductionTaskAuditEvent(
	val id: UUID = UUID.randomUUID(),
	val eventType: String,
	val actorUserId: UUID,
	val targetType: String = "PRODUCTION_TASK",
	val targetId: UUID,
	val eventAt: Instant,
	val summary: String,
	val metadata: String? = null,
)
