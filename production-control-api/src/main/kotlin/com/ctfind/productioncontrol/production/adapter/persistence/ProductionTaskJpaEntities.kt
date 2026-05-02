package com.ctfind.productioncontrol.production.adapter.persistence

import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEventType
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "production_task")
class ProductionTaskEntity(
	@Id
	var id: UUID = UUID.randomUUID(),

	@Column(name = "task_number", nullable = false, unique = true)
	var taskNumber: String = "",

	@Column(name = "order_id", nullable = false)
	var orderId: UUID = UUID.randomUUID(),

	@Column(name = "order_item_id")
	var orderItemId: UUID? = null,

	@Column(nullable = false)
	var purpose: String = "",

	@Column(name = "item_name", nullable = false)
	var itemName: String = "",

	@Column(nullable = false)
	var quantity: BigDecimal = BigDecimal.ONE,

	@Column(nullable = false)
	var uom: String = "",

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	var status: ProductionTaskStatus = ProductionTaskStatus.NOT_STARTED,

	@Enumerated(EnumType.STRING)
	@Column(name = "previous_active_status")
	var previousActiveStatus: ProductionTaskStatus? = null,

	@Column(name = "executor_user_id")
	var executorUserId: UUID? = null,

	@Column(name = "planned_start_date")
	var plannedStartDate: LocalDate? = null,

	@Column(name = "planned_finish_date")
	var plannedFinishDate: LocalDate? = null,

	@Column(name = "blocked_reason")
	var blockedReason: String? = null,

	@Column(name = "created_by_user_id", nullable = false)
	var createdByUserId: UUID = UUID.randomUUID(),

	@Column(name = "created_at", nullable = false)
	var createdAt: Instant = Instant.EPOCH,

	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant = Instant.EPOCH,

	@Version
	@Column(nullable = false)
	var version: Long = 0,
)

@Entity
@Table(name = "production_task_history_event")
class ProductionTaskHistoryEventEntity(
	@Id
	var id: UUID = UUID.randomUUID(),

	@Column(name = "task_id", nullable = false)
	var taskId: UUID = UUID.randomUUID(),

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false)
	var eventType: ProductionTaskHistoryEventType = ProductionTaskHistoryEventType.CREATED,

	@Column(name = "actor_user_id", nullable = false)
	var actorUserId: UUID = UUID.randomUUID(),

	@Column(name = "event_at", nullable = false)
	var eventAt: Instant = Instant.EPOCH,

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status")
	var fromStatus: ProductionTaskStatus? = null,

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status")
	var toStatus: ProductionTaskStatus? = null,

	@Column(name = "previous_executor_user_id")
	var previousExecutorUserId: UUID? = null,

	@Column(name = "new_executor_user_id")
	var newExecutorUserId: UUID? = null,

	@Column(name = "planned_start_date_before")
	var plannedStartDateBefore: LocalDate? = null,

	@Column(name = "planned_start_date_after")
	var plannedStartDateAfter: LocalDate? = null,

	@Column(name = "planned_finish_date_before")
	var plannedFinishDateBefore: LocalDate? = null,

	@Column(name = "planned_finish_date_after")
	var plannedFinishDateAfter: LocalDate? = null,

	@Column
	var reason: String? = null,

	@Column
	var note: String? = null,
)

@Entity
@Table(name = "production_task_audit_event")
class ProductionTaskAuditEventEntity(
	@Id
	var id: UUID = UUID.randomUUID(),

	@Column(name = "event_type", nullable = false)
	var eventType: String = "",

	@Column(name = "actor_user_id", nullable = false)
	var actorUserId: UUID = UUID.randomUUID(),

	@Column(name = "target_type", nullable = false)
	var targetType: String = "PRODUCTION_TASK",

	@Column(name = "target_id", nullable = false)
	var targetId: UUID = UUID.randomUUID(),

	@Column(name = "event_at", nullable = false)
	var eventAt: Instant = Instant.EPOCH,

	@Column(nullable = false)
	var summary: String = "",

	@Column
	var metadata: String? = null,
)
