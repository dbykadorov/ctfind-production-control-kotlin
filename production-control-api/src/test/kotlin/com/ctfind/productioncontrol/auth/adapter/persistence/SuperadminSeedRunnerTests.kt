package com.ctfind.productioncontrol.auth.adapter.persistence

import com.ctfind.productioncontrol.auth.application.EnsureSuperadminResult
import com.ctfind.productioncontrol.auth.application.EnsureSuperadminUseCase
import com.ctfind.productioncontrol.auth.application.AuthenticationAuditPort
import com.ctfind.productioncontrol.auth.application.RolePort
import com.ctfind.productioncontrol.auth.application.UserAccountPort
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.Role
import com.ctfind.productioncontrol.auth.domain.UserAccount
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SuperadminSeedRunnerTests {

	@Test
	fun `runner delegates credentials to use case`() {
		var capturedLogin: String? = null
		var capturedDisplayName: String? = null
		var capturedSecret: String? = null
		val runner = SuperadminSeedRunner(
			useCase = object : EnsureSuperadminUseCase(
				users = dummyUsers(),
				roles = dummyRoles(),
				audit = dummyAudit(),
				passwordEncoder = BCryptPasswordEncoder(),
				clock = Clock.systemUTC(),
			) {
				override fun ensureConfiguredSuperadmin(login: String?, displayName: String?, secret: String?): EnsureSuperadminResult {
					capturedLogin = login
					capturedDisplayName = displayName
					capturedSecret = secret
					return EnsureSuperadminResult.Seeded
				}
			},
			login = "admin.ops",
			displayName = "Ops Admin",
			secret = "secret",
		)

		runner.run(args = org.springframework.boot.DefaultApplicationArguments(*emptyArray<String>()))

		assertEquals("admin.ops", capturedLogin)
		assertEquals("Ops Admin", capturedDisplayName)
		assertEquals("secret", capturedSecret)
	}

	@Test
	fun `runner throws when credentials are missing`() {
		val runner = SuperadminSeedRunner(
			useCase = object : EnsureSuperadminUseCase(
				users = dummyUsers(),
				roles = dummyRoles(),
				audit = dummyAudit(),
				passwordEncoder = BCryptPasswordEncoder(),
				clock = Clock.systemUTC(),
			) {
				override fun ensureConfiguredSuperadmin(login: String?, displayName: String?, secret: String?): EnsureSuperadminResult =
					EnsureSuperadminResult.FailedMissingCredentials("Missing required superadmin bootstrap configuration")
			},
			login = "",
			displayName = "",
			secret = "",
		)

		val ex = assertFailsWith<IllegalStateException> {
			runner.run(args = org.springframework.boot.DefaultApplicationArguments(*emptyArray<String>()))
		}
		assertEquals(
			"Missing required superadmin bootstrap configuration. Set APP_SUPERADMIN_LOGIN, APP_SUPERADMIN_DISPLAY_NAME and APP_SUPERADMIN_PASSWORD.",
			ex.message,
		)
	}

	private fun dummyUsers(): UserAccountPort = object : UserAccountPort {
		override fun findByLogin(login: String): UserAccount? = null
		override fun save(user: UserAccount): UserAccount = user
		override fun existsEnabledWithRole(roleCode: String): Boolean = false
	}

	private fun dummyRoles(): RolePort = object : RolePort {
		override fun findByCode(code: String): Role? = null
		override fun save(role: Role): Role = role
		override fun findAllByCodes(codes: Set<String>): List<Role> = emptyList()
	}

	private fun dummyAudit(): AuthenticationAuditPort = object : AuthenticationAuditPort {
		override fun record(event: AuthenticationAuditEvent): AuthenticationAuditEvent = event
	}
}
