package com.ctfind.productioncontrol.auth.application

import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEventType
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditOutcome
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class AuthenticationAuditService(
	private val audit: AuthenticationAuditPort,
	private val clock: Clock,
) {
	fun loginFailure(
		login: String?,
		outcome: AuthenticationAuditOutcome,
		userId: UUID?,
		requestIp: String?,
		userAgent: String?,
	): AuthenticationAuditEvent =
		audit.record(
			event(
				eventType = AuthenticationAuditEventType.LOGIN_FAILURE,
				outcome = outcome,
				login = login,
				userId = userId,
				requestIp = requestIp,
				userAgent = userAgent,
			),
		)

	fun logout(login: String, userId: UUID?, requestIp: String?, userAgent: String?): AuthenticationAuditEvent =
		audit.record(
			event(
				eventType = AuthenticationAuditEventType.LOGOUT,
				outcome = AuthenticationAuditOutcome.LOGGED_OUT,
				login = login,
				userId = userId,
				requestIp = requestIp,
				userAgent = userAgent,
			),
		)

	private fun event(
		eventType: AuthenticationAuditEventType,
		outcome: AuthenticationAuditOutcome,
		login: String?,
		userId: UUID?,
		requestIp: String?,
		userAgent: String?,
	): AuthenticationAuditEvent =
		AuthenticationAuditEvent(
			id = UUID.randomUUID(),
			eventType = eventType,
			outcome = outcome,
			login = login,
			userId = userId,
			requestIp = requestIp,
			userAgent = userAgent,
			occurredAt = Instant.now(clock),
			details = null,
		)
}
