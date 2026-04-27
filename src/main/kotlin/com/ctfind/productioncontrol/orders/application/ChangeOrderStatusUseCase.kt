package com.ctfind.productioncontrol.orders.application

import com.ctfind.productioncontrol.orders.domain.Customer
import com.ctfind.productioncontrol.orders.domain.CustomerOrder
import com.ctfind.productioncontrol.orders.domain.OrderAuditEvent
import com.ctfind.productioncontrol.orders.domain.OrderChangeDiff
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import com.ctfind.productioncontrol.orders.domain.OrderStatusChange
import com.ctfind.productioncontrol.orders.domain.OrderStatusPolicy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class ChangeOrderStatusUseCase private constructor(
	private val customers: CustomerPort? = null,
	private val orders: CustomerOrderPort? = null,
	private val traces: OrderTracePort? = null,
	private val audit: OrderAuditPort? = null,
	private val handler: ((ChangeOrderStatusCommand) -> OrderMutationResult<OrderDetailView>)? = null,
) {
	@Autowired
	constructor(
		customers: CustomerPort,
		orders: CustomerOrderPort,
		traces: OrderTracePort,
		audit: OrderAuditPort,
	) : this(customers, orders, traces, audit, null)

	constructor(handler: (ChangeOrderStatusCommand) -> OrderMutationResult<OrderDetailView>) : this(null, null, null, null, handler)

	fun changeStatus(command: ChangeOrderStatusCommand): OrderMutationResult<OrderDetailView> {
		handler?.let { return it(command) }
		val customers = requireNotNull(customers)
		val orders = requireNotNull(orders)
		val traces = requireNotNull(traces)
		val audit = requireNotNull(audit)

		if (!command.actor.canWriteOrders)
			return OrderMutationResult.Forbidden
		val existing = orders.findById(command.orderId) ?: return OrderMutationResult.NotFound
		if (existing.version != command.expectedVersion)
			return OrderMutationResult.StaleVersion
		if (!OrderStatusPolicy.isDirectForward(existing.status, command.toStatus))
			return OrderMutationResult.InvalidTransition
		val customer = customers.findById(existing.customerId) ?: return OrderMutationResult.NotFound

		val now = Instant.now()
		val saved = orders.save(
			existing.copy(
				status = command.toStatus,
				updatedAt = now,
				version = existing.version + 1,
			),
		)
		traces.saveStatusChange(
			OrderStatusChange(
				id = UUID.randomUUID(),
				orderId = saved.id,
				fromStatus = existing.status,
				toStatus = saved.status,
				actorUserId = command.actor.userId,
				changedAt = now,
				note = command.note,
			),
		)
		audit.record(
			OrderAuditEvent(
				id = UUID.randomUUID(),
				eventType = "ORDER_STATUS_CHANGED",
				actorUserId = command.actor.userId,
				targetType = "CUSTOMER_ORDER",
				targetId = saved.id,
				eventAt = now,
				summary = "Changed order ${saved.orderNumber} status from ${existing.status} to ${saved.status}",
			),
		)

		return OrderMutationResult.Success(
			saved.toDetailView(
				customer = customer,
				statusChanges = traces.findStatusChanges(saved.id),
				changeDiffs = traces.findChangeDiffs(saved.id),
				today = LocalDate.now(),
			),
		)
	}
}

private fun CustomerOrder.toDetailView(
	customer: Customer,
	statusChanges: List<OrderStatusChange>,
	changeDiffs: List<OrderChangeDiff>,
	today: LocalDate,
): OrderDetailView =
	OrderDetailView(
		id = id,
		orderNumber = orderNumber,
		customer = CustomerSummary(
			id = customer.id,
			displayName = customer.displayName,
			status = customer.status,
			contactPerson = customer.contactPerson,
			phone = customer.phone,
			email = customer.email,
		),
		deliveryDate = deliveryDate,
		status = status,
		notes = notes,
		items = items.map {
			OrderItemView(
				id = it.id,
				lineNo = it.lineNo,
				itemName = it.itemName,
				quantity = it.quantity,
				uom = it.uom,
			)
		},
		history = statusChanges.map {
			OrderStatusChangeView(
				fromStatus = it.fromStatus,
				toStatus = it.toStatus,
				actorDisplayName = it.actorUserId.toString(),
				changedAt = it.changedAt,
				note = it.note,
			)
		},
		changeDiffs = changeDiffs.map {
			OrderChangeDiffView(
				type = it.changeType,
				actorDisplayName = it.actorUserId.toString(),
				changedAt = it.changedAt,
				fieldDiffs = it.fieldDiffs,
			)
		},
		createdAt = createdAt,
		updatedAt = updatedAt,
		version = version,
		overdue = deliveryDate.isBefore(today) && status != OrderStatus.SHIPPED,
	)
