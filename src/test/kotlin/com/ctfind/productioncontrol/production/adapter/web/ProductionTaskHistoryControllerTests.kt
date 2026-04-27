package com.ctfind.productioncontrol.production.adapter.web

import com.ctfind.productioncontrol.orders.domain.OrderStatus
import com.ctfind.productioncontrol.production.application.ProductionTaskDetailView
import com.ctfind.productioncontrol.production.application.ProductionTaskHistoryEventView
import com.ctfind.productioncontrol.production.application.ProductionTaskListRowView
import com.ctfind.productioncontrol.production.application.ProductionTaskOrderItemSummary
import com.ctfind.productioncontrol.production.application.ProductionTaskOrderSummary
import com.ctfind.productioncontrol.production.application.productionTaskStatusLabelRu
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEventType
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProductionTaskHistoryControllerTests {

	@Test
	fun `detail response surfaces history entries in given order with full enriched fields`() {
		val view = ProductionTaskDetailView(
			row = sampleListRow(),
			allowedActions = emptySet(),
			history = listOf(
				ProductionTaskHistoryEventView(
					type = ProductionTaskHistoryEventType.CREATED,
					actorDisplayName = "Менеджер Анна",
					eventAt = Instant.parse("2026-04-27T10:00:00Z"),
					fromStatus = null,
					toStatus = ProductionTaskStatus.NOT_STARTED,
					previousExecutorDisplayName = null,
					newExecutorDisplayName = null,
					plannedStartDateBefore = null,
					plannedStartDateAfter = null,
					plannedFinishDateBefore = null,
					plannedFinishDateAfter = null,
					note = null,
					reason = null,
				),
				ProductionTaskHistoryEventView(
					type = ProductionTaskHistoryEventType.ASSIGNED,
					actorDisplayName = "Менеджер Анна",
					eventAt = Instant.parse("2026-04-27T11:00:00Z"),
					fromStatus = null,
					toStatus = null,
					previousExecutorDisplayName = "Исполнитель А",
					newExecutorDisplayName = "Исполнитель Б",
					plannedStartDateBefore = null,
					plannedStartDateAfter = null,
					plannedFinishDateBefore = null,
					plannedFinishDateAfter = null,
					note = "переназначение",
					reason = null,
				),
				ProductionTaskHistoryEventView(
					type = ProductionTaskHistoryEventType.PLANNING_UPDATED,
					actorDisplayName = "Менеджер Анна",
					eventAt = Instant.parse("2026-04-27T11:30:00Z"),
					fromStatus = null,
					toStatus = null,
					previousExecutorDisplayName = null,
					newExecutorDisplayName = null,
					plannedStartDateBefore = LocalDate.parse("2026-05-01"),
					plannedStartDateAfter = LocalDate.parse("2026-05-02"),
					plannedFinishDateBefore = LocalDate.parse("2026-05-03"),
					plannedFinishDateAfter = LocalDate.parse("2026-05-05"),
					note = null,
					reason = null,
				),
				ProductionTaskHistoryEventView(
					type = ProductionTaskHistoryEventType.BLOCKED,
					actorDisplayName = "Иван",
					eventAt = Instant.parse("2026-04-27T13:00:00Z"),
					fromStatus = ProductionTaskStatus.IN_PROGRESS,
					toStatus = ProductionTaskStatus.BLOCKED,
					previousExecutorDisplayName = null,
					newExecutorDisplayName = null,
					plannedStartDateBefore = null,
					plannedStartDateAfter = null,
					plannedFinishDateBefore = null,
					plannedFinishDateAfter = null,
					note = "Ожидаем поставку",
					reason = "Нет материала",
				),
			),
			createdAt = Instant.parse("2026-04-27T10:00:00Z"),
		)

		val response = view.toDetailResponse()

		assertEquals(4, response.history.size)
		assertEquals(
			listOf(
				ProductionTaskHistoryEventType.CREATED,
				ProductionTaskHistoryEventType.ASSIGNED,
				ProductionTaskHistoryEventType.PLANNING_UPDATED,
				ProductionTaskHistoryEventType.BLOCKED,
			),
			response.history.map { it.type },
		)

		val assigned = response.history.first { it.type == ProductionTaskHistoryEventType.ASSIGNED }
		assertEquals("Исполнитель А", assigned.previousExecutorDisplayName)
		assertEquals("Исполнитель Б", assigned.newExecutorDisplayName)
		assertEquals("переназначение", assigned.note)

		val planning = response.history.first { it.type == ProductionTaskHistoryEventType.PLANNING_UPDATED }
		assertEquals(LocalDate.parse("2026-05-01"), planning.plannedStartDateBefore)
		assertEquals(LocalDate.parse("2026-05-02"), planning.plannedStartDateAfter)
		assertEquals(LocalDate.parse("2026-05-03"), planning.plannedFinishDateBefore)
		assertEquals(LocalDate.parse("2026-05-05"), planning.plannedFinishDateAfter)

		val blocked = response.history.first { it.type == ProductionTaskHistoryEventType.BLOCKED }
		assertEquals(ProductionTaskStatus.IN_PROGRESS, blocked.fromStatus)
		assertEquals(ProductionTaskStatus.BLOCKED, blocked.toStatus)
		assertEquals("Нет материала", blocked.reason)
		assertEquals("Ожидаем поставку", blocked.note)
	}

	@Test
	fun `detail response history list is empty when there are no events`() {
		val view = ProductionTaskDetailView(
			row = sampleListRow(),
			allowedActions = emptySet(),
			history = emptyList(),
			createdAt = Instant.parse("2026-04-27T10:00:00Z"),
		)

		val response = view.toDetailResponse()

		assertEquals(0, response.history.size)
	}

	@Test
	fun `enriched fields are absent for events that have no executor or planning context`() {
		val view = ProductionTaskDetailView(
			row = sampleListRow(),
			allowedActions = emptySet(),
			history = listOf(
				ProductionTaskHistoryEventView(
					type = ProductionTaskHistoryEventType.CREATED,
					actorDisplayName = "Менеджер Анна",
					eventAt = Instant.parse("2026-04-27T10:00:00Z"),
					fromStatus = null,
					toStatus = ProductionTaskStatus.NOT_STARTED,
					previousExecutorDisplayName = null,
					newExecutorDisplayName = null,
					plannedStartDateBefore = null,
					plannedStartDateAfter = null,
					plannedFinishDateBefore = null,
					plannedFinishDateAfter = null,
					note = null,
					reason = null,
				),
			),
			createdAt = Instant.parse("2026-04-27T10:00:00Z"),
		)

		val event = view.toDetailResponse().history.single()

		assertNull(event.previousExecutorDisplayName)
		assertNull(event.newExecutorDisplayName)
		assertNull(event.plannedStartDateBefore)
		assertNull(event.plannedStartDateAfter)
		assertNull(event.plannedFinishDateBefore)
		assertNull(event.plannedFinishDateAfter)
		assertNull(event.note)
		assertNull(event.reason)
	}

	private fun sampleListRow(): ProductionTaskListRowView =
		ProductionTaskListRowView(
			id = UUID.fromString("60000000-0000-0000-0000-000000000006"),
			taskNumber = "PT-000001",
			purpose = "Раскрой",
			order = ProductionTaskOrderSummary(
				id = UUID.fromString("10000000-0000-0000-0000-000000000001"),
				orderNumber = "ORD-1",
				customerDisplayName = "Acme",
				status = OrderStatus.IN_WORK,
				deliveryDate = LocalDate.parse("2026-05-15"),
			),
			orderItem = ProductionTaskOrderItemSummary(
				id = UUID.fromString("20000000-0000-0000-0000-000000000002"),
				lineNo = 1,
				itemName = "Столешница",
				quantity = BigDecimal("2"),
				uom = "шт",
			),
			quantity = BigDecimal("2"),
			uom = "шт",
			status = ProductionTaskStatus.NOT_STARTED,
			statusLabel = productionTaskStatusLabelRu(ProductionTaskStatus.NOT_STARTED),
			previousActiveStatus = null,
			executor = null,
			plannedStartDate = null,
			plannedFinishDate = null,
			blockedReason = null,
			updatedAt = Instant.parse("2026-04-27T10:00:00Z"),
			version = 0,
		)
}
