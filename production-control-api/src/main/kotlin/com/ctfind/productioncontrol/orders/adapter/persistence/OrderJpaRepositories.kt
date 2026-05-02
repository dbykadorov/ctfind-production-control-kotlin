package com.ctfind.productioncontrol.orders.adapter.persistence

import com.ctfind.productioncontrol.orders.domain.CustomerStatus
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface CustomerJpaRepository : JpaRepository<CustomerEntity, UUID> {
	fun findByStatus(status: CustomerStatus): List<CustomerEntity>
}

interface CustomerOrderJpaRepository : JpaRepository<CustomerOrderEntity, UUID> {
	fun findByOrderNumber(orderNumber: String): CustomerOrderEntity?
	fun countByStatus(status: OrderStatus): Long
	fun countByStatusNot(status: OrderStatus): Long
	fun countByDeliveryDateBeforeAndStatusNot(date: LocalDate, status: OrderStatus): Long
}

interface OrderStatusChangeJpaRepository : JpaRepository<OrderStatusChangeEntity, UUID> {
	fun findByOrderIdOrderByChangedAtAsc(orderId: UUID): List<OrderStatusChangeEntity>
	fun findTop10ByOrderByChangedAtDesc(): List<OrderStatusChangeEntity>
}

interface OrderChangeDiffJpaRepository : JpaRepository<OrderChangeDiffEntity, UUID> {
	fun findByOrderIdOrderByChangedAtAsc(orderId: UUID): List<OrderChangeDiffEntity>
}

interface OrderAuditEventJpaRepository : JpaRepository<OrderAuditEventEntity, UUID>
