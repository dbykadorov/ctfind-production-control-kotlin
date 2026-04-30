package com.ctfind.productioncontrol.auth.application

import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.Role
import com.ctfind.productioncontrol.auth.domain.UserAccount

interface UserAccountPort {
	fun findByLogin(login: String): UserAccount?
	fun findById(id: java.util.UUID): UserAccount? = null
	fun save(user: UserAccount): UserAccount
	fun existsEnabledWithRole(roleCode: String): Boolean
	fun countEnabledWithRole(roleCode: String): Long =
		if (existsEnabledWithRole(roleCode)) 1 else 0
}

interface RolePort {
	fun findByCode(code: String): Role?
	fun save(role: Role): Role
	fun findAllByCodes(codes: Set<String>): List<Role>
}

interface UserRolePort {
	fun assignRole(login: String, roleCode: String)
}

interface AuthenticationAuditPort {
	fun record(event: AuthenticationAuditEvent): AuthenticationAuditEvent
}

interface TokenIssuer {
	fun issue(user: UserAccount): IssuedToken
}

interface UserQueryPort {
	fun searchUsers(search: String?, limit: Int): List<UserSummary>
}

interface RoleCatalogPort {
	fun listRoles(codes: Set<String>): List<RoleSummary>
}

data class RoleSummary(
	val code: String,
	val name: String,
)

data class UserSummary(
	val id: java.util.UUID,
	val login: String,
	val displayName: String,
	val roles: List<RoleSummary>,
)
