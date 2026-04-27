package com.ctfind.productioncontrol.production.adapter.persistence

import com.ctfind.productioncontrol.auth.adapter.persistence.UserAccountJpaRepository
import com.ctfind.productioncontrol.orders.adapter.persistence.CustomerOrderJpaRepository
import com.ctfind.productioncontrol.production.application.ProductionExecutorPort
import com.ctfind.productioncontrol.production.application.ProductionOrderSourcePort
import com.ctfind.productioncontrol.production.application.ProductionTaskAuditPort
import com.ctfind.productioncontrol.production.application.ProductionTaskExecutorSummary
import com.ctfind.productioncontrol.production.application.ProductionTaskListQuery
import com.ctfind.productioncontrol.production.application.ProductionTaskNumberPort
import com.ctfind.productioncontrol.production.application.ProductionTaskOrderItemSummary
import com.ctfind.productioncontrol.production.application.ProductionTaskOrderSummary
import com.ctfind.productioncontrol.production.application.ProductionTaskPageResult
import com.ctfind.productioncontrol.production.application.ProductionTaskPort
import com.ctfind.productioncontrol.production.application.ProductionTaskTracePort
import com.ctfind.productioncontrol.production.application.PRODUCTION_EXECUTOR_ROLE_CODE
import com.ctfind.productioncontrol.production.application.canViewAllProductionTasks
import com.ctfind.productioncontrol.production.domain.ProductionTask
import com.ctfind.productioncontrol.production.domain.ProductionTaskAuditEvent
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEvent
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JpaProductionTaskAdapter(
	private val taskRepository: ProductionTaskJpaRepository,
) : ProductionTaskPort {
	override fun findById(id: UUID): ProductionTask? =
		taskRepository.findById(id).orElse(null)?.toDomain()

	override fun save(task: ProductionTask): ProductionTask =
		taskRepository.save(task.toEntity()).toDomain()

	override fun search(
		query: ProductionTaskListQuery,
		currentUserId: UUID?,
		roleCodes: Set<String>,
	): ProductionTaskPageResult<ProductionTask> {
		val filtered = filter(taskRepository.findAll(), query, currentUserId, roleCodes)
			.sortedWith(compareByDescending<ProductionTaskEntity> { it.updatedAt }.thenBy { it.taskNumber })
		val page = query.page.coerceAtLeast(0)
		val size = query.size.coerceIn(1, 100)
		val from = (page * size).coerceAtMost(filtered.size)
		val to = (from + size).coerceAtMost(filtered.size)
		return ProductionTaskPageResult(
			items = filtered.subList(from, to).map { it.toDomain() },
			page = page,
			size = size,
			totalItems = filtered.size.toLong(),
		)
	}

	override fun existsByOrderItemIdAndPurpose(orderItemId: UUID, purpose: String): Boolean =
		taskRepository.existsByOrderItemIdAndPurposeIgnoreCase(orderItemId, purpose.trim())

	private fun filter(
		rows: List<ProductionTaskEntity>,
		query: ProductionTaskListQuery,
		currentUserId: UUID?,
		roleCodes: Set<String>,
	): List<ProductionTaskEntity> {
		val search = query.search?.trim()?.lowercase().orEmpty()
		return rows.filter { row ->
			(canViewAllProductionTasks(roleCodes) || (currentUserId != null && row.executorUserId == currentUserId)) &&
				(search.isBlank() || row.taskNumber.lowercase().contains(search) || row.purpose.lowercase().contains(search) || row.itemName.lowercase().contains(search)) &&
				(query.status == null || row.status == query.status) &&
				(query.orderId == null || row.orderId == query.orderId) &&
				(query.orderItemId == null || row.orderItemId == query.orderItemId) &&
				(query.executorUserId == null || row.executorUserId == query.executorUserId) &&
				(!query.assignedToMe || (currentUserId != null && row.executorUserId == currentUserId)) &&
				(!query.blockedOnly || row.status == ProductionTaskStatus.BLOCKED) &&
				(!query.activeOnly || row.status != ProductionTaskStatus.COMPLETED) &&
				(query.dueDateFrom == null || row.plannedFinishDate == null || !row.plannedFinishDate!!.isBefore(query.dueDateFrom)) &&
				(query.dueDateTo == null || row.plannedFinishDate == null || !row.plannedFinishDate!!.isAfter(query.dueDateTo))
		}
	}
}

@Component
class JpaProductionTaskTraceAdapter(
	private val historyRepository: ProductionTaskHistoryEventJpaRepository,
) : ProductionTaskTracePort {
	override fun saveHistoryEvent(event: ProductionTaskHistoryEvent): ProductionTaskHistoryEvent =
		historyRepository.save(event.toEntity()).toDomain()

	override fun findHistoryEvents(taskId: UUID): List<ProductionTaskHistoryEvent> =
		historyRepository.findByTaskIdOrderByEventAtAsc(taskId).map { it.toDomain() }
}

@Component
class JpaProductionTaskAuditAdapter(
	private val auditRepository: ProductionTaskAuditEventJpaRepository,
) : ProductionTaskAuditPort {
	override fun record(event: ProductionTaskAuditEvent): ProductionTaskAuditEvent =
		auditRepository.save(event.toEntity()).toDomain()
}

@Component
class JpaProductionTaskNumberAdapter(
	private val entityManager: EntityManager,
) : ProductionTaskNumberPort {
	override fun nextTaskNumber(): String {
		val next = entityManager.createNativeQuery("select nextval('production_task_number_seq')")
			.singleResult
			.toString()
			.toLong()
		return "PT-%06d".format(next)
	}
}

