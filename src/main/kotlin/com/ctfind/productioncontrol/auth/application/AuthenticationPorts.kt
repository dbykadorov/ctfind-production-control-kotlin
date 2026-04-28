package com.ctfind.productioncontrol.auth.application

import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.Role
import com.ctfind.productioncontrol.auth.domain.UserAccount

interface UserAccountPort {
	fun findByLogin(login: String): UserAccount?
	fun save(user: UserAccount): UserAccount
}

interface RolePort {
	fun findByCode(code: String): Role?
	fun save(role: Role): Role
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

data class UserSummary(
	val id: java.util.UUID,
	val login: String,
	val displayName: String,
)
