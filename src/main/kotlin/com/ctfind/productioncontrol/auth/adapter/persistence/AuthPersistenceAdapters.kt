package com.ctfind.productioncontrol.auth.adapter.persistence

import com.ctfind.productioncontrol.auth.application.AuthenticationAuditPort
import com.ctfind.productioncontrol.auth.application.RoleCatalogPort
import com.ctfind.productioncontrol.auth.application.RolePort
import com.ctfind.productioncontrol.auth.application.RoleSummary
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

	override fun existsEnabledWithRole(roleCode: String): Boolean =
		userRepository.existsByEnabledTrueAndRoles_Code(roleCode.uppercase())

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

	override fun findAllByCodes(codes: Set<String>): List<Role> {
		if (codes.isEmpty())
			return emptyList()
		return roleRepository.findAllByCodeIn(codes.map { it.uppercase() }.toSet())
			.map { it.toDomain() }
	}
}

@Component
class JpaRoleCatalogAdapter(
	private val roleRepository: RoleJpaRepository,
) : RoleCatalogPort {
	override fun listRoles(codes: Set<String>): List<RoleSummary> {
		if (codes.isEmpty())
			return emptyList()
		return roleRepository.findAllByCodeIn(codes.map { it.uppercase() }.toSet())
			.map { RoleSummary(code = it.code, name = it.name) }
	}
}

@Component
class JpaUserQueryAdapter(
	private val entityManager: EntityManager,
) : UserQueryPort {

	override fun searchUsers(search: String?, limit: Int): List<UserSummary> {
		val normalizedSearch = search?.trim()?.lowercase()
		val hasSearch = !normalizedSearch.isNullOrEmpty()
		val jpql = buildString {
			append("SELECT u FROM UserAccountEntity u")
			if (hasSearch)
				append(" WHERE LOWER(u.login) LIKE :pattern OR LOWER(u.displayName) LIKE :pattern")
			append(" ORDER BY u.displayName")
		}
		val query = entityManager.createQuery(jpql, UserAccountEntity::class.java)
		if (hasSearch)
			query.setParameter("pattern", "%$normalizedSearch%")
		query.maxResults = limit
		val rows = query.resultList
		return rows.map { row ->
			UserSummary(
				id = row.id,
				login = row.login,
				displayName = row.displayName,
				roles = row.roles
					.map { RoleSummary(code = it.code, name = it.name) }
					.sortedBy { it.code },
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
