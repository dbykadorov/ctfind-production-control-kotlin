package com.ctfind.productioncontrol.auth.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEventType
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditOutcome
import com.ctfind.productioncontrol.auth.domain.UserAccount
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

data class UpdateUserCommand(
	val userId: UUID,
	val displayName: String,
	val roleCodes: Set<String>,
	val actorRoleCodes: Set<String>,
	val actorLogin: String?,
	val actorUserId: UUID?,
)

sealed interface UpdateUserResult {
	data class Success(val user: UserSummary) : UpdateUserResult
	data object Forbidden : UpdateUserResult
	data object NotFound : UpdateUserResult
	data class ValidationError(val message: String, val field: String?) : UpdateUserResult
	data class InvalidRoles(val roleCodes: Set<String>) : UpdateUserResult
	data object LastAdminRoleRemovalForbidden : UpdateUserResult
}

@Service
open class UpdateUserUseCase(
	private val users: UserAccountPort,
	private val roles: RolePort,
	private val audit: AuthenticationAuditPort,
	private val clock: Clock,
) {

	@Transactional
	open fun update(command: UpdateUserCommand): UpdateUserResult {
		if (ADMIN_ROLE_CODE !in command.actorRoleCodes)
			return UpdateUserResult.Forbidden

		val target = users.findById(command.userId)
			?: return UpdateUserResult.NotFound

		val displayName = command.displayName.trim()
		if (displayName.isBlank())
			return UpdateUserResult.ValidationError(message = "displayName is required", field = "displayName")

		val normalizedRoleCodes = command.roleCodes
			.map { it.trim().uppercase() }
			.filter { it.isNotBlank() }
			.toSet()
		if (normalizedRoleCodes.isEmpty())
			return UpdateUserResult.ValidationError(message = "at least one role is required", field = "roleCodes")

		val unsupportedRoles = normalizedRoleCodes - SUPPORTED_ROLE_CODES
		if (unsupportedRoles.isNotEmpty())
			return UpdateUserResult.InvalidRoles(unsupportedRoles)

		val existingRoles = roles.findAllByCodes(normalizedRoleCodes).associateBy { it.code }
		val missingRoles = normalizedRoleCodes - existingRoles.keys
		if (missingRoles.isNotEmpty())
			return UpdateUserResult.InvalidRoles(missingRoles)

		if (shouldRejectLastAdminRoleRemoval(target, normalizedRoleCodes))
			return UpdateUserResult.LastAdminRoleRemovalForbidden

		val now = Instant.now(clock)
		val updated = users.save(
			target.copy(
				displayName = displayName,
				roleCodes = normalizedRoleCodes,
				updatedAt = now,
			),
		)

		recordUpdatedAudit(command, before = target, after = updated, roleCodes = normalizedRoleCodes, occurredAt = now)

		val summaryRoles = normalizedRoleCodes
			.mapNotNull { code -> existingRoles[code]?.let { RoleSummary(code = it.code, name = it.name) } }
			.sortedBy { it.code }

		return UpdateUserResult.Success(
			UserSummary(
				id = updated.id,
				login = updated.login,
				displayName = updated.displayName,
				roles = summaryRoles,
			),
		)
	}

	private fun shouldRejectLastAdminRoleRemoval(target: UserAccount, nextRoleCodes: Set<String>): Boolean {
		val isActiveAdmin = target.enabled && ADMIN_ROLE_CODE in target.roleCodes
		if (!isActiveAdmin)
			return false
		if (ADMIN_ROLE_CODE in nextRoleCodes)
			return false
		return users.countEnabledWithRole(ADMIN_ROLE_CODE) <= 1
	}

	private fun recordUpdatedAudit(
		command: UpdateUserCommand,
		before: UserAccount,
		after: UserAccount,
		roleCodes: Set<String>,
		occurredAt: Instant,
	) {
		val actorUserId = command.actorLogin
			?.let { users.findByLogin(it)?.id }

		val addedRoles = roleCodes - before.roleCodes
		val removedRoles = before.roleCodes - roleCodes
		val displayNameChanged = before.displayName != after.displayName

		val details = buildString {
			append("target_login=")
			append(after.login)
			append(";display_name_changed=")
			append(displayNameChanged)
			if (displayNameChanged) {
				append(";display_name_from=")
				append(before.displayName)
				append(";display_name_to=")
				append(after.displayName)
			}
			append(";roles_added=")
			append(addedRoles.sorted().joinToString(","))
			append(";roles_removed=")
			append(removedRoles.sorted().joinToString(","))
		}

		audit.record(
			AuthenticationAuditEvent(
				id = UUID.randomUUID(),
				eventType = AuthenticationAuditEventType.USER_UPDATED,
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
