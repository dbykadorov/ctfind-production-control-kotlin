package com.ctfind.productioncontrol.production.adapter.persistence

import com.ctfind.productioncontrol.orders.domain.OrderStatus
import com.ctfind.productioncontrol.production.application.ProductionOrderSourcePort
import com.ctfind.productioncontrol.production.application.ProductionTaskListQuery
import com.ctfind.productioncontrol.production.application.ProductionTaskOrderSummary
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class ProductionTaskQueryPersistenceTests {

	@Test
	fun `filter matches order customer search text`() {
		val orderId = UUID.fromString("11111111-1111-1111-1111-111111111111")
		val taskId = UUID.fromString("22222222-2222-2222-2222-222222222222")
		val row = ProductionTaskEntity(
			id = taskId,
			taskNumber = "PT-000099",
			orderId = orderId,
			orderItemId = null,
			purpose = "Work",
			itemName = "Item",
			quantity = BigDecimal.ONE,
			uom = "pcs",
			status = ProductionTaskStatus.NOT_STARTED,
			createdByUserId = UUID.randomUUID(),
			createdAt = Instant.parse("2026-04-27T10:00:00Z"),
			updatedAt = Instant.parse("2026-04-27T10:00:00Z"),
		)
		val orderSource = object : ProductionOrderSourcePort {
			override fun findOrderSource(id: UUID): ProductionTaskOrderSummary? =
				ProductionTaskOrderSummary(
					id = orderId,
					orderNumber = "ORD-9",
					customerDisplayName = "ООО Romashka",
					status = OrderStatus.IN_WORK,
					deliveryDate = LocalDate.parse("2026-06-01"),
				).takeIf { id == orderId }

			override fun findOrderItemSource(orderId: UUID, orderItemId: UUID) = null
		}
		val filtered = filterProductionTaskEntitiesForQuery(
			listOf(row),
			ProductionTaskListQuery(search = "romash"),
			null,
			setOf(com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE),
			orderSource,
		)
		assertEquals(1, filtered.size)
	}

	@Test
	fun `blockedOnly keeps blocked tasks`() {
		val oid = UUID.randomUUID()
		val blocked = ProductionTaskEntity(
			taskNumber = "PT-B",
			orderId = oid,
			purpose = "p",
			itemName = "i",
			status = ProductionTaskStatus.BLOCKED,
			blockedReason = "x",
			createdByUserId = UUID.randomUUID(),
			createdAt = Instant.now(),
			updatedAt = Instant.now(),
		)
		val open = ProductionTaskEntity(
			taskNumber = "PT-O",
			orderId = oid,
			purpose = "p2",
			itemName = "i2",
			status = ProductionTaskStatus.IN_PROGRESS,
			createdByUserId = UUID.randomUUID(),
			createdAt = Instant.now(),
			updatedAt = Instant.now(),
		)
		val noOrders = object : ProductionOrderSourcePort {
			override fun findOrderSource(orderId: UUID) = null
			override fun findOrderItemSource(orderId: UUID, orderItemId: UUID) = null
		}
		val r = filterProductionTaskEntitiesForQuery(
			listOf(blocked, open),
			ProductionTaskListQuery(blockedOnly = true),
			null,
			setOf("PRODUCTION_SUPERVISOR"),
			noOrders,
		)
		assertEquals(1, r.size)
		assertEquals("PT-B", r.single().taskNumber)
	}
}
