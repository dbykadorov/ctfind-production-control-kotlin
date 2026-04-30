package com.ctfind.productioncontrol.auth.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEventType
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditOutcome
import com.ctfind.productioncontrol.auth.domain.Role
import com.ctfind.productioncontrol.auth.domain.UserAccount
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

enum class LocalAdminSeedResult {
	SEEDED,
	SKIPPED_EXISTING,
}

@Service
class LocalAdminSeedUseCase(
	private val users: UserAccountPort,
	private val roles: RolePort,
	private val userRoles: UserRolePort,
	private val audit: AuthenticationAuditPort,
	private val passwordEncoder: PasswordEncoder,
	private val clock: Clock,
) {
	// Local-only fallback bootstrap. Production-like environments use EnsureSuperadminUseCase
	// through SuperadminSeedRunner(!local) and do not rely on hardcoded credentials.

	@Transactional
	fun seedLocalAdmin(): LocalAdminSeedResult {
		val now = Instant.now(clock)
		ensureAdminRole(now)

		if (users.findByLogin(LOCAL_ADMIN_LOGIN) != null) {
			recordSeedAudit(AuthenticationAuditOutcome.SKIPPED_EXISTING, now)
			return LocalAdminSeedResult.SKIPPED_EXISTING
		}

		users.save(
			UserAccount(
				id = UUID.randomUUID(),
				login = LOCAL_ADMIN_LOGIN,
				displayName = LOCAL_ADMIN_DISPLAY_NAME,
				passwordHash = passwordEncoder.encode(LOCAL_ADMIN_PASSWORD)
					?: error("PasswordEncoder returned null hash"),
				enabled = true,
				roleCodes = setOf(ADMIN_ROLE_CODE),
				createdAt = now,
				updatedAt = now,
			),
		)
		userRoles.assignRole(LOCAL_ADMIN_LOGIN, ADMIN_ROLE_CODE)
		recordSeedAudit(AuthenticationAuditOutcome.SEEDED, now)
		return LocalAdminSeedResult.SEEDED
	}

	private fun ensureAdminRole(now: Instant): Role =
		roles.findByCode(ADMIN_ROLE_CODE)
			?: roles.save(
				Role(
					id = UUID.randomUUID(),
					code = ADMIN_ROLE_CODE,
					name = "Administrator",
					createdAt = now,
				),
			)

	private fun recordSeedAudit(outcome: AuthenticationAuditOutcome, occurredAt: Instant) {
		audit.record(
			AuthenticationAuditEvent(
				id = UUID.randomUUID(),
				eventType = AuthenticationAuditEventType.LOCAL_SEED,
				outcome = outcome,
				login = LOCAL_ADMIN_LOGIN,
				userId = null,
				requestIp = null,
				userAgent = null,
				occurredAt = occurredAt,
				details = null,
			),
		)
	}

	private companion object {
		const val LOCAL_ADMIN_LOGIN = "admin"
		const val LOCAL_ADMIN_PASSWORD = "admin"
		const val LOCAL_ADMIN_DISPLAY_NAME = "Local Administrator"
	}
}
