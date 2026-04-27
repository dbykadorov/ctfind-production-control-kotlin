package com.ctfind.productioncontrol.production.domain

enum class ProductionTaskStatus {
	NOT_STARTED,
	IN_PROGRESS,
	BLOCKED,
	COMPLETED,
}

enum class ProductionTaskAction {
	ASSIGN,
	PLAN,
	START,
	BLOCK,
	UNBLOCK,
	COMPLETE,
}

enum class ProductionTaskHistoryEventType {
	CREATED,
	ASSIGNED,
	PLANNING_UPDATED,
	STATUS_CHANGED,
	BLOCKED,
	UNBLOCKED,
	COMPLETED,
}
