package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEvent
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProductionTaskHistoryUseCase(
	private val traces: ProductionTaskTracePort,
	private val actorLookup: ProductionActorLookupPort,
) {

	fun timeline(taskId: UUID): List<ProductionTaskHistoryEventView> =
		traces.findHistoryEvents(taskId).map { it.toView() }

	private fun ProductionTaskHistoryEvent.toView(): ProductionTaskHistoryEventView =
		ProductionTaskHistoryEventView(
			type = eventType,
			actorDisplayName = resolveActorName(actorUserId),
			eventAt = eventAt,
			fromStatus = fromStatus,
			toStatus = toStatus,
			previousExecutorDisplayName = previousExecutorUserId?.let(::resolveActorName),
			newExecutorDisplayName = newExecutorUserId?.let(::resolveActorName),
			plannedStartDateBefore = plannedStartDateBefore,
			plannedStartDateAfter = plannedStartDateAfter,
			plannedFinishDateBefore = plannedFinishDateBefore,
			plannedFinishDateAfter = plannedFinishDateAfter,
			note = note,
			reason = reason,
		)

	private fun resolveActorName(userId: UUID): String =
		actorLookup.displayName(userId) ?: userId.toString()
}
