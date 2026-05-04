package com.ctfind.productioncontrol.orders.adapter.persistence

import com.ctfind.productioncontrol.orders.application.CustomerOrderPort
import com.ctfind.productioncontrol.orders.application.CustomerPort
import com.ctfind.productioncontrol.orders.application.CustomerSearchQuery
import com.ctfind.productioncontrol.orders.application.OrderAuditPort
import com.ctfind.productioncontrol.orders.application.OrderListQuery
import com.ctfind.productioncontrol.orders.application.OrderNumberPort
import com.ctfind.productioncontrol.orders.application.OrderTracePort
import com.ctfind.productioncontrol.orders.application.OrderTrendPoint
import com.ctfind.productioncontrol.orders.application.PageResult
import com.ctfind.productioncontrol.orders.domain.Customer
import com.ctfind.productioncontrol.orders.domain.CustomerOrder
import com.ctfind.productioncontrol.orders.domain.CustomerOrderItem
import com.ctfind.productioncontrol.orders.domain.CustomerStatus
import com.ctfind.productioncontrol.orders.domain.OrderAuditEvent
import com.ctfind.productioncontrol.orders.domain.OrderChangeDiff
import com.ctfind.productioncontrol.orders.domain.OrderChangeType
import com.ctfind.productioncontrol.orders.domain.OrderFieldDiff
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import com.ctfind.productioncontrol.orders.domain.OrderStatusChange
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Component
class JpaCustomerAdapter(
	private val customerRepository: CustomerJpaRepository,
) : CustomerPort {
	override fun findById(id: UUID): Customer? =
		customerRepository.findById(id).orElse(null)?.toDomain()

	override fun search(query: CustomerSearchQuery): List<Customer> {
		val rows = if (query.activeOnly) {
			customerRepository.findByStatus(CustomerStatus.ACTIVE)
		}
		else {
			customerRepository.findAll()
		}
		val search = query.search?.trim()?.lowercase().orEmpty()
		return rows
			.asSequence()
			.filter { search.isBlank() || it.displayName.lowercase().contains(search) }
			.sortedBy { it.displayName.lowercase() }
			.take(query.limit.coerceIn(1, 50))
			.map { it.toDomain() }
			.toList()
	}

	override fun save(customer: Customer): Customer =
		customerRepository.save(customer.toEntity()).toDomain()
}

@Component
class JpaOrderNumberAdapter(
	private val entityManager: EntityManager,
) : OrderNumberPort {
	override fun nextOrderNumber(): String {
		val next = entityManager
			.createNativeQuery("select nextval('customer_order_number_seq')")
			.singleResult
			.toString()
			.toLong()
		return "ORD-%06d".format(next)
	}
}

