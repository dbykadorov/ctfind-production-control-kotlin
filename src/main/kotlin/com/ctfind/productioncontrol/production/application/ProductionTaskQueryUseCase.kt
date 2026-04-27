package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.orders.domain.OrderStatus
import com.ctfind.productioncontrol.production.domain.ProductionTask
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class ProductionTaskQueryUseCase(
	private val tasks: ProductionTaskPort,
	private val orderSource: ProductionOrderSourcePort,
	private val executors: ProductionExecutorPort,
	private val history: ProductionTaskHistoryUseCase,
) {

	fun list(query: ProductionTaskListQuery, actor: AuthenticatedProductionActor): ProductionTaskPageResult<ProductionTaskListRowView> {
		val page = tasks.search(query, actor.userId, actor.roleCodes)
		return ProductionTaskPageResult(
			items = page.items.map { it.toListRowView() },
			page = page.page,
			size = page.size,
			totalItems = page.totalItems,
		)
	}

	fun detail(taskId: UUID, actor: AuthenticatedProductionActor): ProductionTaskDetailQueryResult {
		val task = tasks.findById(taskId) ?: return ProductionTaskDetailQueryResult.NotFound
		if (!canViewProductionTask(actor.roleCodes, actor.userId, task.executorUserId)) {
			return ProductionTaskDetailQueryResult.Forbidden
		}
		val row = task.toListRowView()
		return ProductionTaskDetailQueryResult.Found(
			ProductionTaskDetailView(
				row = row,
				allowedActions = allowedProductionTaskActions(task, actor.roleCodes, actor.userId),
				history = history.timeline(taskId),
				createdAt = task.createdAt,
			),
		)
	}

	private fun ProductionTask.toListRowView(): ProductionTaskListRowView {
		val order = orderSource.findOrderSource(orderId)
			?: ProductionTaskOrderSummary(
				id = orderId,
				orderNumber = "?",
				customerDisplayName = "—",
				status = OrderStatus.NEW,
				deliveryDate = LocalDate.EPOCH,
			)
		val item = resolveOrderItem(this)
		val executor = executorUserId?.let { executors.findExecutor(it) }
		return ProductionTaskListRowView(
			id = id,
			taskNumber = taskNumber,
			purpose = purpose,
			order = order,
			orderItem = item,
			quantity = quantity,
			uom = uom,
			status = status,
			statusLabel = productionTaskStatusLabelRu(status),
			previousActiveStatus = previousActiveStatus,
			executor = executor,
			plannedStartDate = plannedStartDate,
			plannedFinishDate = plannedFinishDate,
			blockedReason = blockedReason,
			updatedAt = updatedAt,
			version = version,
		)
	}

	private fun resolveOrderItem(task: ProductionTask): ProductionTaskOrderItemSummary? {
		val oid = task.orderItemId ?: return ProductionTaskOrderItemSummary(
			id = UUID.nameUUIDFromBytes(("no-item:${task.id}").toByteArray()),
			lineNo = 0,
			itemName = task.itemName,
			quantity = task.quantity,
			uom = task.uom,
		)
		return orderSource.findOrderItemSource(task.orderId, oid)
			?: ProductionTaskOrderItemSummary(
				id = oid,
				lineNo = 0,
				itemName = task.itemName,
				quantity = task.quantity,
				uom = task.uom,
			)
	}

}

fun productionTaskStatusLabelRu(status: ProductionTaskStatus): String =
	when (status) {
		ProductionTaskStatus.NOT_STARTED -> "не начато"
		ProductionTaskStatus.IN_PROGRESS -> "в работе"
		ProductionTaskStatus.BLOCKED -> "заблокировано"
		ProductionTaskStatus.COMPLETED -> "выполнено"
	}
