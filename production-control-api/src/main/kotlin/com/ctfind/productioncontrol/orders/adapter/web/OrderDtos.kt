package com.ctfind.productioncontrol.orders.adapter.web

import com.ctfind.productioncontrol.orders.application.CustomerSummary
import com.ctfind.productioncontrol.orders.application.DashboardSummary
import com.ctfind.productioncontrol.orders.application.OrderChangeDiffView
import com.ctfind.productioncontrol.orders.application.OrderDetailView
import com.ctfind.productioncontrol.orders.application.OrderItemCommand
import com.ctfind.productioncontrol.orders.application.OrderItemView
import com.ctfind.productioncontrol.orders.application.OrderListItemView
import com.ctfind.productioncontrol.orders.application.OrderStatusChangeView
import com.ctfind.productioncontrol.orders.application.PageResult
import com.ctfind.productioncontrol.orders.domain.CustomerStatus
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class OrderApiErrorResponse(
	val code: String,
	val message: String,
	val details: Map<String, String> = emptyMap(),
)

data class CustomerSearchResponse(
	val items: List<CustomerResponse>,
)

data class CustomerResponse(
	val id: UUID,
	val displayName: String,
	val status: CustomerStatus,
	val contactPerson: String?,
	val phone: String?,
	val email: String?,
)

data class OrderPageResponse(
	val items: List<OrderListItemResponse>,
	val page: Int,
	val size: Int,
	val totalItems: Long,
	val totalPages: Int,
)

data class OrderListItemResponse(
	val id: UUID,
	val orderNumber: String,
	val customer: CustomerResponse,
	val deliveryDate: LocalDate,
	val status: OrderStatus,
	val statusLabel: String,
	val updatedAt: Instant,
	val createdAt: Instant,
	val version: Long,
	val overdue: Boolean,
)

data class OrderDetailResponse(
	val id: UUID,
	val orderNumber: String,
	val customer: CustomerResponse,
	val deliveryDate: LocalDate,
	val status: OrderStatus,
	val statusLabel: String,
	val notes: String?,
	val items: List<OrderItemResponse>,
	val history: List<OrderStatusChangeResponse>,
	val changeDiffs: List<OrderChangeDiffResponse>,
	val createdAt: Instant,
	val updatedAt: Instant,
	val version: Long,
	val overdue: Boolean,
)

data class OrderItemResponse(
	val id: UUID,
	val lineNo: Int,
	val itemName: String,
	val quantity: BigDecimal,
	val uom: String,
)

data class OrderStatusChangeResponse(
	val type: String,
	val fromStatus: OrderStatus?,
	val toStatus: OrderStatus?,
	val actorDisplayName: String,
	val changedAt: Instant,
	val note: String?,
)

data class OrderFieldDiffResponse(
	val fieldname: String,
	val fieldLabel: String?,
	val fromValue: String?,
	val toValue: String?,
)

data class OrderChangeDiffResponse(
	val type: String,
	val actorDisplayName: String,
	val changedAt: Instant,
	val fieldDiffs: List<OrderFieldDiffResponse>,
)

data class CreateOrderRequest(
	@field:NotNull
	val customerId: UUID?,
	@field:NotNull
	val deliveryDate: LocalDate?,
	val notes: String? = null,
	@field:Valid
	@field:NotEmpty
	val items: List<OrderItemRequest> = emptyList(),
)

data class UpdateOrderRequest(
	@field:PositiveOrZero
	val expectedVersion: Long,
	@field:NotNull
	val customerId: UUID?,
	@field:NotNull
	val deliveryDate: LocalDate?,
	val notes: String? = null,
	@field:Valid
	@field:NotEmpty
	val items: List<OrderItemRequest> = emptyList(),
)

data class OrderItemRequest(
	@field:NotBlank
	val itemName: String,
	@field:DecimalMin(value = "0.001")
	val quantity: BigDecimal,
	@field:NotBlank
	val uom: String,
)

data class ChangeOrderStatusRequest(
	@field:PositiveOrZero
	val expectedVersion: Long,
	@field:NotNull
	val toStatus: OrderStatus?,
	val note: String? = null,
)