@Component
class JpaCustomerOrderAdapter(
	private val orderRepository: CustomerOrderJpaRepository,
	private val customerRepository: CustomerJpaRepository,
	private val entityManager: EntityManager,
) : CustomerOrderPort {
	override fun findById(id: UUID): CustomerOrder? =
		orderRepository.findById(id).orElse(null)?.toDomain()

	@Transactional
	override fun save(order: CustomerOrder): CustomerOrder {
		val customer = customerRepository.findById(order.customerId)
			.orElseThrow { IllegalArgumentException("customer not found") }
		val entity = orderRepository.findById(order.id).orElse(CustomerOrderEntity(id = order.id))
		entity.orderNumber = order.orderNumber
		entity.customer = customer
		entity.deliveryDate = order.deliveryDate
		entity.status = order.status
		entity.notes = order.notes
		entity.createdByUserId = order.createdByUserId
		entity.createdAt = order.createdAt
		entity.updatedAt = order.updatedAt
		entity.version = order.version
		if (entity.items.isNotEmpty()) {
			entity.items.clear()
			entityManager.flush()
		}
		order.items.forEach { item ->
			entity.items.add(item.toEntity(entity, order.createdAt, order.updatedAt))
		}
		return orderRepository.save(entity).toDomain()
	}

	override fun search(query: OrderListQuery, today: LocalDate): PageResult<CustomerOrder> {
		val filtered = filter(orderRepository.findAll(), query, today)
			.sortedWith(compareByDescending<CustomerOrderEntity> { it.updatedAt }.thenBy { it.orderNumber })
		val from = (query.page.coerceAtLeast(0) * query.size.coerceAtLeast(1)).coerceAtMost(filtered.size)
		val to = (from + query.size.coerceAtLeast(1)).coerceAtMost(filtered.size)
		return PageResult(
			items = filtered.subList(from, to).map { it.toDomain() },
			page = query.page.coerceAtLeast(0),
			size = query.size.coerceAtLeast(1),
			totalItems = filtered.size.toLong(),
		)
	}

	override fun count(query: OrderListQuery, today: LocalDate): Long =
		filter(orderRepository.findAll(), query, today).size.toLong()

	override fun countByStatus(status: OrderStatus): Long =
		orderRepository.countByStatus(status)

	override fun countAll(): Long =
		orderRepository.count()

	override fun countActive(): Long =
		orderRepository.countByStatusNot(OrderStatus.SHIPPED)

	override fun countOverdue(today: LocalDate): Long =
		orderRepository.countByDeliveryDateBeforeAndStatusNot(today, OrderStatus.SHIPPED)

	override fun createdTrend(days: Int, today: LocalDate): List<OrderTrendPoint> {
		val from = today.minusDays(days.coerceAtLeast(1).toLong() - 1)
		return orderRepository.findAll()
			.filter { !it.createdAt.atZone(ZoneOffset.UTC).toLocalDate().isBefore(from) }
			.groupingBy { it.createdAt.atZone(ZoneOffset.UTC).toLocalDate() }
			.eachCount()
			.map { (date, count) -> OrderTrendPoint(date = date, created = count.toLong(), shipped = 0) }
	}

	override fun shippedTrend(days: Int, today: LocalDate): List<OrderTrendPoint> {
		val from = today.minusDays(days.coerceAtLeast(1).toLong() - 1)
		return orderRepository.findAll()
			.filter { it.status == OrderStatus.SHIPPED }
			.filter { !it.updatedAt.atZone(ZoneOffset.UTC).toLocalDate().isBefore(from) }
			.groupingBy { it.updatedAt.atZone(ZoneOffset.UTC).toLocalDate() }
			.eachCount()
			.map { (date, count) -> OrderTrendPoint(date = date, created = 0, shipped = count.toLong()) }
	}

	private fun filter(rows: List<CustomerOrderEntity>, query: OrderListQuery, today: LocalDate): List<CustomerOrderEntity> {
		val search = query.search?.trim()?.lowercase().orEmpty()
		return rows.filter { row ->
			(search.isBlank() || row.orderNumber.lowercase().contains(search) || row.customer.displayName.lowercase().contains(search)) &&
				(query.status == null || row.status == query.status) &&
				(query.customerId == null || row.customer.id == query.customerId) &&
				(!query.activeOnly || row.status != OrderStatus.SHIPPED) &&
				(!query.overdueOnly || (row.deliveryDate.isBefore(today) && row.status != OrderStatus.SHIPPED)) &&
				(query.deliveryDateFrom == null || !row.deliveryDate.isBefore(query.deliveryDateFrom)) &&
				(query.deliveryDateTo == null || !row.deliveryDate.isAfter(query.deliveryDateTo))
		}
	}
}

@Component
class JpaOrderTraceAdapter(
	private val statusChangeRepository: OrderStatusChangeJpaRepository,
	private val changeDiffRepository: OrderChangeDiffJpaRepository,
) : OrderTracePort {
	override fun saveStatusChange(change: OrderStatusChange): OrderStatusChange =
		statusChangeRepository.save(change.toEntity()).toDomain()

	override fun saveChangeDiff(diff: OrderChangeDiff): OrderChangeDiff =
		changeDiffRepository.save(diff.toEntity()).toDomain()

	override fun findStatusChanges(orderId: UUID): List<OrderStatusChange> =
		statusChangeRepository.findByOrderIdOrderByChangedAtAsc(orderId).map { it.toDomain() }

	override fun findChangeDiffs(orderId: UUID): List<OrderChangeDiff> =
		changeDiffRepository.findByOrderIdOrderByChangedAtAsc(orderId).map { it.toDomain() }

	override fun recentStatusChanges(limit: Int): List<OrderStatusChange> =
		statusChangeRepository.findTop10ByOrderByChangedAtDesc().take(limit).map { it.toDomain() }
}

@Component
class JpaOrderAuditAdapter(
	private val auditRepository: OrderAuditEventJpaRepository,
) : OrderAuditPort {
	override fun record(event: OrderAuditEvent): OrderAuditEvent =
		auditRepository.save(event.toEntity()).toDomain()
}

private fun CustomerEntity.toDomain(): Customer =
	Customer(
		id = id,
		displayName = displayName,
		status = status,
		contactPerson = contactPerson,
		phone = phone,
		email = email,
		createdAt = createdAt,
		updatedAt = updatedAt,
	)

