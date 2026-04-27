package com.ctfind.productioncontrol.orders.application

import com.ctfind.productioncontrol.orders.domain.Customer
import com.ctfind.productioncontrol.orders.domain.CustomerOrder
import com.ctfind.productioncontrol.orders.domain.OrderAuditEvent
import com.ctfind.productioncontrol.orders.domain.OrderChangeDiff
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import com.ctfind.productioncontrol.orders.domain.OrderStatusChange
import java.time.LocalDate
import java.util.UUID

interface CustomerPort {
	fun findById(id: UUID): Customer?
	fun search(query: CustomerSearchQuery): List<Customer>
	fun save(customer: Customer): Customer
}

interface OrderNumberPort {
	fun nextOrderNumber(): String
}

interface CustomerOrderPort {
	fun findById(id: UUID): CustomerOrder?
	fun save(order: CustomerOrder): CustomerOrder
	fun search(query: OrderListQuery, today: LocalDate = LocalDate.now()): PageResult<CustomerOrder>
	fun count(query: OrderListQuery, today: LocalDate = LocalDate.now()): Long
	fun countByStatus(status: OrderStatus): Long
	fun countAll(): Long
	fun countActive(): Long
	fun countOverdue(today: LocalDate): Long
	fun createdTrend(days: Int, today: LocalDate): List<OrderTrendPoint>
	fun shippedTrend(days: Int, today: LocalDate): List<OrderTrendPoint>
}

interface OrderTracePort {
	fun saveStatusChange(change: OrderStatusChange): OrderStatusChange
	fun saveChangeDiff(diff: OrderChangeDiff): OrderChangeDiff
	fun findStatusChanges(orderId: UUID): List<OrderStatusChange>
	fun findChangeDiffs(orderId: UUID): List<OrderChangeDiff>
	fun recentStatusChanges(limit: Int): List<OrderStatusChange>
}

interface OrderAuditPort {
	fun record(event: OrderAuditEvent): OrderAuditEvent
}
