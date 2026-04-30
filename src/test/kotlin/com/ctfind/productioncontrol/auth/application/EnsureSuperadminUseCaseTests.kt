package com.ctfind.productioncontrol.auth.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEventType
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditOutcome
import com.ctfind.productioncontrol.auth.domain.Role
import com.ctfind.productioncontrol.auth.domain.UserAccount
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EnsureSuperadminUseCaseTests {

	private val clock = Clock.fixed(Instant.parse("2026-04-30T12:00:00Z"), ZoneOffset.UTC)
	private val encoder = BCryptPasswordEncoder()

	@Test
	fun `seeds configured admin when no enabled ADMIN exists`() {
		val users = BootstrapInMemoryUsers()
		val roles = BootstrapInMemoryRoles()
		val audit = BootstrapInMemoryAudit()
		val useCase = EnsureSuperadminUseCase(users, roles, audit, encoder, clock)

		val result = useCase.ensureConfiguredSuperadmin(
			login = "admin.ops",
			displayName = "Ops Admin",
			secret = "secret",
		)

		assertIs<EnsureSuperadminResult.Seeded>(result)
		val saved = users.findByLogin("admin.ops")
		assertTrue(saved != null && saved.enabled)
		assertTrue(saved!!.roleCodes.contains(ADMIN_ROLE_CODE))
		assertTrue(encoder.matches("secret", saved.passwordHash))
		assertEquals(AuthenticationAuditEventType.SUPERADMIN_BOOTSTRAP, audit.events.last().eventType)
		assertEquals(AuthenticationAuditOutcome.SEEDED, audit.events.last().outcome)
	}

	@Test
	fun `fails with missing credentials when admin does not exist`() {
		val useCase = EnsureSuperadminUseCase(
			users = BootstrapInMemoryUsers(),
			roles = BootstrapInMemoryRoles(),
			audit = BootstrapInMemoryAudit(),
			passwordEncoder = encoder,
			clock = clock,
		)

		val result = useCase.ensureConfiguredSuperadmin(
			login = "",
			displayName = "Ops Admin",
			secret = "",
		)

		val failed = assertIs<EnsureSuperadminResult.FailedMissingCredentials>(result)
		assertTrue(failed.message.contains("Missing required superadmin"))
	}

	@Test
	fun `skips when enabled admin already exists`() {
		val users = BootstrapInMemoryUsers().apply {
			save(
				UserAccount(
					id = UUID.randomUUID(),
					login = "admin",
					displayName = "Administrator",
					passwordHash = "hash",
					enabled = true,
					roleCodes = setOf(ADMIN_ROLE_CODE),
					createdAt = Instant.now(clock),
					updatedAt = Instant.now(clock),
				),
			)
		}
		val audit = BootstrapInMemoryAudit()
		val useCase = EnsureSuperadminUseCase(
			users = users,
			roles = BootstrapInMemoryRoles(),
			audit = audit,
			passwordEncoder = encoder,
			clock = clock,
		)

		val result = useCase.ensureConfiguredSuperadmin(
			login = "admin.ops",
			displayName = "Ops Admin",
			secret = "secret",
		)

		assertIs<EnsureSuperadminResult.SkippedExistingAdmin>(result)
		assertEquals(AuthenticationAuditOutcome.SKIPPED_EXISTING, audit.events.last().outcome)
	}
}

private class BootstrapInMemoryUsers : UserAccountPort {
	private val users = linkedMapOf<String, UserAccount>()

	override fun findByLogin(login: String): UserAccount? = users[login.trim().lowercase()]

	override fun save(user: UserAccount): UserAccount {
		users[user.normalizedLogin] = user
		return user
	}

	override fun existsEnabledWithRole(roleCode: String): Boolean =
		users.values.any { it.enabled && roleCode.uppercase() in it.roleCodes }
}

private class BootstrapInMemoryRoles : RolePort {
	private val roles = linkedMapOf<String, Role>()

	override fun findByCode(code: String): Role? = roles[code.uppercase()]

	override fun save(role: Role): Role {
		roles[role.code.uppercase()] = role
		return role
	}

	override fun findAllByCodes(codes: Set<String>): List<Role> =
		codes.mapNotNull { roles[it.uppercase()] }
}

private class BootstrapInMemoryAudit : AuthenticationAuditPort {
	val events = mutableListOf<AuthenticationAuditEvent>()

	override fun record(event: AuthenticationAuditEvent): AuthenticationAuditEvent {
		events += event
		return event
	}
}
