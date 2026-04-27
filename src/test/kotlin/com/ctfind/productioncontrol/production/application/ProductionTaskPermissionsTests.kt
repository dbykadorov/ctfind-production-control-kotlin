package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.orders.application.ORDER_MANAGER_ROLE_CODE
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductionTaskPermissionsTests {

	@Test
	fun `admin can perform every production task action`() {
		val roles = setOf(ADMIN_ROLE_CODE)

		assertTrue(canCreateProductionTasks(roles))
		assertTrue(canAssignProductionTasks(roles))
		assertTrue(canViewAllProductionTasks(roles))
		assertTrue(canUpdateAnyProductionTaskStatus(roles))
	}

	@Test
	fun `order manager can create assign and plan but cannot update arbitrary execution status`() {
		val roles = setOf(ORDER_MANAGER_ROLE_CODE)

		assertTrue(canCreateProductionTasks(roles))
		assertTrue(canAssignProductionTasks(roles))
		assertFalse(canUpdateAnyProductionTaskStatus(roles))
	}

	@Test
	fun `production supervisor can view all assign and update any status`() {
		val roles = setOf(PRODUCTION_SUPERVISOR_ROLE_CODE)

		assertFalse(canCreateProductionTasks(roles))
		assertTrue(canAssignProductionTasks(roles))
		assertTrue(canViewAllProductionTasks(roles))
		assertTrue(canUpdateAnyProductionTaskStatus(roles))
	}

	@Test
	fun `executor can view and update only assigned task`() {
		val executorId = UUID.randomUUID()
		val otherExecutorId = UUID.randomUUID()
		val roles = setOf(PRODUCTION_EXECUTOR_ROLE_CODE)

		assertFalse(canViewAllProductionTasks(roles))
		assertTrue(canViewProductionTask(roles, currentUserId = executorId, executorUserId = executorId))
		assertFalse(canViewProductionTask(roles, currentUserId = executorId, executorUserId = otherExecutorId))
		assertTrue(canUpdateAssignedProductionTaskStatus(roles, currentUserId = executorId, executorUserId = executorId))
		assertFalse(canUpdateAssignedProductionTaskStatus(roles, currentUserId = executorId, executorUserId = otherExecutorId))
	}

	@Test
	fun `read only roles cannot mutate production tasks`() {
		val roles = setOf("WAREHOUSE")

		assertFalse(canCreateProductionTasks(roles))
		assertFalse(canAssignProductionTasks(roles))
		assertFalse(canUpdateAnyProductionTaskStatus(roles))
		assertFalse(canUpdateAssignedProductionTaskStatus(roles, currentUserId = UUID.randomUUID(), executorUserId = UUID.randomUUID()))
	}
}
