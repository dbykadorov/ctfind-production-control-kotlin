package com.ctfind.productioncontrol.orders.domain

import java.time.Instant
import java.util.UUID

enum class OrderChangeType {
	CREATED,
	UPDATED,
	STATUS_CHANGED,
}

data class OrderStatusChange(
	val id: UUID,
	val orderId: UUID,
	val fromStatus: OrderStatus?,
	val toStatus: OrderStatus,
	val actorUserId: UUID,
	val changedAt: Instant,
	val note: String? = null,
)

data class OrderFieldDiff(
	val fieldName: String,
	val fieldLabel: String? = null,
	val fromValue: String? = null,
	val toValue: String? = null,
)

data class OrderChangeDiff(
	val id: UUID,
	val orderId: UUID,
	val actorUserId: UUID,
	val changedAt: Instant,
	val changeType: OrderChangeType,
	val fieldDiffs: List<OrderFieldDiff>,
	val beforeSnapshot: String? = null,
	val afterSnapshot: String? = null,
)

data class OrderAuditEvent(
	val id: UUID,
	val eventType: String,
	val actorUserId: UUID,
	val targetType: String,
	val targetId: UUID,
	val eventAt: Instant,
	val summary: String,
	val metadata: String? = null,
) {
	init {
		require(eventType.isNotBlank()) { "audit event type must not be blank" }
		require(targetType.isNotBlank()) { "audit target type must not be blank" }
		require(summary.isNotBlank()) { "audit summary must not be blank" }
	}
}
