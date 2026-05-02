package com.ctfind.productioncontrol.orders.application

import com.ctfind.productioncontrol.orders.domain.CustomerOrder
import com.ctfind.productioncontrol.orders.domain.CustomerOrderItem
import com.ctfind.productioncontrol.orders.domain.CustomerStatus
import com.ctfind.productioncontrol.orders.domain.OrderAuditEvent
import com.ctfind.productioncontrol.orders.domain.OrderChangeDiff
import com.ctfind.productioncontrol.orders.domain.OrderChangeType
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import com.ctfind.productioncontrol.orders.domain.OrderStatusChange
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class CreateOrderUseCase private constructor(
	private val customers: CustomerPort? = null,
	private val orders: CustomerOrderPort? = null,
	private val orderNumbers: OrderNumberPort? = null,
	private val traces: OrderTracePort? = null,
	private val audit: OrderAuditPort? = null,
	private val handler: ((CreateOrderCommand) -> OrderMutationResult<OrderDetailView>)? = null,
) {
	@Autowired
	constructor(
		customers: CustomerPort,
		orders: CustomerOrderPort,
		orderNumbers: OrderNumberPort,
		traces: OrderTracePort,
		audit: OrderAuditPort,
	) : this(customers, orders, orderNumbers, traces, audit, null)

	constructor(handler: (CreateOrderCommand) -> OrderMutationResult<OrderDetailView>) : this(null, null, null, null, null, handler)

	fun create(command: CreateOrderCommand): OrderMutationResult<OrderDetailView> {
		handler?.let { return it(command) }
		val customers = requireNotNull(customers)
		val orders = requireNotNull(orders)
		val orderNumbers = requireNotNull(orderNumbers)
		val traces = requireNotNull(traces)
		val audit = requireNotNull(audit)

		if (!command.actor.canWriteOrders)
			return OrderMutationResult.Forbidden
		if (command.items.isEmpty())
			return OrderMutationResult.ValidationFailed("Order must contain at least one item", "items")
		val customer = customers.findById(command.customerId)
			?: return OrderMutationResult.ValidationFailed("Customer not found", "customerId")
		if (customer.status != CustomerStatus.ACTIVE)
			return OrderMutationResult.ValidationFailed("Customer must be active", "customerId")

		val now = Instant.now()
		val order = CustomerOrder(
			id = UUID.randomUUID(),
			orderNumber = orderNumbers.nextOrderNumber(),
			customerId = command.customerId,
			deliveryDate = command.deliveryDate,
			status = OrderStatus.NEW,
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
			createdByUserId = command.actor.userId,
			createdAt = now,
			updatedAt = now,
			version = 0,
		)
		val saved = orders.save(order)
		traces.saveStatusChange(
			OrderStatusChange(
				id = UUID.randomUUID(),
				orderId = saved.id,
				fromStatus = null,
				toStatus = saved.status,
				actorUserId = command.actor.userId,
				changedAt = now,
			),
		)
		traces.saveChangeDiff(
			OrderChangeDiff(
				id = UUID.randomUUID(),
				orderId = saved.id,
				actorUserId = command.actor.userId,
				changedAt = now,
				changeType = OrderChangeType.CREATED,
				fieldDiffs = emptyList(),
			),
		)
		audit.record(
			OrderAuditEvent(
				id = UUID.randomUUID(),
				eventType = "ORDER_CREATED",
				actorUserId = command.actor.userId,
				targetType = "CUSTOMER_ORDER",
				targetId = saved.id,
				eventAt = now,
				summary = "Created order ${saved.orderNumber}",
			),
		)

		return OrderMutationResult.Success(saved.toDetailView(customer, today = LocalDate.now()))
	}
}

private fun CustomerOrder.toDetailView(
	customer: com.ctfind.productioncontrol.orders.domain.Customer,
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
		changeDiffs = emptyList(),
		createdAt = createdAt,
		updatedAt = updatedAt,
		version = version,
		overdue = deliveryDate.isBefore(today) && status != OrderStatus.SHIPPED,
	)
