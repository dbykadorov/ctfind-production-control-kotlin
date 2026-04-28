package com.ctfind.productioncontrol.audit.adapter.web

import com.ctfind.productioncontrol.audit.application.AuditLogPageResult
import com.ctfind.productioncontrol.audit.application.AuditLogQuery
import com.ctfind.productioncontrol.audit.application.AuditLogQueryPort
import com.ctfind.productioncontrol.audit.application.AuditLogQueryResult
import com.ctfind.productioncontrol.audit.application.AuditLogQueryUseCase
import com.ctfind.productioncontrol.audit.domain.AuditCategory
import com.ctfind.productioncontrol.audit.domain.AuditLogRow
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.util.UUID

internal fun jwtFor(actorId: UUID, roles: Set<String>): Jwt =
	Jwt.withTokenValue("tok")
		.header("alg", "none")
		.subject("user1")
		.claim("userId", actorId.toString())
		.claim("displayName", "Tester")
		.claim("roles", roles.toList())
		.build()

internal fun unusedQueryPort(): AuditLogQueryPort = object : AuditLogQueryPort {
	override fun search(query: AuditLogQuery): AuditLogPageResult =
		AuditLogPageResult(emptyList(), 0, 50, 0)
}

internal fun stubAuditQueryUseCase(
	exec: (AuditLogQuery, Set<String>) -> AuditLogQueryResult,
): AuditLogQueryUseCase = object : AuditLogQueryUseCase(
	port = unusedQueryPort(),
) {
	override fun list(query: AuditLogQuery, roleCodes: Set<String>): AuditLogQueryResult =
		exec(query, roleCodes)
}

internal fun sampleAuditLogRow(
	id: UUID = UUID.fromString("10000000-0000-0000-0000-000000000001"),
	occurredAt: Instant = Instant.parse("2026-04-28T10:00:00Z"),
	category: AuditCategory = AuditCategory.AUTH,
	eventType: String = "LOGIN_SUCCESS",
	actorUserId: UUID? = UUID.fromString("20000000-0000-0000-0000-000000000002"),
	actorDisplayName: String = "Admin User",
	actorLogin: String? = "admin",
	summary: String = "Вход в систему: admin",
	targetType: String? = null,
	targetId: UUID? = null,
): AuditLogRow = AuditLogRow(
	id = id,
	occurredAt = occurredAt,
	category = category,
	eventType = eventType,
	actorUserId = actorUserId,
	actorDisplayName = actorDisplayName,
	actorLogin = actorLogin,
	summary = summary,
	targetType = targetType,
	targetId = targetId,
)

internal fun sampleAuditLogPage(
	items: List<AuditLogRow> = listOf(sampleAuditLogRow()),
	page: Int = 0,
	size: Int = 50,
	totalItems: Long = items.size.toLong(),
): AuditLogPageResult = AuditLogPageResult(
	items = items,
	page = page,
	size = size,
	totalItems = totalItems,
)
