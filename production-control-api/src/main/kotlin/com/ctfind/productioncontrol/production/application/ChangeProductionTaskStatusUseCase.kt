package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.notifications.application.CreateNotificationCommand
import com.ctfind.productioncontrol.notifications.application.NotificationCreatePort
import com.ctfind.productioncontrol.notifications.domain.NotificationTargetType
import com.ctfind.productioncontrol.notifications.domain.NotificationType
import com.ctfind.productioncontrol.production.domain.InvalidProductionTaskStatusTransition
import com.ctfind.productioncontrol.production.domain.ProductionTask
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEvent
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEventType
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatusPolicy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ChangeProductionTaskStatusUseCase(
	private val tasks: ProductionTaskPort,
	private val traces: ProductionTaskTracePort,
	private val audit: ProductionTaskAuditService,
	private val notifications: NotificationCreatePort,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	@Transactional
	fun execute(cmd: ChangeProductionTaskStatusCommand): ProductionTaskMutationResult<Unit> {
		val task = tasks.findById(cmd.taskId) ?: return ProductionTaskMutationResult.NotFound
		if (!canUpdateAssignedProductionTaskStatus(cmd.roleCodes, cmd.actorUserId, task.executorUserId)) {
			return ProductionTaskMutationResult.Forbidden
		}
		if (task.status == ProductionTaskStatus.COMPLETED) {
			return ProductionTaskMutationResult.ValidationFailed("Completed tasks cannot change status.")
		}
		if (task.version != cmd.expectedVersion) {
			return ProductionTaskMutationResult.StaleVersion
		}
		val from = task.status
		val to = cmd.toStatus
		if (from == to) {
			return ProductionTaskMutationResult.ValidationFailed("Status is already $to.")
		}

		if (to == ProductionTaskStatus.BLOCKED) {
			if (cmd.reason.isNullOrBlank()) {
				return ProductionTaskMutationResult.ValidationFailed("Block reason is required.")
			}
		}

		val updated: ProductionTask
		if (from == ProductionTaskStatus.BLOCKED) {
			val expected = try {
				ProductionTaskStatusPolicy.unblockedStatus(task.previousActiveStatus)
			} catch (_: IllegalArgumentException) {
				return ProductionTaskMutationResult.ValidationFailed("Blocked task is missing a restorable status.")
			}
			if (to != expected) {
				return ProductionTaskMutationResult.InvalidTransition
			}
			updated = task.copy(
				status = to,
				previousActiveStatus = null,
				blockedReason = null,
				updatedAt = Instant.now(),
			)
		} else {
			try {
				ProductionTaskStatusPolicy.assertAllowed(from, to)
			} catch (_: InvalidProductionTaskStatusTransition) {
				return ProductionTaskMutationResult.InvalidTransition
			}
			updated = when (to) {
				ProductionTaskStatus.BLOCKED ->
					task.copy(
						status = ProductionTaskStatus.BLOCKED,
						previousActiveStatus = from,
						blockedReason = cmd.reason!!.trim(),
						updatedAt = Instant.now(),
					)
				ProductionTaskStatus.COMPLETED ->
					task.copy(
						status = ProductionTaskStatus.COMPLETED,
						updatedAt = Instant.now(),
					)
				else ->
					task.copy(
						status = to,
						updatedAt = Instant.now(),
					)
			}
		}

		val saved = tasks.save(updated)
		val now = saved.updatedAt
		val eventType = when {
			to == ProductionTaskStatus.BLOCKED -> ProductionTaskHistoryEventType.BLOCKED
			from == ProductionTaskStatus.BLOCKED -> ProductionTaskHistoryEventType.UNBLOCKED
			to == ProductionTaskStatus.COMPLETED -> ProductionTaskHistoryEventType.COMPLETED
			else -> ProductionTaskHistoryEventType.STATUS_CHANGED
		}
		traces.saveHistoryEvent(
			ProductionTaskHistoryEvent(
				taskId = saved.id,
				eventType = eventType,
				actorUserId = cmd.actorUserId,
				eventAt = now,
				fromStatus = from,
				toStatus = to,
				reason = if (to == ProductionTaskStatus.BLOCKED) cmd.reason?.trim() else null,
				note = cmd.note?.trim()?.takeIf { it.isNotEmpty() },
			),
		)
		val summary = when (to) {
			ProductionTaskStatus.BLOCKED -> "Заблокирована ${saved.taskNumber}"
			ProductionTaskStatus.COMPLETED -> "Завершена ${saved.taskNumber}"
			ProductionTaskStatus.IN_PROGRESS -> if (from == ProductionTaskStatus.BLOCKED) "Разблокирована ${saved.taskNumber}" else "В работе ${saved.taskNumber}"
			ProductionTaskStatus.NOT_STARTED -> "Статус: не начато (${saved.taskNumber})"
		}
		audit.record(
			eventType = "PRODUCTION_TASK_STATUS_${to.name}",
			actorUserId = cmd.actorUserId,
			taskId = saved.id,
			summary = summary,
			eventAt = now,
		)
		if (cmd.actorUserId != saved.createdByUserId) {
			try {
				notifications.create(
					CreateNotificationCommand(
						recipientUserId = saved.createdByUserId,
						type = NotificationType.STATUS_CHANGED,
						title = "Задача ${saved.taskNumber}: статус изменён на ${to.name}",
						targetType = NotificationTargetType.PRODUCTION_TASK,
						targetId = saved.taskNumber,
						targetEntityId = saved.id,
					),
				)
			} catch (e: Exception) {
				log.warn("Failed to create STATUS_CHANGED notification for task {}", saved.taskNumber, e)
			}
		}
		return ProductionTaskMutationResult.Success(Unit)
	}
}
