package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.production.domain.ProductionTaskAuditEvent
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class ProductionTaskServicesTests {

	@Test
	fun `number service delegates allocation to number port`() {
		val service = ProductionTaskNumberService(FakeNumberPort("PT-000123"))

		assertEquals("PT-000123", service.nextTaskNumber())
	}

	@Test
	fun `audit service records production task event with explicit timestamp`() {
		val auditPort = CapturingAuditPort()
		val actorUserId = UUID.randomUUID()
		val taskId = UUID.randomUUID()
		val eventAt = Instant.parse("2026-04-27T12:34:56Z")
		val service = ProductionTaskAuditService(auditPort)

		val recorded = service.record(
			eventType = "PRODUCTION_TASK_CREATED",
			actorUserId = actorUserId,
			taskId = taskId,
			summary = "Created production task PT-000123",
			metadata = """{"taskNumber":"PT-000123"}""",
			eventAt = eventAt,
		)

		assertEquals(recorded, auditPort.recorded.single())
		assertEquals("PRODUCTION_TASK_CREATED", recorded.eventType)
		assertEquals(actorUserId, recorded.actorUserId)
		assertEquals("PRODUCTION_TASK", recorded.targetType)
		assertEquals(taskId, recorded.targetId)
		assertEquals(eventAt, recorded.eventAt)
		assertEquals("Created production task PT-000123", recorded.summary)
		assertEquals("""{"taskNumber":"PT-000123"}""", recorded.metadata)
	}
}

private class FakeNumberPort(
	private val next: String,
) : ProductionTaskNumberPort {
	override fun nextTaskNumber(): String = next
}

private class CapturingAuditPort : ProductionTaskAuditPort {
	val recorded = mutableListOf<ProductionTaskAuditEvent>()

	override fun record(event: ProductionTaskAuditEvent): ProductionTaskAuditEvent {
		recorded += event
		return event
	}
}
