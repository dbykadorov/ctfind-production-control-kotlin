package com.ctfind.productioncontrol.production.adapter.persistence

import com.ctfind.productioncontrol.production.application.ProductionOrderSourcePort
import com.ctfind.productioncontrol.production.application.ProductionTaskListQuery
import com.ctfind.productioncontrol.production.application.canViewAllProductionTasks
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import java.util.UUID

internal fun filterProductionTaskEntitiesForQuery(
	rows: List<ProductionTaskEntity>,
	query: ProductionTaskListQuery,
	currentUserId: UUID?,
	roleCodes: Set<String>,
	orderSource: ProductionOrderSourcePort,
): List<ProductionTaskEntity> {
	val search = query.search?.trim()?.lowercase().orEmpty()
	return rows.filter { row ->
		(canViewAllProductionTasks(roleCodes) || (currentUserId != null && row.executorUserId == currentUserId)) &&
			matchesProductionTaskSearch(search, row, orderSource) &&
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

internal fun matchesProductionTaskSearch(
	search: String,
	row: ProductionTaskEntity,
	orderSource: ProductionOrderSourcePort,
): Boolean {
	if (search.isBlank()) return true
	if (row.taskNumber.lowercase().contains(search) ||
		row.purpose.lowercase().contains(search) ||
		row.itemName.lowercase().contains(search)
	) {
		return true
	}
	val order = orderSource.findOrderSource(row.orderId) ?: return false
	return order.orderNumber.lowercase().contains(search) ||
		order.customerDisplayName.lowercase().contains(search)
}
