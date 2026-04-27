package com.ctfind.productioncontrol.orders.domain

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderPoliciesTests {

	@Test
	fun `direct forward transitions are allowed`() {
		assertTrue(OrderStatusPolicy.isDirectForward(OrderStatus.NEW, OrderStatus.IN_WORK))
		assertTrue(OrderStatusPolicy.isDirectForward(OrderStatus.IN_WORK, OrderStatus.READY))
		assertTrue(OrderStatusPolicy.isDirectForward(OrderStatus.READY, OrderStatus.SHIPPED))
	}

	@Test
	fun `skipped reverse and repeated transitions are rejected`() {
		assertFalse(OrderStatusPolicy.isDirectForward(OrderStatus.NEW, OrderStatus.READY))
		assertFalse(OrderStatusPolicy.isDirectForward(OrderStatus.READY, OrderStatus.IN_WORK))
		assertFalse(OrderStatusPolicy.isDirectForward(OrderStatus.IN_WORK, OrderStatus.IN_WORK))
		assertFailsWith<InvalidOrderStatusTransition> {
			OrderStatusPolicy.assertDirectForward(OrderStatus.SHIPPED, OrderStatus.READY)
		}
	}
}
