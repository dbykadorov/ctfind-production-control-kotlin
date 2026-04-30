package com.ctfind.productioncontrol.auth.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEventType
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditOutcome
import com.ctfind.productioncontrol.auth.domain.UserAccount
import com.ctfind.productioncontrol.auth.domain.normalizeLogin
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

data class CreateUserCommand(
	val login: String,
	val displayName: String,
	val initialPassword: String,
	val roleCodes: Set<String>,
	val actorRoleCodes: Set<String>,
	val actorLogin: String?,
	val actorUserId: UUID?,
)

sealed interface CreateUserResult {
	data class Success(val user: UserSummary) : CreateUserResult
	data object Forbidden : CreateUserResult
	data class ValidationError(val message: String, val field: String?) : CreateUserResult
	data class DuplicateLogin(val login: String) : CreateUserResult
	data class InvalidRoles(val roleCodes: Set<String>) : CreateUserResult
}

@Service
open class CreateUserUseCase(
	private val users: UserAccountPort,
	private val roles: RolePort,
	private val audit: AuthenticationAuditPort,
	private val passwordEncoder: PasswordEncoder,
	private val clock: Clock,
) {

	@Transactional
	open fun create(command: CreateUserCommand): CreateUserResult {
		if (ADMIN_ROLE_CODE !in command.actorRoleCodes)
			return CreateUserResult.Forbidden

		val normalizedLogin = normalizeLogin(command.login)
		if (normalizedLogin.isBlank())
			return CreateUserResult.ValidationError(message = "login is required", field = "login")

		val displayName = command.displayName.trim()
		if (displayName.isBlank())
			return CreateUserResult.ValidationError(message = "displayName is required", field = "displayName")

		if (command.initialPassword.isEmpty())
			return CreateUserResult.ValidationError(message = "initialPassword must not be empty", field = "initialPassword")

		val normalizedRoleCodes = command.roleCodes
			.map { it.trim().uppercase() }
			.filter { it.isNotBlank() }
			.toSet()
		if (normalizedRoleCodes.isEmpty())
			return CreateUserResult.ValidationError(message = "at least one role is required", field = "roleCodes")

		val unsupportedRoles = normalizedRoleCodes - SUPPORTED_ROLE_CODES
		if (unsupportedRoles.isNotEmpty())
			return CreateUserResult.InvalidRoles(unsupportedRoles)

		val existingRoles = roles.findAllByCodes(normalizedRoleCodes).associateBy { it.code }
		val missingRoles = normalizedRoleCodes - existingRoles.keys
		if (missingRoles.isNotEmpty())
			return CreateUserResult.InvalidRoles(missingRoles)

		if (users.findByLogin(normalizedLogin) != null)
			return CreateUserResult.DuplicateLogin(normalizedLogin)

		val now = Instant.now(clock)
		val saved = users.save(
			UserAccount(
				id = UUID.randomUUID(),
				login = normalizedLogin,
				displayName = displayName,
				passwordHash = passwordEncoder.encode(command.initialPassword)
					?: error("PasswordEncoder returned null hash"),
				enabled = true,
				roleCodes = normalizedRoleCodes,
				createdAt = now,
				updatedAt = now,
			),
		)

		recordCreatedAudit(command, saved, normalizedRoleCodes, now)

		val summaryRoles = normalizedRoleCodes
			.mapNotNull { code -> existingRoles[code]?.let { RoleSummary(code = it.code, name = it.name) } }
			.sortedBy { it.code }
		return CreateUserResult.Success(
			UserSummary(
				id = saved.id,
				login = saved.login,
				displayName = saved.displayName,
				roles = summaryRoles,
			),
		)
	}

	private fun recordCreatedAudit(
		command: CreateUserCommand,
		created: UserAccount,
		roleCodes: Set<String>,
		occurredAt: Instant,
	) {
		// Use persisted actor id resolved by login to avoid FK violations from stale JWT claims.
		val actorUserId = command.actorLogin
			?.let { users.findByLogin(it)?.id }
		val details = buildString {
			append("created_login=")
			append(created.login)
			append(";roles=")
			append(roleCodes.sorted().joinToString(","))
		}
		audit.record(
			AuthenticationAuditEvent(
				id = UUID.randomUUID(),
				eventType = AuthenticationAuditEventType.USER_CREATED,
				outcome = AuthenticationAuditOutcome.SUCCESS,
				login = command.actorLogin,
				userId = actorUserId,
				requestIp = null,
				userAgent = null,
				occurredAt = occurredAt,
				details = details,
			),
		)
	}
}
