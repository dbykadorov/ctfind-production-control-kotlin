package com.ctfind.productioncontrol.auth.domain

import java.time.Instant
import java.util.UUID

enum class AuthenticationAuditEventType {
	LOGIN_SUCCESS,
	LOGIN_FAILURE,
	LOGOUT,
	LOCAL_SEED,
}

enum class AuthenticationAuditOutcome {
	SUCCESS,
	INVALID_CREDENTIALS,
	THROTTLED,
	DISABLED,
	SEEDED,
	SKIPPED_EXISTING,
	LOGGED_OUT,
	OPERATIONAL_FAILURE,
}

data class AuthenticationAuditEvent(
	val id: UUID,
	val eventType: AuthenticationAuditEventType,
	val outcome: AuthenticationAuditOutcome,
	val login: String?,
	val userId: UUID?,
	val requestIp: String?,
	val userAgent: String?,
	val occurredAt: Instant,
	val details: String?,
) {
	init {
		require(details?.contains("password", ignoreCase = true) != true) {
			"audit details must not contain password values"
		}
	}
}
