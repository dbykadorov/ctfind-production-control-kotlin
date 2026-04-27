package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.production.domain.ProductionTask
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEvent
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEventType
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class CreateProductionTasksFromOrderUseCase(
	private val tasks: ProductionTaskPort,
	private val orderSource: ProductionOrderSourcePort,
	private val executors: ProductionExecutorPort,
	private val numbers: ProductionTaskNumberPort,
	private val traces: ProductionTaskTracePort,
	private val audit: ProductionTaskAuditService,
) {

	@Transactional
	fun execute(cmd: CreateProductionTasksFromOrderCommand): ProductionTaskMutationResult<List<CreatedProductionTaskSummary>> {
		if (!canCreateProductionTasks(cmd.roleCodes)) {
			return ProductionTaskMutationResult.Forbidden
		}
		if (cmd.tasks.isEmpty()) {
			return ProductionTaskMutationResult.ValidationFailed("At least one task is required.")
		}
		val order = orderSource.findOrderSource(cmd.orderId)
			?: return ProductionTaskMutationResult.ValidationFailed(
				message = "Order not found.",
				errorCode = "validation_failed",
				details = mapOf("orderId" to cmd.orderId.toString()),
			)

		val seenPurposeByItem = mutableSetOf<Pair<UUID, String>>()
		val validatedDrafts = mutableListOf<ValidatedCreateDraft>()
		for (draft in cmd.tasks) {
			val orderItemId = draft.orderItemId
				?: return ProductionTaskMutationResult.ValidationFailed("orderItemId is required for each task.")
			val purpose = draft.purpose.trim()
			if (purpose.isEmpty()) {
				return ProductionTaskMutationResult.ValidationFailed("Task purpose must not be blank.")
			}
			val key = orderItemId to purpose.lowercase()
			if (key in seenPurposeByItem) {
				return ProductionTaskMutationResult.ValidationFailed(
					message = "Duplicate purpose for the same order line in this request.",
					details = mapOf("orderItemId" to orderItemId.toString(), "purpose" to purpose),
				)
			}
			seenPurposeByItem += key
			val line = orderSource.findOrderItemSource(cmd.orderId, orderItemId)
				?: return ProductionTaskMutationResult.ValidationFailed(
					message = "Order line not found for this order.",
					errorCode = "order_item_not_found",
					details = mapOf("orderItemId" to orderItemId.toString()),
				)
			if (tasks.existsByOrderItemIdAndPurpose(orderItemId, purpose)) {
				return ProductionTaskMutationResult.ValidationFailed(
					message = "A task with this purpose already exists for the order line.",
					details = mapOf("orderItemId" to orderItemId.toString(), "purpose" to purpose),
				)
			}
			if (draft.quantity <= BigDecimal.ZERO) {
				return ProductionTaskMutationResult.ValidationFailed("Task quantity must be positive.")
			}
			if (draft.quantity > line.quantity) {
				return ProductionTaskMutationResult.ValidationFailed(
					message = "Task quantity cannot exceed the order line quantity.",
					details = mapOf("orderItemId" to orderItemId.toString()),
				)
			}
			draft.executorUserId?.let { execId ->
				executors.findExecutor(execId)
					?: return ProductionTaskMutationResult.ValidationFailed(
						message = "Executor is not a valid active production user.",
						details = mapOf("executorUserId" to execId.toString()),
					)
			}
			if (draft.uom.isBlank()) {
				return ProductionTaskMutationResult.ValidationFailed("UoM must not be blank.")
			}
			if (invalidDateRange(draft.plannedStartDate, draft.plannedFinishDate)) {
				return ProductionTaskMutationResult.ValidationFailed(
					message = "Planned finish date must not be before planned start date.",
					details = mapOf("plannedStartDate" to draft.plannedStartDate.toString(), "plannedFinishDate" to draft.plannedFinishDate.toString()),
				)
			}
			validatedDrafts += ValidatedCreateDraft(
				orderItemId = orderItemId,
				purpose = purpose,
				quantity = draft.quantity,
				uom = draft.uom.trim(),
				executorUserId = draft.executorUserId,
				plannedStartDate = draft.plannedStartDate,
				plannedFinishDate = draft.plannedFinishDate,
				line = line,
			)
		}

		val now = Instant.now()
		val out = mutableListOf<CreatedProductionTaskSummary>()
		for (draft in validatedDrafts) {

			val id = UUID.randomUUID()
			val task = ProductionTask(
				id = id,
				taskNumber = numbers.nextTaskNumber(),
				orderId = order.id,
				orderItemId = draft.orderItemId,
				purpose = draft.purpose,
				itemName = draft.line.itemName,
				quantity = draft.quantity,
				uom = draft.uom,
				status = ProductionTaskStatus.NOT_STARTED,
				previousActiveStatus = null,
				executorUserId = draft.executorUserId,
				plannedStartDate = draft.plannedStartDate,
				plannedFinishDate = draft.plannedFinishDate,
				blockedReason = null,
				createdByUserId = cmd.actorUserId,
				createdAt = now,
				updatedAt = now,
			)
			val saved = tasks.save(task)
			traces.saveHistoryEvent(
				ProductionTaskHistoryEvent(
					taskId = saved.id,
					eventType = ProductionTaskHistoryEventType.CREATED,
					actorUserId = cmd.actorUserId,
					eventAt = now,
					fromStatus = null,
					toStatus = ProductionTaskStatus.NOT_STARTED,
					note = null,
					reason = null,
				),
			)
			audit.record(
				eventType = "PRODUCTION_TASK_CREATED",
				actorUserId = cmd.actorUserId,
				taskId = saved.id,
				summary = "Создана производственная задача ${saved.taskNumber} (${draft.purpose})",
				metadata = null,
				eventAt = now,
			)
			out.add(
				CreatedProductionTaskSummary(
					id = saved.id,
					taskNumber = saved.taskNumber,
					status = saved.status,
					version = saved.version,
				),
			)
		}
		return ProductionTaskMutationResult.Success(out)
	}

	private fun invalidDateRange(start: LocalDate?, finish: LocalDate?): Boolean =
		start != null && finish != null && finish.isBefore(start)

	private data class ValidatedCreateDraft(
		val orderItemId: UUID,
		val purpose: String,
		val quantity: BigDecimal,
		val uom: String,
		val executorUserId: UUID?,
		val plannedStartDate: LocalDate?,
		val plannedFinishDate: LocalDate?,
		val line: ProductionTaskOrderItemSummary,
	)
}
