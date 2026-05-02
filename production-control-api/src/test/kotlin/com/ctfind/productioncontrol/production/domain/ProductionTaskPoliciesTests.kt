package com.ctfind.productioncontrol.production.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductionTaskPoliciesTests {

	@Test
	fun `linear lifecycle and blocked interrupt transitions are allowed`() {
		assertTrue(ProductionTaskStatusPolicy.isAllowed(ProductionTaskStatus.NOT_STARTED, ProductionTaskStatus.IN_PROGRESS))
		assertTrue(ProductionTaskStatusPolicy.isAllowed(ProductionTaskStatus.IN_PROGRESS, ProductionTaskStatus.COMPLETED))
		assertTrue(ProductionTaskStatusPolicy.isAllowed(ProductionTaskStatus.NOT_STARTED, ProductionTaskStatus.BLOCKED))
		assertTrue(ProductionTaskStatusPolicy.isAllowed(ProductionTaskStatus.IN_PROGRESS, ProductionTaskStatus.BLOCKED))
		assertTrue(ProductionTaskStatusPolicy.isAllowed(ProductionTaskStatus.BLOCKED, ProductionTaskStatus.NOT_STARTED))
		assertTrue(ProductionTaskStatusPolicy.isAllowed(ProductionTaskStatus.BLOCKED, ProductionTaskStatus.IN_PROGRESS))
	}

	@Test
	fun `skipped reverse repeated and completed transitions are rejected`() {
		assertFalse(ProductionTaskStatusPolicy.isAllowed(ProductionTaskStatus.NOT_STARTED, ProductionTaskStatus.COMPLETED))
		assertFalse(ProductionTaskStatusPolicy.isAllowed(ProductionTaskStatus.IN_PROGRESS, ProductionTaskStatus.NOT_STARTED))
		assertFalse(ProductionTaskStatusPolicy.isAllowed(ProductionTaskStatus.IN_PROGRESS, ProductionTaskStatus.IN_PROGRESS))
		assertFalse(ProductionTaskStatusPolicy.isAllowed(ProductionTaskStatus.BLOCKED, ProductionTaskStatus.COMPLETED))
		assertFalse(ProductionTaskStatusPolicy.isAllowed(ProductionTaskStatus.COMPLETED, ProductionTaskStatus.IN_PROGRESS))
		assertFailsWith<InvalidProductionTaskStatusTransition> {
			ProductionTaskStatusPolicy.assertAllowed(ProductionTaskStatus.NOT_STARTED, ProductionTaskStatus.COMPLETED)
		}
	}

	@Test
	fun `unblock returns task to previous active status only`() {
		assertEquals(ProductionTaskStatus.NOT_STARTED, ProductionTaskStatusPolicy.unblockedStatus(ProductionTaskStatus.NOT_STARTED))
		assertEquals(ProductionTaskStatus.IN_PROGRESS, ProductionTaskStatusPolicy.unblockedStatus(ProductionTaskStatus.IN_PROGRESS))

		assertFailsWith<IllegalArgumentException> {
			ProductionTaskStatusPolicy.unblockedStatus(null)
		}
		assertFailsWith<IllegalArgumentException> {
			ProductionTaskStatusPolicy.unblockedStatus(ProductionTaskStatus.COMPLETED)
		}
	}
}
