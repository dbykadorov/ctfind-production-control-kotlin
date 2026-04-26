package com.ctfind.productioncontrol.auth.adapter.web

import com.ctfind.productioncontrol.auth.application.AuthenticationAuditPort
import com.ctfind.productioncontrol.auth.application.AuthenticateUserUseCase
import com.ctfind.productioncontrol.auth.application.IssuedToken
import com.ctfind.productioncontrol.auth.application.TokenIssuer
import com.ctfind.productioncontrol.auth.application.UserAccountPort
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.UserAccount
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthControllerFailureTests {

	@Test
	fun `blank login returns validation error response`() {
		val controller = AuthController(authenticateUserUseCase())

		val response = controller.login(LoginRequest(" ", "admin"), MockHttpServletRequest())

		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		assertEquals("AUTH_VALIDATION_FAILED", assertIs<AuthErrorResponse>(response.body).code)
	}

	@Test
	fun `wrong password returns generic invalid credentials response`() {
		val controller = AuthController(authenticateUserUseCase())

		val response = controller.login(LoginRequest("admin", "wrong"), MockHttpServletRequest())

		assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
		assertEquals("AUTH_INVALID_CREDENTIALS", assertIs<AuthErrorResponse>(response.body).code)
	}
}

private fun authenticateUserUseCase(): AuthenticateUserUseCase =
	AuthenticateUserUseCase(
		userAccounts = object : UserAccountPort {
			override fun findByLogin(login: String): UserAccount? = null
			override fun save(user: UserAccount): UserAccount = user
		},
		passwordEncoder = BCryptPasswordEncoder(),
		tokenIssuer = object : TokenIssuer {
			override fun issue(user: UserAccount): IssuedToken = error("not used")
		},
		audit = object : AuthenticationAuditPort {
			override fun record(event: AuthenticationAuditEvent): AuthenticationAuditEvent = event
		},
	)
