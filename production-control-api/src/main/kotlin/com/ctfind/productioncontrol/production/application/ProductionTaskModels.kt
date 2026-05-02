package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.orders.domain.OrderStatus
import com.ctfind.productioncontrol.production.domain.ProductionTaskAction
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEventType
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ProductionTaskListQuery(
	val search: String? = null,
	val status: ProductionTaskStatus? = null,
	val orderId: UUID? = null,
	val orderItemId: UUID? = null,
	val executorUserId: UUID? = null,
	val assignedToMe: Boolean = false,
	val blockedOnly: Boolean = false,
	val activeOnly: Boolean = false,
	val dueDateFrom: LocalDate? = null,
	val dueDateTo: LocalDate? = null,
	val page: Int = 0,
	val size: Int = 20,
	val sort: String? = null,
)

data class ProductionTaskPageResult<T>(
	val items: List<T>,
	val page: Int,
	val size: Int,
	val totalItems: Long,
) {
	val totalPages: Int =
		if (size <= 0) 0 else ((totalItems + size - 1) / size).toInt()
}

data class ProductionTaskOrderSummary(
	val id: UUID,
	val orderNumber: String,
	val customerDisplayName: String,
	val status: OrderStatus,
	val deliveryDate: LocalDate,
)

data class ProductionTaskOrderItemSummary(
	val id: UUID,
	val lineNo: Int,
	val itemName: String,
	val quantity: BigDecimal,
	val uom: String,
)

data class ProductionTaskExecutorSummary(
	val id: UUID,
	val displayName: String,
	val login: String,
)

data class ProductionTaskListRowView(
	val id: UUID,
	val taskNumber: String,
	val purpose: String,
	val order: ProductionTaskOrderSummary,
	val orderItem: ProductionTaskOrderItemSummary?,
	val quantity: BigDecimal,
	val uom: String,
	val status: ProductionTaskStatus,
	val statusLabel: String,
	val previousActiveStatus: ProductionTaskStatus?,
	val executor: ProductionTaskExecutorSummary?,
	val plannedStartDate: LocalDate?,
	val plannedFinishDate: LocalDate?,
	val blockedReason: String?,
	val updatedAt: Instant,
	val version: Long,
)

data class ProductionTaskHistoryEventView(
	val type: ProductionTaskHistoryEventType,
	val actorDisplayName: String,
	val eventAt: Instant,
	val fromStatus: ProductionTaskStatus?,
	val toStatus: ProductionTaskStatus?,
	val previousExecutorDisplayName: String?,
	val newExecutorDisplayName: String?,
	val plannedStartDateBefore: LocalDate?,
	val plannedStartDateAfter: LocalDate?,
	val plannedFinishDateBefore: LocalDate?,
	val plannedFinishDateAfter: LocalDate?,
	val note: String?,
	val reason: String?,
)

data class ProductionTaskDetailView(
	val row: ProductionTaskListRowView,
	val allowedActions: Set<ProductionTaskAction>,
	val history: List<ProductionTaskHistoryEventView>,
	val createdAt: Instant,
)

data class AuthenticatedProductionActor(
	val userId: UUID,
	val login: String,
	val displayName: String,
	val roleCodes: Set<String>,
)

sealed interface ProductionTaskDetailQueryResult {
	data class Found(val detail: ProductionTaskDetailView) : ProductionTaskDetailQueryResult
	data object NotFound : ProductionTaskDetailQueryResult
	data object Forbidden : ProductionTaskDetailQueryResult
}

data class CreateProductionTaskDraft(
	val orderItemId: UUID?,
	val purpose: String,
	val quantity: BigDecimal,
	val uom: String,
	val executorUserId: UUID? = null,
	val plannedStartDate: LocalDate? = null,
	val plannedFinishDate: LocalDate? = null,
)

data class CreateProductionTasksFromOrderCommand(
	val orderId: UUID,
	val tasks: List<CreateProductionTaskDraft>,
	val actorUserId: UUID,
	val roleCodes: Set<String>,
)

data class AssignProductionTaskCommand(
	val taskId: UUID,
	val expectedVersion: Long,
	val executorUserId: UUID,
	val plannedStartDate: LocalDate?,
	val plannedFinishDate: LocalDate?,
	val note: String?,
	val actorUserId: UUID,
	val roleCodes: Set<String>,
)

data class ChangeProductionTaskStatusCommand(
	val taskId: UUID,
	val expectedVersion: Long,
	val toStatus: ProductionTaskStatus,
	val reason: String?,
	val note: String?,
	val actorUserId: UUID,
	val roleCodes: Set<String>,
)

data class CreatedProductionTaskSummary(
	val id: UUID,
	val taskNumber: String,
	val status: ProductionTaskStatus,
	val version: Long,
)

sealed interface ProductionTaskMutationResult<out T> {
	data class Success<T>(val value: T) : ProductionTaskMutationResult<T>
	data object Forbidden : ProductionTaskMutationResult<Nothing>
	data object NotFound : ProductionTaskMutationResult<Nothing>
	data object StaleVersion : ProductionTaskMutationResult<Nothing>
	data class ValidationFailed(
		val message: String,
		val errorCode: String = "validation_failed",
		val details: Map<String, String> = emptyMap(),
	) : ProductionTaskMutationResult<Nothing>
	data object InvalidTransition : ProductionTaskMutationResult<Nothing>
}
