package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEvent
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEventType
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ProductionTaskHistoryUseCaseTests {

	private val taskId = UUID.fromString("11111111-1111-1111-1111-111111111111")
	private val managerId = UUID.fromString("22222222-2222-2222-2222-222222222222")
	private val executorAId = UUID.fromString("33333333-3333-3333-3333-333333333333")
	private val executorBId = UUID.fromString("44444444-4444-4444-4444-444444444444")

	@Test
	fun `timeline preserves chronological order from trace port`() {
		val events = listOf(
			event("2026-04-27T10:00:00Z", ProductionTaskHistoryEventType.CREATED),
			event("2026-04-27T11:00:00Z", ProductionTaskHistoryEventType.ASSIGNED, newExecutorUserId = executorAId),
			event("2026-04-27T12:00:00Z", ProductionTaskHistoryEventType.STATUS_CHANGED, fromStatus = ProductionTaskStatus.NOT_STARTED, toStatus = ProductionTaskStatus.IN_PROGRESS),
		)

		val timeline = useCase(events).timeline(taskId)

		assertEquals(
			listOf(
				ProductionTaskHistoryEventType.CREATED,
				ProductionTaskHistoryEventType.ASSIGNED,
				ProductionTaskHistoryEventType.STATUS_CHANGED,
			),
			timeline.map { it.type },
		)
		assertEquals(
			listOf(
				Instant.parse("2026-04-27T10:00:00Z"),
				Instant.parse("2026-04-27T11:00:00Z"),
				Instant.parse("2026-04-27T12:00:00Z"),
			),
			timeline.map { it.eventAt },
		)
	}

	@Test
	fun `actor display name is resolved through actor lookup port`() {
		val events = listOf(event("2026-04-27T10:00:00Z", ProductionTaskHistoryEventType.CREATED))

		val timeline = useCase(events).timeline(taskId)

		assertEquals("Менеджер Анна", timeline.single().actorDisplayName)
	}

	@Test
	fun `actor display name falls back to user id when not resolvable`() {
		val unknownActor = UUID.fromString("99999999-9999-9999-9999-999999999999")
		val events = listOf(event("2026-04-27T10:00:00Z", ProductionTaskHistoryEventType.CREATED, actorUserId = unknownActor))

		val timeline = useCase(events).timeline(taskId)

		assertEquals(unknownActor.toString(), timeline.single().actorDisplayName)
	}

	@Test
	fun `assignment events surface previous and new executor display names`() {
		val events = listOf(
			event(
				"2026-04-27T11:00:00Z",
				ProductionTaskHistoryEventType.ASSIGNED,
				previousExecutorUserId = executorAId,
				newExecutorUserId = executorBId,
			),
		)

		val view = useCase(events).timeline(taskId).single()

		assertEquals("Исполнитель А", view.previousExecutorDisplayName)
		assertEquals("Исполнитель Б", view.newExecutorDisplayName)
	}

	@Test
	fun `initial assignment has no previous executor name`() {
		val events = listOf(
			event(
				"2026-04-27T11:00:00Z",
				ProductionTaskHistoryEventType.ASSIGNED,
				previousExecutorUserId = null,
				newExecutorUserId = executorAId,
			),
		)

		val view = useCase(events).timeline(taskId).single()

		assertNull(view.previousExecutorDisplayName)
		assertEquals("Исполнитель А", view.newExecutorDisplayName)
	}

	@Test
	fun `planning updates surface start and finish before and after dates`() {
		val events = listOf(
			event(
				"2026-04-27T11:30:00Z",
				ProductionTaskHistoryEventType.PLANNING_UPDATED,
				plannedStartDateBefore = LocalDate.parse("2026-05-01"),
				plannedStartDateAfter = LocalDate.parse("2026-05-02"),
				plannedFinishDateBefore = LocalDate.parse("2026-05-03"),
				plannedFinishDateAfter = LocalDate.parse("2026-05-05"),
			),
		)

		val view = useCase(events).timeline(taskId).single()

		assertEquals(LocalDate.parse("2026-05-01"), view.plannedStartDateBefore)
		assertEquals(LocalDate.parse("2026-05-02"), view.plannedStartDateAfter)
		assertEquals(LocalDate.parse("2026-05-03"), view.plannedFinishDateBefore)
		assertEquals(LocalDate.parse("2026-05-05"), view.plannedFinishDateAfter)
	}

	@Test
	fun `block events surface reason and note details`() {
		val events = listOf(
			event(
				"2026-04-27T13:00:00Z",
				ProductionTaskHistoryEventType.BLOCKED,
				fromStatus = ProductionTaskStatus.IN_PROGRESS,
				toStatus = ProductionTaskStatus.BLOCKED,
				reason = "Нет материала",
				note = "Ожидаем поставку",
			),
		)

		val view = useCase(events).timeline(taskId).single()

		assertEquals("Нет материала", view.reason)
		assertEquals("Ожидаем поставку", view.note)
		assertEquals(ProductionTaskStatus.IN_PROGRESS, view.fromStatus)
		assertEquals(ProductionTaskStatus.BLOCKED, view.toStatus)
	}

	@Test
	fun `timeline returns empty list when no events exist`() {
		val view = useCase(emptyList()).timeline(taskId)

		assertNotNull(view)
		assertEquals(0, view.size)
	}

	private fun event(
		eventAt: String,
		eventType: ProductionTaskHistoryEventType,
		actorUserId: UUID = managerId,
		fromStatus: ProductionTaskStatus? = null,
		toStatus: ProductionTaskStatus? = null,
		previousExecutorUserId: UUID? = null,
		newExecutorUserId: UUID? = null,
		plannedStartDateBefore: LocalDate? = null,
		plannedStartDateAfter: LocalDate? = null,
		plannedFinishDateBefore: LocalDate? = null,
		plannedFinishDateAfter: LocalDate? = null,
		reason: String? = null,
		note: String? = null,
	): ProductionTaskHistoryEvent =
		ProductionTaskHistoryEvent(
			taskId = taskId,
			eventType = eventType,
			actorUserId = actorUserId,
			eventAt = Instant.parse(eventAt),
			fromStatus = fromStatus,
			toStatus = toStatus,
			previousExecutorUserId = previousExecutorUserId,
			newExecutorUserId = newExecutorUserId,
			plannedStartDateBefore = plannedStartDateBefore,
			plannedStartDateAfter = plannedStartDateAfter,
			plannedFinishDateBefore = plannedFinishDateBefore,
			plannedFinishDateAfter = plannedFinishDateAfter,
			reason = reason,
			note = note,
		)

	private fun useCase(events: List<ProductionTaskHistoryEvent>): ProductionTaskHistoryUseCase {
		val tracePort = object : ProductionTaskTracePort {
			override fun saveHistoryEvent(event: ProductionTaskHistoryEvent) = event
			override fun findHistoryEvents(taskId: UUID): List<ProductionTaskHistoryEvent> =
				events.filter { it.taskId == taskId }
		}
		val actorLookup = object : ProductionActorLookupPort {
			override fun displayName(userId: UUID): String? =
				when (userId) {
					managerId -> "Менеджер Анна"
					executorAId -> "Исполнитель А"
					executorBId -> "Исполнитель Б"
					else -> null
				}
		}
		return ProductionTaskHistoryUseCase(traces = tracePort, actorLookup = actorLookup)
	}
}
