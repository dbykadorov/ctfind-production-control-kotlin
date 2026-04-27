package com.ctfind.productioncontrol.production.adapter.persistence

import com.ctfind.productioncontrol.auth.adapter.persistence.RoleEntity
import com.ctfind.productioncontrol.auth.adapter.persistence.RoleJpaRepository
import com.ctfind.productioncontrol.auth.adapter.persistence.UserAccountEntity
import com.ctfind.productioncontrol.auth.adapter.persistence.UserAccountJpaRepository
import com.ctfind.productioncontrol.production.application.PRODUCTION_EXECUTOR_ROLE_CODE
import com.ctfind.productioncontrol.production.application.PRODUCTION_SUPERVISOR_ROLE_CODE
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
@Profile("local")
class LocalProductionSeedRunner(
	private val roleRepository: RoleJpaRepository,
	private val userRepository: UserAccountJpaRepository,
	private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {

	@Transactional
	override fun run(args: ApplicationArguments) {
		val now = Instant.parse("2026-04-27T12:00:00Z")
		val supervisorRole = ensureRole(PRODUCTION_SUPERVISOR_ROLE_CODE, "Production Supervisor", now)
		val executorRole = ensureRole(PRODUCTION_EXECUTOR_ROLE_CODE, "Production Executor", now)

		ensureUser("production.supervisor", "Production Supervisor", "supervisor", supervisorRole, now)
		ensureUser("production.executor", "Production Executor", "executor", executorRole, now)
	}

	private fun ensureRole(code: String, name: String, now: Instant): RoleEntity =
		roleRepository.findByCode(code)
			?: roleRepository.save(
				RoleEntity(
					id = UUID.randomUUID(),
					code = code,
					name = name,
					createdAt = now,
				),
			)

	private fun ensureUser(
		login: String,
		displayName: String,
		password: String,
		role: RoleEntity,
		now: Instant,
	) {
		val user = userRepository.findByLogin(login)
			?: userRepository.save(
				UserAccountEntity(
					id = UUID.randomUUID(),
					login = login,
					displayName = displayName,
					passwordHash = passwordEncoder.encode(password) ?: error("PasswordEncoder returned null hash"),
					enabled = true,
					createdAt = now,
					updatedAt = now,
					roles = mutableSetOf(role),
				),
			)
		if (user.roles.none { it.code == role.code }) {
			user.roles.add(role)
			userRepository.save(user)
		}
	}
}
