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
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocalAdminSeedUseCaseTests {

	private val clock = Clock.fixed(Instant.parse("2026-04-26T16:00:00Z"), ZoneOffset.UTC)
	private val passwordEncoder = BCryptPasswordEncoder()

	@Test
	fun `seed creates local admin once and does not overwrite existing password`() {
		val users = InMemoryUsers()
		val roles = InMemoryRoles()
		val assignments = InMemoryAssignments(users, roles)
		val audits = InMemoryAudit()
		val useCase = LocalAdminSeedUseCase(users, roles, assignments, audits, passwordEncoder, clock)

		val first = useCase.seedLocalAdmin()
		val seeded = users.findByLogin("admin")

		assertEquals(LocalAdminSeedResult.SEEDED, first)
		assertNotNull(seeded)
		assertEquals("admin", seeded.login)
		assertEquals(setOf(ADMIN_ROLE_CODE), seeded.roleCodes)
		assertTrue(passwordEncoder.matches("admin", seeded.passwordHash))
		assertEquals(AuthenticationAuditOutcome.SEEDED, audits.events.single().outcome)

		val originalHash = seeded.passwordHash
		val second = useCase.seedLocalAdmin()

		assertEquals(LocalAdminSeedResult.SKIPPED_EXISTING, second)
		assertEquals(1, users.savedCount)
		assertEquals(originalHash, users.findByLogin("admin")!!.passwordHash)
		assertNotEquals("admin", originalHash)
		assertEquals(
			listOf(AuthenticationAuditOutcome.SEEDED, AuthenticationAuditOutcome.SKIPPED_EXISTING),
			audits.events.map { it.outcome },
		)
	}
}

private class InMemoryUsers : UserAccountPort {
	private val users = linkedMapOf<String, UserAccount>()
	var savedCount = 0

	override fun findByLogin(login: String): UserAccount? = users[login.trim().lowercase()]

	override fun save(user: UserAccount): UserAccount {
		savedCount += 1
		users[user.normalizedLogin] = user
		return user
	}
}

private class InMemoryRoles : RolePort {
	private val roles = linkedMapOf<String, Role>()

	override fun findByCode(code: String): Role? = roles[code.uppercase()]

	override fun save(role: Role): Role {
		roles[role.code] = role
		return role
	}
}

private class InMemoryAssignments(
	private val users: InMemoryUsers,
	private val roles: InMemoryRoles,
) : UserRolePort {
	override fun assignRole(login: String, roleCode: String) {
		val user = users.findByLogin(login) ?: return
		roles.findByCode(roleCode) ?: return
		users.save(user.copy(roleCodes = user.roleCodes + roleCode))
		users.savedCount -= 1
	}
}

private class InMemoryAudit : AuthenticationAuditPort {
	val events = mutableListOf<AuthenticationAuditEvent>()

	override fun record(event: AuthenticationAuditEvent): AuthenticationAuditEvent {
		assertEquals(AuthenticationAuditEventType.LOCAL_SEED, event.eventType)
		events += event
		return event
	}
}
