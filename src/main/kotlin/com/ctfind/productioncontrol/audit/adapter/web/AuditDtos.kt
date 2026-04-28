package com.ctfind.productioncontrol.audit.adapter.web

import java.time.Instant
import java.util.UUID

data class AuditLogPageResponse(
    val items: List<AuditLogRowResponse>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
)

data class AuditLogRowResponse(
    val id: UUID,
    val occurredAt: Instant,
    val category: String,
    val eventType: String,
    val actorDisplayName: String,
    val actorLogin: String?,
    val summary: String,
    val targetType: String?,
    val targetId: UUID?,
)

data class AuditApiErrorResponse(
    val code: String,
    val message: String,
)
