package com.ctfind.productioncontrol.orders.application

import com.ctfind.productioncontrol.orders.domain.CustomerStatus
import com.ctfind.productioncontrol.orders.domain.OrderChangeType
import com.ctfind.productioncontrol.orders.domain.OrderFieldDiff
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class AuthenticatedOrderActor(
	val userId: UUID,
	val login: String,
	val displayName: String,
	val roleCodes: Set<String>,
) {
	val canWriteOrders: Boolean get() = com.ctfind.productioncontrol.orders.application.canWriteOrders(roleCodes)
}

data class CustomerSummary(
	val id: UUID,
	val displayName: String,
	val status: CustomerStatus,
	val contactPerson: String? = null,
	val phone: String? = null,
	val email: String? = null,
)

data class CustomerSearchQuery(
	val search: String? = null,
	val activeOnly: Boolean = true,
	val limit: Int = 20,
)

data class OrderListQuery(
	val search: String? = null,
	val status: OrderStatus? = null,
	val customerId: UUID? = null,
	val activeOnly: Boolean = false,
	val overdueOnly: Boolean = false,
	val deliveryDateFrom: LocalDate? = null,
	val deliveryDateTo: LocalDate? = null,
	val page: Int = 0,
	val size: Int = 20,
)

data class OrderItemCommand(
	val itemName: String,
	val quantity: BigDecimal,
	val uom: String,
)

data class CreateOrderCommand(
	val customerId: UUID,
	val deliveryDate: LocalDate,
	val notes: String? = null,
	val items: List<OrderItemCommand>,
	val actor: AuthenticatedOrderActor,
)

data class UpdateOrderCommand(
	val orderId: UUID,
	val expectedVersion: Long,
	val customerId: UUID,
	val deliveryDate: LocalDate,
	val notes: String? = null,
	val items: List<OrderItemCommand>,
	val actor: AuthenticatedOrderActor,
)

data class ChangeOrderStatusCommand(
	val orderId: UUID,
	val expectedVersion: Long,
	val toStatus: OrderStatus,
	val note: String? = null,
	val actor: AuthenticatedOrderActor,
)

data class OrderItemView(
	val id: UUID,
	val lineNo: Int,
	val itemName: String,
	val quantity: BigDecimal,
	val uom: String,
)

data class OrderListItemView(
	val id: UUID,
	val orderNumber: String,
	val customer: CustomerSummary,
	val deliveryDate: LocalDate,
	val status: OrderStatus,
	val updatedAt: Instant,
	val createdAt: Instant,
	val version: Long,
	val overdue: Boolean,
)

data class OrderStatusChangeView(
	val type: OrderChangeType = OrderChangeType.STATUS_CHANGED,
	val fromStatus: OrderStatus?,
	val toStatus: OrderStatus,
	val actorDisplayName: String,
	val changedAt: Instant,
	val note: String? = null,
)

data class OrderChangeDiffView(
	val type: OrderChangeType,
	val actorDisplayName: String,
	val changedAt: Instant,
	val fieldDiffs: List<OrderFieldDiff>,
)

data class OrderDetailView(
	val id: UUID,
	val orderNumber: String,
	val customer: CustomerSummary,
	val deliveryDate: LocalDate,
	val status: OrderStatus,
	val notes: String? = null,
	val items: List<OrderItemView>,
	val history: List<OrderStatusChangeView>,
	val changeDiffs: List<OrderChangeDiffView>,
	val createdAt: Instant,
	val updatedAt: Instant,
	val version: Long,
	val overdue: Boolean,
)

data class PageResult<T>(
	val items: List<T>,
	val page: Int,
	val size: Int,
	val totalItems: Long,
) {
	val totalPages: Int =
		if (size <= 0) 0 else ((totalItems + size - 1) / size).toInt()
}

data class OrderStatusChangeSummary(
	val orderId: UUID,
	val orderNumber: String,
	val customerDisplayName: String,
	val fromStatus: OrderStatus?,
	val toStatus: OrderStatus,
	val changedAt: Instant,
	val actorDisplayName: String,
)

data class OrderTrendPoint(
	val date: LocalDate,
	val created: Long,
	val shipped: Long,
)

data class DashboardSummary(
	val totalOrders: Long,
	val activeOrders: Long,
	val overdueOrders: Long,
	val statusCounts: Map<OrderStatus, Long>,
	val recentChanges: List<OrderStatusChangeSummary>,
	val trend: List<OrderTrendPoint>,
)

sealed interface OrderMutationResult<out T> {
	data class Success<T>(val value: T) : OrderMutationResult<T>
	data object Forbidden : OrderMutationResult<Nothing>
	data object NotFound : OrderMutationResult<Nothing>
	data object StaleVersion : OrderMutationResult<Nothing>
	data class ValidationFailed(val message: String, val field: String? = null) : OrderMutationResult<Nothing>
	data object InvalidTransition : OrderMutationResult<Nothing>
}
