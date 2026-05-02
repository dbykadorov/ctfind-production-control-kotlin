package com.ctfind.productioncontrol.production.domain

class InvalidProductionTaskStatusTransition(
	from: ProductionTaskStatus,
	to: ProductionTaskStatus,
) : IllegalArgumentException("Invalid production task status transition: $from -> $to")

object ProductionTaskStatusPolicy {
	fun isAllowed(from: ProductionTaskStatus, to: ProductionTaskStatus): Boolean =
		when (from) {
			ProductionTaskStatus.NOT_STARTED -> to == ProductionTaskStatus.IN_PROGRESS || to == ProductionTaskStatus.BLOCKED
			ProductionTaskStatus.IN_PROGRESS -> to == ProductionTaskStatus.COMPLETED || to == ProductionTaskStatus.BLOCKED
			ProductionTaskStatus.BLOCKED -> to == ProductionTaskStatus.NOT_STARTED || to == ProductionTaskStatus.IN_PROGRESS
			ProductionTaskStatus.COMPLETED -> false
		}

	fun assertAllowed(from: ProductionTaskStatus, to: ProductionTaskStatus) {
		if (!isAllowed(from, to))
			throw InvalidProductionTaskStatusTransition(from, to)
	}

	fun unblockedStatus(previousActiveStatus: ProductionTaskStatus?): ProductionTaskStatus =
		when (previousActiveStatus) {
			ProductionTaskStatus.NOT_STARTED,
			ProductionTaskStatus.IN_PROGRESS,
			-> previousActiveStatus
			else -> throw IllegalArgumentException("Blocked task does not have a previous active status")
		}
}
