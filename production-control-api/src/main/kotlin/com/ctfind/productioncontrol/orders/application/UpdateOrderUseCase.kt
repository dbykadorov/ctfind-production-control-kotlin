package com.ctfind.productioncontrol.orders.application

import com.ctfind.productioncontrol.orders.domain.Customer
import com.ctfind.productioncontrol.orders.domain.CustomerOrder
import com.ctfind.productioncontrol.orders.domain.CustomerOrderItem
import com.ctfind.productioncontrol.orders.domain.CustomerStatus
import com.ctfind.productioncontrol.orders.domain.OrderAuditEvent
import com.ctfind.productioncontrol.orders.domain.OrderChangeDiff
import com.ctfind.productioncontrol.orders.domain.OrderChangeType
import com.ctfind.productioncontrol.orders.domain.OrderEditPolicy
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class UpdateOrderUseCase private constructor(
	private val customers: CustomerPort? = null,
	private val orders: CustomerOrderPort? = null,
	private val traces: OrderTracePort? = null,
	private val audit: OrderAuditPort? = null,
	private val diffService: OrderDiffService? = null,
	private val handler: ((UpdateOrderCommand) -> OrderMutationResult<OrderDetailView>)? = null,
) {
	@Autowired
	constructor(
		customers: CustomerPort,
		orders: CustomerOrderPort,
		traces: OrderTracePort,
		audit: OrderAuditPort,
	) : this(customers, orders, traces, audit, OrderDiffService(), null)

	constructor(
		customers: CustomerPort,
		orders: CustomerOrderPort,
		traces: OrderTracePort,
		audit: OrderAuditPort,
		diffService: OrderDiffService,
	) : this(customers, orders, traces, audit, diffService, null)

	constructor(handler: (UpdateOrderCommand) -> OrderMutationResult<OrderDetailView>) : this(null, null, null, null, null, handler)

	fun update(command: UpdateOrderCommand): OrderMutationResult<OrderDetailView> {
		handler?.let { return it(command) }
		val customers = requireNotNull(customers)
		val orders = requireNotNull(orders)
		val traces = requireNotNull(traces)
		val audit = requireNotNull(audit)
		val diffService = requireNotNull(diffService)

		if (!command.actor.canWriteOrders)
			return OrderMutationResult.Forbidden
		if (command.items.isEmpty())
			return OrderMutationResult.ValidationFailed("Order must contain at least one item", "items")
		val existing = orders.findById(command.orderId) ?: return OrderMutationResult.NotFound
		if (existing.version != command.expectedVersion)
			return OrderMutationResult.StaleVersion
		if (!OrderEditPolicy.canRegularEdit(existing))
			return OrderMutationResult.ValidationFailed("Shipped orders are read-only", "status")
		val customer = customers.findById(command.customerId)
			?: return OrderMutationResult.ValidationFailed("Customer not found", "customerId")
		if (customer.status != CustomerStatus.ACTIVE)
			return OrderMutationResult.ValidationFailed("Customer must be active", "customerId")

		val now = Instant.now()
		val updated = existing.copy(
			customerId = command.customerId,
			deliveryDate = command.deliveryDate,
			notes = command.notes,
			items = command.items.mapIndexed { index, item ->
				CustomerOrderItem(
					id = UUID.randomUUID(),
					lineNo = index + 1,
					itemName = item.itemName,
					quantity = item.quantity,
					uom = item.uom,
				)
			},
			updatedAt = now,
			version = existing.version + 1,
		)
		val diffs = diffService.diff(existing, updated)
		val saved = orders.save(updated)
		if (diffs.isNotEmpty()) {
			traces.saveChangeDiff(
				OrderChangeDiff(
					id = UUID.randomUUID(),
					orderId = saved.id,
					actorUserId = command.actor.userId,
					changedAt = now,
					changeType = OrderChangeType.UPDATED,
					fieldDiffs = diffs,
				),
			)
		}
		audit.record(
			OrderAuditEvent(
				id = UUID.randomUUID(),
				eventType = "ORDER_UPDATED",
				actorUserId = command.actor.userId,
				targetType = "CUSTOMER_ORDER",
				targetId = saved.id,
				eventAt = now,
				summary = "Updated order ${saved.orderNumber}",
			),
		)

		return OrderMutationResult.Success(saved.toDetailView(customer, traces.findChangeDiffs(saved.id), today = LocalDate.now()))
	}
}

private fun CustomerOrder.toDetailView(
	customer: Customer,
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
		history = emptyList(),
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
