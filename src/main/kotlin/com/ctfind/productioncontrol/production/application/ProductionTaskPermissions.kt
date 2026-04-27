package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.orders.application.ORDER_MANAGER_ROLE_CODE
import java.util.UUID

const val PRODUCTION_SUPERVISOR_ROLE_CODE = "PRODUCTION_SUPERVISOR"
const val PRODUCTION_EXECUTOR_ROLE_CODE = "PRODUCTION_EXECUTOR"

fun canCreateProductionTasks(roleCodes: Set<String>): Boolean =
	roleCodes.any { it == ADMIN_ROLE_CODE || it == ORDER_MANAGER_ROLE_CODE }

fun canAssignProductionTasks(roleCodes: Set<String>): Boolean =
	roleCodes.any { it == ADMIN_ROLE_CODE || it == ORDER_MANAGER_ROLE_CODE || it == PRODUCTION_SUPERVISOR_ROLE_CODE }

fun canViewAllProductionTasks(roleCodes: Set<String>): Boolean =
	roleCodes.any { it == ADMIN_ROLE_CODE || it == ORDER_MANAGER_ROLE_CODE || it == PRODUCTION_SUPERVISOR_ROLE_CODE }

fun canViewProductionTask(
	roleCodes: Set<String>,
	currentUserId: UUID?,
	executorUserId: UUID?,
): Boolean =
	canViewAllProductionTasks(roleCodes) ||
		(roleCodes.contains(PRODUCTION_EXECUTOR_ROLE_CODE) && currentUserId != null && currentUserId == executorUserId)

fun canUpdateAnyProductionTaskStatus(roleCodes: Set<String>): Boolean =
	roleCodes.any { it == ADMIN_ROLE_CODE || it == PRODUCTION_SUPERVISOR_ROLE_CODE }

fun canUpdateAssignedProductionTaskStatus(
	roleCodes: Set<String>,
	currentUserId: UUID?,
	executorUserId: UUID?,
): Boolean =
	canUpdateAnyProductionTaskStatus(roleCodes) ||
		(roleCodes.contains(PRODUCTION_EXECUTOR_ROLE_CODE) && currentUserId != null && currentUserId == executorUserId)
