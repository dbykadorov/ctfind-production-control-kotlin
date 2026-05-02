package com.ctfind.productioncontrol.audit.adapter.web

import com.ctfind.productioncontrol.audit.application.AuditLogQuery
import com.ctfind.productioncontrol.audit.application.AuditLogQueryResult
import com.ctfind.productioncontrol.audit.application.AuditLogQueryUseCase
import com.ctfind.productioncontrol.audit.domain.AuditCategory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@RestController
@RequestMapping("/api/audit")
class AuditController(
    private val useCase: AuditLogQueryUseCase,
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) from: Instant? = null,
        @RequestParam(required = false) to: Instant? = null,
        @RequestParam(required = false) category: List<String>? = null,
        @RequestParam(required = false) actorUserId: UUID? = null,
        @RequestParam(required = false) search: String? = null,
        @RequestParam(required = false, defaultValue = "0") page: Int = 0,
        @RequestParam(required = false, defaultValue = "50") size: Int = 50,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<*> {
        val roleCodes = jwtRoles(jwt)
        val now = Instant.now()
        val query = AuditLogQuery(
            from = from ?: now.minus(7, ChronoUnit.DAYS),
            to = to ?: now,
            categories = category?.mapNotNull { runCatching { AuditCategory.valueOf(it) }.getOrNull() }?.toSet()?.ifEmpty { null },
            actorUserId = actorUserId,
            search = search?.trim()?.ifBlank { null },
            page = page.coerceAtLeast(0),
            size = size.coerceIn(1, 100),
        )
        return when (val result = useCase.list(query, roleCodes)) {
            is AuditLogQueryResult.Success -> ResponseEntity.ok(result.page.toResponse())
            is AuditLogQueryResult.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(AuditApiErrorResponse("forbidden", "Access denied"))
        }
    }

    private fun jwtRoles(jwt: Jwt): Set<String> {
        val roles = jwt.claims["roles"]
        return when (roles) {
            is Collection<*> -> roles.filterIsInstance<String>().toSet()
            else -> emptySet()
        }
    }

    private fun com.ctfind.productioncontrol.audit.application.AuditLogPageResult.toResponse() =
        AuditLogPageResponse(
            items = items.map { it.toResponse() },
            page = page,
            size = size,
            totalItems = totalItems,
            totalPages = totalPages,
        )

    private fun com.ctfind.productioncontrol.audit.domain.AuditLogRow.toResponse() =
        AuditLogRowResponse(
            id = id,
            occurredAt = occurredAt,
            category = category.name,
            eventType = eventType,
            actorDisplayName = actorDisplayName,
            actorLogin = actorLogin,
            summary = summary,
            targetType = targetType,
            targetId = targetId,
        )
}
