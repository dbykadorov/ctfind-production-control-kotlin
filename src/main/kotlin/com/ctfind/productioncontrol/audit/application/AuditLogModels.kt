package com.ctfind.productioncontrol.audit.application

import com.ctfind.productioncontrol.audit.domain.AuditCategory
import com.ctfind.productioncontrol.audit.domain.AuditLogRow
import java.time.Instant
import java.util.UUID

data class AuditLogQuery(
    val from: Instant,
    val to: Instant,
    val categories: Set<AuditCategory>? = null,
    val actorUserId: UUID? = null,
    val search: String? = null,
    val page: Int = 0,
    val size: Int = 50,
)

data class AuditLogPageResult(
    val items: List<AuditLogRow>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
) {
    val totalPages: Int get() = if (size > 0) ((totalItems + size - 1) / size).toInt() else 0
}
