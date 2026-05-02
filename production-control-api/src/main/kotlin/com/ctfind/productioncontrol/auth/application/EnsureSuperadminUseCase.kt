package com.ctfind.productioncontrol.auth.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEventType
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditOutcome
import com.ctfind.productioncontrol.auth.domain.Role
import com.ctfind.productioncontrol.auth.domain.UserAccount
import com.ctfind.productioncontrol.auth.domain.normalizeLogin
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

sealed interface EnsureSuperadminResult {
	data object Seeded : EnsureSuperadminResult
	data object SkippedExistingAdmin : EnsureSuperadminResult
	data class FailedMissingCredentials(val message: String) : EnsureSuperadminResult
}

@Service
open class EnsureSuperadminUseCase(
	private val users: UserAccountPort,
	private val roles: RolePort,
	private val audit: AuthenticationAuditPort,
	private val passwordEncoder: PasswordEncoder,
	private val clock: Clock,
) {

	@Transactional
	open fun ensureConfiguredSuperadmin(
		login: String?,
		displayName: String?,
		secret: String?,
	): EnsureSuperadminResult {
		val now = Instant.now(clock)
		ensureAdminRole(now)

		if (users.existsEnabledWithRole(ADMIN_ROLE_CODE)) {
			record(
				outcome = AuthenticationAuditOutcome.SKIPPED_EXISTING,
				login = login?.trim(),
				occurredAt = now,
				details = "superadmin bootstrap skipped: admin already exists",
			)
			return EnsureSuperadminResult.SkippedExistingAdmin
		}

		val normalizedLogin = normalizeLogin(login ?: "")
		val normalizedDisplayName = displayName?.trim().orEmpty()
		if (normalizedLogin.isBlank() || normalizedDisplayName.isBlank() || secret.isNullOrEmpty()) {
			record(
				outcome = AuthenticationAuditOutcome.OPERATIONAL_FAILURE,
				login = login?.trim(),
				occurredAt = now,
				details = "superadmin bootstrap config incomplete",
			)
			return EnsureSuperadminResult.FailedMissingCredentials(
				message = "Missing required superadmin bootstrap configuration",
			)
		}

		users.save(
			UserAccount(
				id = UUID.randomUUID(),
				login = normalizedLogin,
				displayName = normalizedDisplayName,
				passwordHash = passwordEncoder.encode(secret)
					?: error("PasswordEncoder returned null hash"),
				enabled = true,
				roleCodes = setOf(ADMIN_ROLE_CODE),
				createdAt = now,
				updatedAt = now,
			),
		)
		record(
			outcome = AuthenticationAuditOutcome.SEEDED,
			login = normalizedLogin,
			occurredAt = now,
			details = "superadmin bootstrap seeded configured admin",
		)
		return EnsureSuperadminResult.Seeded
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

	private fun record(
		outcome: AuthenticationAuditOutcome,
		login: String?,
		occurredAt: Instant,
		details: String,
	) {
		audit.record(
			AuthenticationAuditEvent(
				id = UUID.randomUUID(),
				eventType = AuthenticationAuditEventType.SUPERADMIN_BOOTSTRAP,
				outcome = outcome,
				login = login,
				userId = null,
				requestIp = null,
				userAgent = null,
				occurredAt = occurredAt,
				details = details,
			),
		)
	}
}