private fun Customer.toEntity(): CustomerEntity =
	CustomerEntity(
		id = id,
		displayName = displayName,
		status = status,
		contactPerson = contactPerson,
		phone = phone,
		email = email,
		createdAt = createdAt,
		updatedAt = updatedAt,
	)

private fun CustomerOrderEntity.toDomain(): CustomerOrder =
	CustomerOrder(
		id = id,
		orderNumber = orderNumber,
		customerId = customer.id,
		deliveryDate = deliveryDate,
		status = status,
		notes = notes,
		items = items.map { it.toDomain() },
		createdByUserId = createdByUserId,
		createdAt = createdAt,
		updatedAt = updatedAt,
		version = version,
	)

private fun CustomerOrderItemEntity.toDomain(): CustomerOrderItem =
	CustomerOrderItem(
		id = id,
		lineNo = lineNo,
		itemName = itemName,
		quantity = quantity,
		uom = uom,
	)

private fun CustomerOrderItem.toEntity(
	order: CustomerOrderEntity,
	createdAt: java.time.Instant,
	updatedAt: java.time.Instant,
): CustomerOrderItemEntity =
	CustomerOrderItemEntity(
		id = id,
		order = order,
		lineNo = lineNo,
		itemName = itemName,
		quantity = quantity,
		uom = uom,
		createdAt = createdAt,
		updatedAt = updatedAt,
	)

private fun OrderStatusChange.toEntity(): OrderStatusChangeEntity =
	OrderStatusChangeEntity(
		id = id,
		orderId = orderId,
		fromStatus = fromStatus,
		toStatus = toStatus,
		actorUserId = actorUserId,
		changedAt = changedAt,
		note = note,
	)

private fun OrderStatusChangeEntity.toDomain(): OrderStatusChange =
	OrderStatusChange(
		id = id,
		orderId = orderId,
		fromStatus = fromStatus,
		toStatus = toStatus,
		actorUserId = actorUserId,
		changedAt = changedAt,
		note = note,
	)

private fun OrderChangeDiff.toEntity(): OrderChangeDiffEntity =
	OrderChangeDiffEntity(
		id = id,
		orderId = orderId,
		actorUserId = actorUserId,
		changedAt = changedAt,
		changeType = changeType,
		fieldDiffs = serializeFieldDiffs(fieldDiffs),
		beforeSnapshot = beforeSnapshot,
		afterSnapshot = afterSnapshot,
	)

private fun OrderChangeDiffEntity.toDomain(): OrderChangeDiff =
	OrderChangeDiff(
		id = id,
		orderId = orderId,
		actorUserId = actorUserId,
		changedAt = changedAt,
		changeType = changeType,
		fieldDiffs = deserializeFieldDiffs(fieldDiffs),
		beforeSnapshot = beforeSnapshot,
		afterSnapshot = afterSnapshot,
	)

private fun OrderAuditEvent.toEntity(): OrderAuditEventEntity =
	OrderAuditEventEntity(
		id = id,
		eventType = eventType,
		actorUserId = actorUserId,
		targetType = targetType,
		targetId = targetId,
		eventAt = eventAt,
		summary = summary,
		metadata = metadata,
	)

private fun OrderAuditEventEntity.toDomain(): OrderAuditEvent =
	OrderAuditEvent(
		id = id,
		eventType = eventType,
		actorUserId = actorUserId,
		targetType = targetType,
		targetId = targetId,
		eventAt = eventAt,
		summary = summary,
		metadata = metadata,
	)

private fun serializeFieldDiffs(diffs: List<OrderFieldDiff>): String =
	diffs.joinToString("\n") {
		listOf(it.fieldName, it.fieldLabel.orEmpty(), it.fromValue.orEmpty(), it.toValue.orEmpty())
			.joinToString("\t") { value -> value.replace("\t", " ").replace("\n", " ") }
	}

private fun deserializeFieldDiffs(raw: String): List<OrderFieldDiff> =
	raw.lineSequence()
		.filter { it.isNotBlank() }
		.map {
			val parts = it.split("\t")
			OrderFieldDiff(
				fieldName = parts.getOrElse(0) { "" },
				fieldLabel = parts.getOrNull(1)?.ifBlank { null },
				fromValue = parts.getOrNull(2)?.ifBlank { null },
				toValue = parts.getOrNull(3)?.ifBlank { null },
			)
		}
		.toList()
