package com.ctfind.productioncontrol.auth.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserAccountJpaRepository : JpaRepository<UserAccountEntity, UUID> {
	fun findByLogin(login: String): UserAccountEntity?
}

interface RoleJpaRepository : JpaRepository<RoleEntity, UUID> {
	fun findByCode(code: String): RoleEntity?
}

interface AuthenticationAuditEventJpaRepository : JpaRepository<AuthenticationAuditEventEntity, UUID>
