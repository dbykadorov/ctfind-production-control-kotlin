package com.ctfind.productioncontrol.inventory.domain

import java.time.Instant
import java.util.UUID

data class InventoryAuditEvent(
    val id: UUID = UUID.randomUUID(),
    val eventType: String,
    val actorUserId: UUID,
    val targetId: UUID,
    val eventAt: Instant = Instant.now(),
    val summary: String,
    val metadata: String? = null,
)