data class DashboardSummaryResponse(
	val totalOrders: Long,
	val activeOrders: Long,
	val overdueOrders: Long,
	val statusCounts: Map<OrderStatus, Long>,
	val recentChanges: List<OrderStatusChangeSummaryResponse>,
	val trend: List<OrderTrendPointResponse>,
)

data class OrderStatusChangeSummaryResponse(
	val orderId: UUID,
	val orderNumber: String,
	val customerDisplayName: String,
	val fromStatus: OrderStatus?,
	val toStatus: OrderStatus,
	val changedAt: Instant,
	val actorDisplayName: String,
)

data class OrderTrendPointResponse(
	val date: LocalDate,
	val created: Long,
	val shipped: Long,
)

fun CustomerSummary.toResponse(): CustomerResponse =
	CustomerResponse(
		id = id,
		displayName = displayName,
		status = status,
		contactPerson = contactPerson,
		phone = phone,
		email = email,
	)

fun OrderListItemView.toResponse(): OrderListItemResponse =
	OrderListItemResponse(
		id = id,
		orderNumber = orderNumber,
		customer = customer.toResponse(),
		deliveryDate = deliveryDate,
		status = status,
		statusLabel = status.label,
		updatedAt = updatedAt,
		createdAt = createdAt,
		version = version,
		overdue = overdue,
	)

fun PageResult<OrderListItemView>.toOrderPageResponse(): OrderPageResponse =
	OrderPageResponse(
		items = items.map { it.toResponse() },
		page = page,
		size = size,
		totalItems = totalItems,
		totalPages = totalPages,
	)

fun OrderDetailView.toResponse(): OrderDetailResponse =
	OrderDetailResponse(
		id = id,
		orderNumber = orderNumber,
		customer = customer.toResponse(),
		deliveryDate = deliveryDate,
		status = status,
		statusLabel = status.label,
		notes = notes,
		items = items.map { it.toResponse() },
		history = history.map { it.toResponse() },
		changeDiffs = changeDiffs.map { it.toResponse() },
		createdAt = createdAt,
		updatedAt = updatedAt,
		version = version,
		overdue = overdue,
	)

fun OrderItemView.toResponse(): OrderItemResponse =
	OrderItemResponse(id, lineNo, itemName, quantity, uom)

fun OrderStatusChangeView.toResponse(): OrderStatusChangeResponse =
	OrderStatusChangeResponse(
		type = type.name,
		fromStatus = fromStatus,
		toStatus = toStatus,
		actorDisplayName = actorDisplayName,
		changedAt = changedAt,
		note = note,
	)

fun OrderChangeDiffView.toResponse(): OrderChangeDiffResponse =
	OrderChangeDiffResponse(
		type = type.name,
		actorDisplayName = actorDisplayName,
		changedAt = changedAt,
		fieldDiffs = fieldDiffs.map {
			OrderFieldDiffResponse(
				fieldname = it.fieldName,
				fieldLabel = it.fieldLabel,
				fromValue = it.fromValue,
				toValue = it.toValue,
			)
		},
	)

fun OrderItemRequest.toCommand(): OrderItemCommand =
	OrderItemCommand(itemName = itemName, quantity = quantity, uom = uom)

fun DashboardSummary.toResponse(): DashboardSummaryResponse =
	DashboardSummaryResponse(
		totalOrders = totalOrders,
		activeOrders = activeOrders,
		overdueOrders = overdueOrders,
		statusCounts = statusCounts,
		recentChanges = recentChanges.map {
			OrderStatusChangeSummaryResponse(
				orderId = it.orderId,
				orderNumber = it.orderNumber,
				customerDisplayName = it.customerDisplayName,
				fromStatus = it.fromStatus,
				toStatus = it.toStatus,
				changedAt = it.changedAt,
				actorDisplayName = it.actorDisplayName,
			)
		},
		trend = trend.map {
			OrderTrendPointResponse(
				date = it.date,
				created = it.created,
				shipped = it.shipped,
			)
		},
	)
