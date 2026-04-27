package com.ctfind.productioncontrol.orders.adapter.persistence

import com.ctfind.productioncontrol.orders.domain.CustomerStatus
import com.ctfind.productioncontrol.orders.domain.OrderChangeType
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "customer")
class CustomerEntity(
	@Id
	var id: UUID = UUID.randomUUID(),

	@Column(name = "display_name", nullable = false)
	var displayName: String = "",

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	var status: CustomerStatus = CustomerStatus.ACTIVE,

	@Column(name = "contact_person")
	var contactPerson: String? = null,

	@Column
	var phone: String? = null,

	@Column
	var email: String? = null,

	@Column(name = "created_at", nullable = false)
	var createdAt: Instant = Instant.EPOCH,

	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant = Instant.EPOCH,
)

@Entity
@Table(name = "customer_order")
class CustomerOrderEntity(
	@Id
	var id: UUID = UUID.randomUUID(),

	@Column(name = "order_number", nullable = false, unique = true)
	var orderNumber: String = "",

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id", nullable = false)
	var customer: CustomerEntity = CustomerEntity(),

	@Column(name = "delivery_date", nullable = false)
	var deliveryDate: LocalDate = LocalDate.EPOCH,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	var status: OrderStatus = OrderStatus.NEW,

	@Column
	var notes: String? = null,

	@Column(name = "created_by_user_id", nullable = false)
	var createdByUserId: UUID = UUID.randomUUID(),

	@Column(name = "created_at", nullable = false)
	var createdAt: Instant = Instant.EPOCH,

	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant = Instant.EPOCH,

	@Version
	@Column(nullable = false)
	var version: Long = 0,

	@OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
	@OrderBy("lineNo ASC")
	var items: MutableList<CustomerOrderItemEntity> = mutableListOf(),
)

@Entity
@Table(name = "customer_order_item")
class CustomerOrderItemEntity(
	@Id
	var id: UUID = UUID.randomUUID(),

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	var order: CustomerOrderEntity? = null,

	@Column(name = "line_no", nullable = false)
	var lineNo: Int = 1,

	@Column(name = "item_name", nullable = false)
	var itemName: String = "",

	@Column(nullable = false)
	var quantity: BigDecimal = BigDecimal.ONE,

	@Column(nullable = false)
	var uom: String = "",

	@Column(name = "created_at", nullable = false)
	var createdAt: Instant = Instant.EPOCH,

	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant = Instant.EPOCH,
)

@Entity
@Table(name = "order_status_change")
class OrderStatusChangeEntity(
	@Id
	var id: UUID = UUID.randomUUID(),

	@Column(name = "order_id", nullable = false)
	var orderId: UUID = UUID.randomUUID(),

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status")
	var fromStatus: OrderStatus? = null,

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false)
	var toStatus: OrderStatus = OrderStatus.NEW,

	@Column(name = "actor_user_id", nullable = false)
	var actorUserId: UUID = UUID.randomUUID(),

	@Column(name = "changed_at", nullable = false)
	var changedAt: Instant = Instant.EPOCH,

	@Column
	var note: String? = null,
)

@Entity
@Table(name = "order_change_diff")
class OrderChangeDiffEntity(
	@Id
	var id: UUID = UUID.randomUUID(),

	@Column(name = "order_id", nullable = false)
	var orderId: UUID = UUID.randomUUID(),

	@Column(name = "actor_user_id", nullable = false)
	var actorUserId: UUID = UUID.randomUUID(),

	@Column(name = "changed_at", nullable = false)
	var changedAt: Instant = Instant.EPOCH,

	@Enumerated(EnumType.STRING)
	@Column(name = "change_type", nullable = false)
	var changeType: OrderChangeType = OrderChangeType.UPDATED,

	@Column(name = "field_diffs", nullable = false)
	var fieldDiffs: String = "[]",

	@Column(name = "before_snapshot")
	var beforeSnapshot: String? = null,

	@Column(name = "after_snapshot")
	var afterSnapshot: String? = null,
)

@Entity
@Table(name = "order_audit_event")
class OrderAuditEventEntity(
	@Id
	var id: UUID = UUID.randomUUID(),

	@Column(name = "event_type", nullable = false)
	var eventType: String = "",

	@Column(name = "actor_user_id", nullable = false)
	var actorUserId: UUID = UUID.randomUUID(),

	@Column(name = "target_type", nullable = false)
	var targetType: String = "",

	@Column(name = "target_id", nullable = false)
	var targetId: UUID = UUID.randomUUID(),

	@Column(name = "event_at", nullable = false)
	var eventAt: Instant = Instant.EPOCH,

	@Column(nullable = false)
	var summary: String = "",

	@Column
	var metadata: String? = null,
)
