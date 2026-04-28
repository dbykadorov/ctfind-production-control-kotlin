package com.ctfind.productioncontrol.auth.adapter.persistence

import com.ctfind.productioncontrol.auth.application.AuthenticationAuditPort
import com.ctfind.productioncontrol.auth.application.RolePort
import com.ctfind.productioncontrol.auth.application.UserAccountPort
import com.ctfind.productioncontrol.auth.application.UserQueryPort
import com.ctfind.productioncontrol.auth.application.UserRolePort
import com.ctfind.productioncontrol.auth.application.UserSummary
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEventType
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditOutcome
import com.ctfind.productioncontrol.auth.domain.Role
import com.ctfind.productioncontrol.auth.domain.UserAccount
import com.ctfind.productioncontrol.auth.domain.normalizeLogin
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class JpaUserAccountAdapter(
	private val userRepository: UserAccountJpaRepository,
	private val roleRepository: RoleJpaRepository,
) : UserAccountPort, UserRolePort {

	override fun findByLogin(login: String): UserAccount? =
		userRepository.findByLogin(normalizeLogin(login))?.toDomain()

	override fun save(user: UserAccount): UserAccount {
		val entity = userRepository.findByLogin(user.normalizedLogin) ?: UserAccountEntity(id = user.id)
		entity.login = user.normalizedLogin
		entity.displayName = user.displayName
		entity.passwordHash = user.passwordHash
		entity.enabled = user.enabled
		entity.createdAt = user.createdAt
		entity.updatedAt = user.updatedAt
		entity.roles = user.roleCodes
			.mapNotNull(roleRepository::findByCode)
			.toMutableSet()
		return userRepository.save(entity).toDomain()
	}

	@Transactional
	override fun assignRole(login: String, roleCode: String) {
		val user = userRepository.findByLogin(normalizeLogin(login)) ?: return
		val role = roleRepository.findByCode(roleCode) ?: return
		user.roles.add(role)
		userRepository.save(user)
	}
}

@Component
class JpaRoleAdapter(
	private val roleRepository: RoleJpaRepository,
) : RolePort {

	override fun findByCode(code: String): Role? =
		roleRepository.findByCode(code.uppercase())?.toDomain()

	override fun save(role: Role): Role {
		val entity = roleRepository.findByCode(role.code) ?: RoleEntity(id = role.id)
		entity.code = role.code
		entity.name = role.name
		entity.createdAt = role.createdAt
		return roleRepository.save(entity).toDomain()
	}
}

@Component
class JpaUserQueryAdapter(
	private val entityManager: EntityManager,
) : UserQueryPort {

	override fun searchUsers(search: String?, limit: Int): List<UserSummary> {
		val hasSearch = !search.isNullOrBlank()
		val sql = buildString {
			append("SELECT id, login, display_name FROM app_user")
			if (hasSearch) {
				append(" WHERE LOWER(login) LIKE :pattern OR LOWER(display_name) LIKE :pattern")
			}
			append(" ORDER BY display_name")
		}
		val query = entityManager.createNativeQuery(sql, Array<Any>::class.java)
		if (hasSearch) {
			query.setParameter("pattern", "%${search!!.trim().lowercase()}%")
		}
		query.maxResults = limit
		@Suppress("UNCHECKED_CAST")
		val rows = query.resultList as List<Array<Any>>
		return rows.map { row ->
			UserSummary(
				id = row[0] as java.util.UUID,
				login = row[1] as String,
				displayName = row[2] as String,
			)
		}
	}
}

@Component
class JpaAuthenticationAuditAdapter(
	private val auditRepository: AuthenticationAuditEventJpaRepository,
) : AuthenticationAuditPort {

	override fun record(event: AuthenticationAuditEvent): AuthenticationAuditEvent =
		auditRepository.save(event.toEntity()).toDomain()
}

private fun UserAccountEntity.toDomain(): UserAccount =
	UserAccount(
		id = id,
		login = login,
		displayName = displayName,
		passwordHash = passwordHash,
		enabled = enabled,
		roleCodes = roles.map { it.code }.toSet(),
		createdAt = createdAt,
		updatedAt = updatedAt,
	)

private fun RoleEntity.toDomain(): Role =
	Role(
		id = id,
		code = code,
		name = name,
		createdAt = createdAt,
	)

private fun AuthenticationAuditEvent.toEntity(): AuthenticationAuditEventEntity =
	AuthenticationAuditEventEntity(
		id = id,
		eventType = eventType.name,
		outcome = outcome.name,
		login = login,
		userId = userId,
		requestIp = requestIp,
		userAgent = userAgent,
		occurredAt = occurredAt,
		details = details,
	)

private fun AuthenticationAuditEventEntity.toDomain(): AuthenticationAuditEvent =
	AuthenticationAuditEvent(
		id = id,
		eventType = AuthenticationAuditEventType.valueOf(eventType),
		outcome = AuthenticationAuditOutcome.valueOf(outcome),
		login = login,
		userId = userId,
		requestIp = requestIp,
		userAgent = userAgent,
		occurredAt = occurredAt,
		details = details,
	)
