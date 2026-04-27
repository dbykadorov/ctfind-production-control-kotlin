package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.production.domain.ProductionTaskAuditEvent
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class ProductionTaskAuditService(
	private val audit: ProductionTaskAuditPort,
) {
	fun record(
		eventType: String,
		actorUserId: UUID,
		taskId: UUID,
		summary: String,
		metadata: String? = null,
		eventAt: Instant = Instant.now(),
	): ProductionTaskAuditEvent =
		audit.record(
			ProductionTaskAuditEvent(
				eventType = eventType,
				actorUserId = actorUserId,
				targetId = taskId,
				eventAt = eventAt,
				summary = summary,
				metadata = metadata,
			),
		)
}