@Component
class JpaProductionOrderSourceAdapter(
	private val orderRepository: CustomerOrderJpaRepository,
) : ProductionOrderSourcePort {
	override fun findOrderSource(orderId: UUID): ProductionTaskOrderSummary? {
		val order = orderRepository.findById(orderId).orElse(null) ?: return null
		return ProductionTaskOrderSummary(
			id = order.id,
			orderNumber = order.orderNumber,
			customerDisplayName = order.customer.displayName,
			status = order.status,
			deliveryDate = order.deliveryDate,
		)
	}

	override fun findOrderItemSource(orderId: UUID, orderItemId: UUID): ProductionTaskOrderItemSummary? {
		val order = orderRepository.findById(orderId).orElse(null) ?: return null
		return order.items.firstOrNull { it.id == orderItemId }?.let {
			ProductionTaskOrderItemSummary(
				id = it.id,
				lineNo = it.lineNo,
				itemName = it.itemName,
				quantity = it.quantity,
				uom = it.uom,
			)
		}
	}
}

@Component
class JpaProductionExecutorAdapter(
	private val userRepository: UserAccountJpaRepository,
) : ProductionExecutorPort {
	override fun findExecutor(id: UUID): ProductionTaskExecutorSummary? =
		userRepository.findById(id).orElse(null)
			?.takeIf { user -> user.enabled && user.roles.any { it.code == PRODUCTION_EXECUTOR_ROLE_CODE } }
			?.toExecutorSummary()

	override fun searchExecutors(search: String?, limit: Int): List<ProductionTaskExecutorSummary> {
		val query = search?.trim()?.lowercase().orEmpty()
		return userRepository.findAll()
			.asSequence()
			.filter { it.enabled && it.roles.any { role -> role.code == PRODUCTION_EXECUTOR_ROLE_CODE } }
			.filter { query.isBlank() || it.displayName.lowercase().contains(query) || it.login.lowercase().contains(query) }
			.sortedBy { it.displayName.lowercase() }
			.take(limit.coerceIn(1, 50))
			.map { it.toExecutorSummary() }
			.toList()
	}
}

private fun ProductionTaskEntity.toDomain(): ProductionTask =
	ProductionTask(
		id = id,
		taskNumber = taskNumber,
		orderId = orderId,
		orderItemId = orderItemId,
		purpose = purpose,
		itemName = itemName,
		quantity = quantity,
		uom = uom,
		status = status,
		previousActiveStatus = previousActiveStatus,
		executorUserId = executorUserId,
		plannedStartDate = plannedStartDate,
		plannedFinishDate = plannedFinishDate,
		blockedReason = blockedReason,
		createdByUserId = createdByUserId,
		createdAt = createdAt,
		updatedAt = updatedAt,
		version = version,
	)

private fun ProductionTask.toEntity(): ProductionTaskEntity =
	ProductionTaskEntity(
		id = id,
		taskNumber = taskNumber,
		orderId = orderId,
		orderItemId = orderItemId,
		purpose = purpose,
		itemName = itemName,
		quantity = quantity,
		uom = uom,
		status = status,
		previousActiveStatus = previousActiveStatus,
		executorUserId = executorUserId,
		plannedStartDate = plannedStartDate,
		plannedFinishDate = plannedFinishDate,
		blockedReason = blockedReason,
		createdByUserId = createdByUserId,
		createdAt = createdAt,
		updatedAt = updatedAt,
		version = version,
	)

private fun ProductionTaskHistoryEvent.toEntity(): ProductionTaskHistoryEventEntity =
	ProductionTaskHistoryEventEntity(
		id = id,
		taskId = taskId,
		eventType = eventType,
		actorUserId = actorUserId,
		eventAt = eventAt,
		fromStatus = fromStatus,
		toStatus = toStatus,
		previousExecutorUserId = previousExecutorUserId,
		newExecutorUserId = newExecutorUserId,
		plannedStartDateBefore = plannedStartDateBefore,
		plannedStartDateAfter = plannedStartDateAfter,
		plannedFinishDateBefore = plannedFinishDateBefore,
		plannedFinishDateAfter = plannedFinishDateAfter,
		reason = reason,
		note = note,
	)

private fun ProductionTaskHistoryEventEntity.toDomain(): ProductionTaskHistoryEvent =
	ProductionTaskHistoryEvent(
		id = id,
		taskId = taskId,
		eventType = eventType,
		actorUserId = actorUserId,
		eventAt = eventAt,
		fromStatus = fromStatus,
		toStatus = toStatus,
		previousExecutorUserId = previousExecutorUserId,
		newExecutorUserId = newExecutorUserId,
		plannedStartDateBefore = plannedStartDateBefore,
		plannedStartDateAfter = plannedStartDateAfter,
		plannedFinishDateBefore = plannedFinishDateBefore,
		plannedFinishDateAfter = plannedFinishDateAfter,
		reason = reason,
		note = note,
	)

private fun ProductionTaskAuditEvent.toEntity(): ProductionTaskAuditEventEntity =
	ProductionTaskAuditEventEntity(
		id = id,
		eventType = eventType,
		actorUserId = actorUserId,
		targetType = targetType,
		targetId = targetId,
		eventAt = eventAt,
		summary = summary,
		metadata = metadata,
	)

private fun ProductionTaskAuditEventEntity.toDomain(): ProductionTaskAuditEvent =
	ProductionTaskAuditEvent(
		id = id,
		eventType = eventType,
		actorUserId = actorUserId,
		targetType = targetType,
		targetId = targetId,
		eventAt = eventAt,
		summary = summary,
		metadata = metadata,
	)

private fun com.ctfind.productioncontrol.auth.adapter.persistence.UserAccountEntity.toExecutorSummary(): ProductionTaskExecutorSummary =
	ProductionTaskExecutorSummary(
		id = id,
		displayName = displayName,
		login = login,
	)
