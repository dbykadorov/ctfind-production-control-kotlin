package com.ctfind.productioncontrol.auth.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserAccountJpaRepository : JpaRepository<UserAccountEntity, UUID> {
	fun findByLogin(login: String): UserAccountEntity?
	fun existsByEnabledTrueAndRoles_Code(code: String): Boolean
	fun countByEnabledTrueAndRoles_Code(code: String): Long
}

interface RoleJpaRepository : JpaRepository<RoleEntity, UUID> {
	fun findByCode(code: String): RoleEntity?
	fun findAllByCodeIn(codes: Set<String>): List<RoleEntity>
}

interface AuthenticationAuditEventJpaRepository : JpaRepository<AuthenticationAuditEventEntity, UUID>
