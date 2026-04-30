package com.ctfind.productioncontrol.auth.adapter.web

import com.ctfind.productioncontrol.auth.application.AuthenticationAuditPort
import com.ctfind.productioncontrol.auth.application.AuthenticateUserUseCase
import com.ctfind.productioncontrol.auth.application.IssuedToken
import com.ctfind.productioncontrol.auth.application.TokenIssuer
import com.ctfind.productioncontrol.auth.application.UserAccountPort
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.UserAccount
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import kotlin.test.assertEquals

class AuthControllerMeTests {

	@Test
	fun `me returns authenticated user from bearer token claims`() {
		val controller = AuthController(unusedAuthenticateUserUseCase())
		val jwt = Jwt.withTokenValue("jwt-admin")
			.header("alg", "HS256")
			.subject("admin")
			.claim("roles", listOf("ADMIN"))
			.claim("displayName", "Local Administrator")
			.issuedAt(Instant.parse("2026-04-26T16:00:00Z"))
			.expiresAt(Instant.parse("2026-04-27T00:00:00Z"))
			.build()

		val response = controller.me(jwt)

		assertEquals("admin", response.login)
		assertEquals("Local Administrator", response.displayName)
		assertEquals(setOf("ADMIN"), response.roles)
		assertEquals(Instant.parse("2026-04-27T00:00:00Z"), response.expiresAt)
	}
}

private fun unusedAuthenticateUserUseCase(): AuthenticateUserUseCase =
	AuthenticateUserUseCase(
		userAccounts = object : UserAccountPort {
			override fun findByLogin(login: String): UserAccount? = null
			override fun save(user: UserAccount): UserAccount = user
			override fun existsEnabledWithRole(roleCode: String): Boolean = false
		},
		passwordEncoder = BCryptPasswordEncoder(),
		tokenIssuer = object : TokenIssuer {
			override fun issue(user: UserAccount): IssuedToken = error("not used")
		},
		audit = object : AuthenticationAuditPort {
			override fun record(event: AuthenticationAuditEvent): AuthenticationAuditEvent = event
		},
	)
