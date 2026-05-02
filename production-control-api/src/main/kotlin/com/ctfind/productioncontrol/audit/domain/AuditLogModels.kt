package com.ctfind.productioncontrol.audit.domain

import java.time.Instant
import java.util.UUID

enum class AuditCategory {
    AUTH, ORDER, PRODUCTION_TASK, INVENTORY,
}

data class AuditLogRow(
    val id: UUID,
    val occurredAt: Instant,
    val category: AuditCategory,
    val eventType: String,
    val actorUserId: UUID?,
    val actorDisplayName: String,
    val actorLogin: String?,
    val summary: String,
    val targetType: String?,
    val targetId: UUID?,
)
