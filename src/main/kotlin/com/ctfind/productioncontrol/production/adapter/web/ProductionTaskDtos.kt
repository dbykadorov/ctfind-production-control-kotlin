package com.ctfind.productioncontrol.production.adapter.web

import com.ctfind.productioncontrol.orders.domain.OrderStatus
import com.ctfind.productioncontrol.production.application.AuthenticatedProductionActor
import com.ctfind.productioncontrol.production.application.ProductionTaskDetailView
import com.ctfind.productioncontrol.production.application.ProductionTaskExecutorSummary
import com.ctfind.productioncontrol.production.application.ProductionTaskHistoryEventView
import com.ctfind.productioncontrol.production.application.ProductionTaskListRowView
import com.ctfind.productioncontrol.production.application.ProductionTaskOrderItemSummary
import com.ctfind.productioncontrol.production.application.ProductionTaskOrderSummary
import com.ctfind.productioncontrol.production.domain.ProductionTaskAction
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEventType
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import org.springframework.security.oauth2.jwt.Jwt
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ProductionTaskApiErrorResponse(
	val code: String,
	val message: String,
	val details: Map<String, String> = emptyMap(),
)

data class ProductionTaskOrderListResponse(
	val id: UUID,
	val orderNumber: String,
	val customerDisplayName: String,
	val deliveryDate: LocalDate,
)

data class ProductionTaskOrderDetailResponse(
	val id: UUID,
	val orderNumber: String,
	val customerDisplayName: String,
	val status: OrderStatus,
	val deliveryDate: LocalDate,
)

data class ProductionTaskOrderItemResponse(
	val id: UUID,
	val lineNo: Int,
	val itemName: String,
	val quantity: BigDecimal,
	val uom: String,
)

data class ProductionTaskExecutorResponse(
	val id: UUID,
	val displayName: String,
)

data class ProductionTaskListItemResponse(
	val id: UUID,
	val taskNumber: String,
	val purpose: String,
	val order: ProductionTaskOrderListResponse,
	val orderItem: ProductionTaskOrderItemResponse?,
	val quantity: BigDecimal,
	val uom: String,
	val status: ProductionTaskStatus,
	val statusLabel: String,
	val executor: ProductionTaskExecutorResponse?,
	val plannedStartDate: LocalDate?,
	val plannedFinishDate: LocalDate?,
	val blockedReason: String?,
	val updatedAt: Instant,
	val version: Long,
)

data class ProductionTaskPageResponse(
	val items: List<ProductionTaskListItemResponse>,
	val page: Int,
	val size: Int,
	val totalItems: Long,
	val totalPages: Int,
)

data class ProductionTaskHistoryEventResponse(
	val type: ProductionTaskHistoryEventType,
	val actorDisplayName: String,
	val eventAt: Instant,
	val fromStatus: ProductionTaskStatus?,
	val toStatus: ProductionTaskStatus?,
	val note: String?,
	val reason: String?,
)

data class ProductionTaskDetailResponse(
	val id: UUID,
	val taskNumber: String,
	val purpose: String,
	val order: ProductionTaskOrderDetailResponse,
	val orderItem: ProductionTaskOrderItemResponse?,
	val quantity: BigDecimal,
	val uom: String,
	val status: ProductionTaskStatus,
	val statusLabel: String,
	val executor: ProductionTaskExecutorResponse?,
	val plannedStartDate: LocalDate?,
	val plannedFinishDate: LocalDate?,
	val blockedReason: String?,
	val allowedActions: List<ProductionTaskAction>,
	val history: List<ProductionTaskHistoryEventResponse>,
	val createdAt: Instant,
	val updatedAt: Instant,
	val version: Long,
)

fun ProductionTaskListRowView.toListItemResponse(): ProductionTaskListItemResponse =
	ProductionTaskListItemResponse(
		id = id,
		taskNumber = taskNumber,
		purpose = purpose,
		order = order.toOrderListResponse(),
		orderItem = orderItem?.toOrderItemResponse(),
		quantity = quantity,
		uom = uom,
		status = status,
		statusLabel = statusLabel,
		executor = executor?.toExecutorResponse(),
		plannedStartDate = plannedStartDate,
		plannedFinishDate = plannedFinishDate,
		blockedReason = blockedReason,
		updatedAt = updatedAt,
		version = version,
	)

fun ProductionTaskDetailView.toDetailResponse(): ProductionTaskDetailResponse =
	ProductionTaskDetailResponse(
		id = row.id,
		taskNumber = row.taskNumber,
		purpose = row.purpose,
		order = row.order.toOrderDetailResponse(),
		orderItem = row.orderItem?.toOrderItemResponse(),
		quantity = row.quantity,
		uom = row.uom,
		status = row.status,
		statusLabel = row.statusLabel,
		executor = row.executor?.toExecutorResponse(),
		plannedStartDate = row.plannedStartDate,
		plannedFinishDate = row.plannedFinishDate,
		blockedReason = row.blockedReason,
		allowedActions = allowedActions.sortedBy { it.ordinal }.toList(),
		history = history.map { it.toHistoryResponse() },
		createdAt = createdAt,
		updatedAt = row.updatedAt,
		version = row.version,
	)

private fun ProductionTaskOrderSummary.toOrderListResponse(): ProductionTaskOrderListResponse =
	ProductionTaskOrderListResponse(
		id = id,
		orderNumber = orderNumber,
		customerDisplayName = customerDisplayName,
		deliveryDate = deliveryDate,
	)

private fun ProductionTaskOrderSummary.toOrderDetailResponse(): ProductionTaskOrderDetailResponse =
	ProductionTaskOrderDetailResponse(
		id = id,
		orderNumber = orderNumber,
		customerDisplayName = customerDisplayName,
		status = status,
		deliveryDate = deliveryDate,
	)

private fun ProductionTaskOrderItemSummary.toOrderItemResponse(): ProductionTaskOrderItemResponse =
	ProductionTaskOrderItemResponse(
		id = id,
		lineNo = lineNo,
		itemName = itemName,
		quantity = quantity,
		uom = uom,
	)

private fun ProductionTaskExecutorSummary.toExecutorResponse(): ProductionTaskExecutorResponse =
	ProductionTaskExecutorResponse(id = id, displayName = displayName)

private fun ProductionTaskHistoryEventView.toHistoryResponse(): ProductionTaskHistoryEventResponse =
	ProductionTaskHistoryEventResponse(
		type = type,
		actorDisplayName = actorDisplayName,
		eventAt = eventAt,
		fromStatus = fromStatus,
		toStatus = toStatus,
		note = note,
		reason = reason,
	)

fun Jwt.toProductionActor(): AuthenticatedProductionActor =
	AuthenticatedProductionActor(
		userId = (claims["userId"] as? String)?.let(UUID::fromString)
			?: UUID.nameUUIDFromBytes(subject.toByteArray()),
		login = subject,
		displayName = claims["displayName"] as? String ?: subject,
		roleCodes = jwtRoles(),
	)

@Suppress("UNCHECKED_CAST")
private fun Jwt.jwtRoles(): Set<String> =
	when (val raw = claims["roles"]) {
		is Collection<*> -> raw.filterIsInstance<String>().toSet()
		is String -> setOf(raw)
		else -> emptySet()
	}
