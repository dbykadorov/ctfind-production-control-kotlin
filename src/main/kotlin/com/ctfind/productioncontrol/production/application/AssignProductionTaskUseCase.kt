package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.production.domain.ProductionTask
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEvent
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEventType
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AssignProductionTaskUseCase(
	private val tasks: ProductionTaskPort,
	private val executors: ProductionExecutorPort,
	private val traces: ProductionTaskTracePort,
	private val audit: ProductionTaskAuditService,
) {

	@Transactional
	fun execute(cmd: AssignProductionTaskCommand): ProductionTaskMutationResult<Unit> {
		if (!canAssignProductionTasks(cmd.roleCodes)) {
			return ProductionTaskMutationResult.Forbidden
		}
		val task = tasks.findById(cmd.taskId) ?: return ProductionTaskMutationResult.NotFound
		if (task.status == ProductionTaskStatus.COMPLETED) {
			return ProductionTaskMutationResult.ValidationFailed("Completed tasks cannot be reassigned or replanned.")
		}
		if (
			cmd.plannedStartDate != null &&
			cmd.plannedFinishDate != null &&
			cmd.plannedFinishDate.isBefore(cmd.plannedStartDate)
		) {
			return ProductionTaskMutationResult.ValidationFailed(
				message = "Planned finish date must not be before planned start date.",
				details = mapOf(
					"plannedStartDate" to cmd.plannedStartDate.toString(),
					"plannedFinishDate" to cmd.plannedFinishDate.toString(),
				),
			)
		}
		if (task.version != cmd.expectedVersion) {
			return ProductionTaskMutationResult.StaleVersion
		}
		val exec = executors.findExecutor(cmd.executorUserId)
			?: return ProductionTaskMutationResult.ValidationFailed(
				message = "Executor is not a valid active production user.",
				details = mapOf("executorUserId" to cmd.executorUserId.toString()),
			)

		val now = Instant.now()
		val executorChanged = task.executorUserId != cmd.executorUserId
		val planningChanged =
			task.plannedStartDate != cmd.plannedStartDate || task.plannedFinishDate != cmd.plannedFinishDate
		if (!executorChanged && !planningChanged && cmd.note.isNullOrBlank()) {
			return ProductionTaskMutationResult.Success(Unit)
		}

		val updated = tasks.save(
			task.copy(
				executorUserId = cmd.executorUserId,
				plannedStartDate = cmd.plannedStartDate,
				plannedFinishDate = cmd.plannedFinishDate,
				updatedAt = now,
			),
		)

		when {
			executorChanged -> {
				traces.saveHistoryEvent(
					ProductionTaskHistoryEvent(
						taskId = updated.id,
						eventType = ProductionTaskHistoryEventType.ASSIGNED,
						actorUserId = cmd.actorUserId,
						eventAt = now,
						previousExecutorUserId = task.executorUserId,
						newExecutorUserId = cmd.executorUserId,
						plannedStartDateBefore = task.plannedStartDate,
						plannedStartDateAfter = updated.plannedStartDate,
						plannedFinishDateBefore = task.plannedFinishDate,
						plannedFinishDateAfter = updated.plannedFinishDate,
						note = cmd.note,
					),
				)
				audit.record(
					eventType = "PRODUCTION_TASK_ASSIGNED",
					actorUserId = cmd.actorUserId,
					taskId = updated.id,
					summary = "Назначен исполнитель ${exec.displayName} на ${updated.taskNumber}",
					eventAt = now,
				)
			}
			planningChanged -> {
				traces.saveHistoryEvent(
					ProductionTaskHistoryEvent(
						taskId = updated.id,
						eventType = ProductionTaskHistoryEventType.PLANNING_UPDATED,
						actorUserId = cmd.actorUserId,
						eventAt = now,
						plannedStartDateBefore = task.plannedStartDate,
						plannedStartDateAfter = updated.plannedStartDate,
						plannedFinishDateBefore = task.plannedFinishDate,
						plannedFinishDateAfter = updated.plannedFinishDate,
						note = cmd.note,
					),
				)
				audit.record(
					eventType = "PRODUCTION_TASK_PLANNING_UPDATED",
					actorUserId = cmd.actorUserId,
					taskId = updated.id,
					summary = "Обновлено планирование для ${updated.taskNumber}",
					eventAt = now,
				)
			}
			else -> {
				if (!cmd.note.isNullOrBlank()) {
					traces.saveHistoryEvent(
						ProductionTaskHistoryEvent(
							taskId = updated.id,
							eventType = ProductionTaskHistoryEventType.PLANNING_UPDATED,
							actorUserId = cmd.actorUserId,
							eventAt = now,
							note = cmd.note,
						),
					)
				}
			}
		}
		return ProductionTaskMutationResult.Success(Unit)
	}
}
