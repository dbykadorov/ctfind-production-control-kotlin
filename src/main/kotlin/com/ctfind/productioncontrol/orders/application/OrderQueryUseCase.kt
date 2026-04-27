package com.ctfind.productioncontrol.orders.application

import com.ctfind.productioncontrol.orders.domain.CustomerOrder
import com.ctfind.productioncontrol.orders.domain.OrderChangeDiff
import com.ctfind.productioncontrol.orders.domain.OrderStatusChange
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class OrderQueryUseCase(
	private val orders: CustomerOrderPort,
	private val customers: CustomerPort,
	private val traces: OrderTracePort,
) {
	fun list(query: OrderListQuery, today: LocalDate = LocalDate.now()): PageResult<OrderListItemView> {
		val page = orders.search(query, today)
		return PageResult(
			items = page.items.map { it.toListItemView(today) },
			page = page.page,
			size = page.size,
			totalItems = page.totalItems,
		)
	}

	fun detail(orderId: UUID, today: LocalDate = LocalDate.now()): OrderDetailView? {
		val order = orders.findById(orderId) ?: return null
		val customer = customerSummary(order.customerId) ?: return null
		return OrderDetailView(
			id = order.id,
			orderNumber = order.orderNumber,
			customer = customer,
			deliveryDate = order.deliveryDate,
			status = order.status,
			notes = order.notes,
			items = order.items.map {
				OrderItemView(
					id = it.id,
					lineNo = it.lineNo,
					itemName = it.itemName,
					quantity = it.quantity,
					uom = it.uom,
				)
			},
			history = traces.findStatusChanges(order.id).map { it.toView() },
			changeDiffs = traces.findChangeDiffs(order.id).map { it.toView() },
			createdAt = order.createdAt,
			updatedAt = order.updatedAt,
			version = order.version,
			overdue = order.isOverdue(today),
		)
	}

	private fun CustomerOrder.toListItemView(today: LocalDate): OrderListItemView =
		OrderListItemView(
			id = id,
			orderNumber = orderNumber,
			customer = customerSummary(customerId) ?: CustomerSummary(
				id = customerId,
				displayName = "Unknown customer",
				status = com.ctfind.productioncontrol.orders.domain.CustomerStatus.INACTIVE,
			),
			deliveryDate = deliveryDate,
			status = status,
			updatedAt = updatedAt,
			createdAt = createdAt,
			version = version,
			overdue = isOverdue(today),
		)

	private fun CustomerOrder.isOverdue(today: LocalDate): Boolean =
		deliveryDate.isBefore(today) && status != com.ctfind.productioncontrol.orders.domain.OrderStatus.SHIPPED

	private fun customerSummary(customerId: UUID): CustomerSummary? =
		customers.findById(customerId)?.let {
			CustomerSummary(
				id = it.id,
				displayName = it.displayName,
				status = it.status,
				contactPerson = it.contactPerson,
				phone = it.phone,
				email = it.email,
			)
		}
}

private fun OrderStatusChange.toView(): OrderStatusChangeView =
	OrderStatusChangeView(
		fromStatus = fromStatus,
		toStatus = toStatus,
		actorDisplayName = actorUserId.toString(),
		changedAt = changedAt,
		note = note,
	)

private fun OrderChangeDiff.toView(): OrderChangeDiffView =
	OrderChangeDiffView(
		type = changeType,
		actorDisplayName = actorUserId.toString(),
		changedAt = changedAt,
		fieldDiffs = fieldDiffs,
	)
