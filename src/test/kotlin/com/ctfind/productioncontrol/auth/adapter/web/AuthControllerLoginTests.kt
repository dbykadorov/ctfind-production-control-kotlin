package com.ctfind.productioncontrol.auth.adapter.web

import com.ctfind.productioncontrol.auth.application.AuthenticationAuditPort
import com.ctfind.productioncontrol.auth.application.AuthenticateUserUseCase
import com.ctfind.productioncontrol.auth.application.IssuedToken
import com.ctfind.productioncontrol.auth.application.TokenIssuer
import com.ctfind.productioncontrol.auth.application.UserAccountPort
import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.UserAccount
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class AuthControllerLoginTests {

	@Test
	fun `POST login returns bearer token and authenticated admin payload`() {
		val passwordEncoder = BCryptPasswordEncoder()
		val user = UserAccount(
			id = UUID.randomUUID(),
			login = "admin",
			displayName = "Local Administrator",
			passwordHash = passwordEncoder.encode("admin") ?: error("PasswordEncoder returned null hash"),
			enabled = true,
			roleCodes = setOf(ADMIN_ROLE_CODE),
			createdAt = Instant.parse("2026-04-26T16:00:00Z"),
			updatedAt = Instant.parse("2026-04-26T16:00:00Z"),
		)
		val useCase = AuthenticateUserUseCase(
			userAccounts = object : UserAccountPort {
				override fun findByLogin(login: String): UserAccount? = user.takeIf { login == "admin" }
				override fun save(user: UserAccount): UserAccount = user
			},
			passwordEncoder = passwordEncoder,
			tokenIssuer = object : TokenIssuer {
				override fun issue(user: UserAccount): IssuedToken =
					IssuedToken(
						tokenType = "Bearer",
						accessToken = "jwt-admin",
						expiresAt = Instant.parse("2026-04-27T00:00:00Z"),
					)
			},
			audit = object : AuthenticationAuditPort {
				override fun record(event: AuthenticationAuditEvent): AuthenticationAuditEvent = event
			},
		)
		val controller = AuthController(useCase)
		val request: HttpServletRequest = MockHttpServletRequest().apply {
			remoteAddr = "127.0.0.1"
			addHeader("User-Agent", "vitest")
		}

		val response = controller.login(LoginRequest("admin", "admin"), request)

		assertEquals(HttpStatus.OK, response.statusCode)
		val body = assertIs<LoginResponse>(assertNotNull(response.body))
		assertEquals("Bearer", body.tokenType)
		assertEquals("jwt-admin", body.accessToken)
		assertEquals("admin", body.user.login)
		assertEquals(setOf(ADMIN_ROLE_CODE), body.user.roles.toSet())
	}
}
