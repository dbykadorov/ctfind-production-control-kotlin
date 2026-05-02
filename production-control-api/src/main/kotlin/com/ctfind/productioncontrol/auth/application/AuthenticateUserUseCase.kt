package com.ctfind.productioncontrol.auth.application

import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEventType
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditOutcome
import com.ctfind.productioncontrol.auth.domain.UserAccount
import com.ctfind.productioncontrol.auth.domain.normalizeLogin
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class AuthenticateUserUseCase(
	private val userAccounts: UserAccountPort,
	private val passwordEncoder: PasswordEncoder,
	private val tokenIssuer: TokenIssuer,
	private val audit: AuthenticationAuditPort,
	private val loginThrottle: LoginThrottleService = LoginThrottleService(),
) {

	fun authenticate(command: LoginCommand): AuthenticationResult {
		val normalizedLogin = normalizeLogin(command.login)
		if (normalizedLogin.isBlank() || command.password.isBlank())
			return AuthenticationResult.ValidationFailed("Login and password are required")
		if (loginThrottle.isThrottled(normalizedLogin, command.requestIp)) {
			recordLoginFailure(normalizedLogin, null, command, AuthenticationAuditOutcome.THROTTLED)
			return AuthenticationResult.Throttled
		}

		val user = userAccounts.findByLogin(normalizedLogin)
		if (user == null || !passwordEncoder.matches(command.password, user.passwordHash)) {
			loginThrottle.recordFailure(normalizedLogin, command.requestIp)
			recordLoginFailure(normalizedLogin, null, command, AuthenticationAuditOutcome.INVALID_CREDENTIALS)
			return AuthenticationResult.InvalidCredentials
		}
		if (!user.enabled) {
			recordLoginFailure(normalizedLogin, user, command, AuthenticationAuditOutcome.DISABLED)
			return AuthenticationResult.Disabled
		}

		val token = tokenIssuer.issue(user)
		loginThrottle.clear(normalizedLogin, command.requestIp)
		audit.record(
			AuthenticationAuditEvent(
				id = UUID.randomUUID(),
				eventType = AuthenticationAuditEventType.LOGIN_SUCCESS,
				outcome = AuthenticationAuditOutcome.SUCCESS,
				login = user.normalizedLogin,
				userId = user.id,
				requestIp = command.requestIp,
				userAgent = command.userAgent,
				occurredAt = Instant.now(),
				details = null,
			),
		)

		return AuthenticationResult.Success(
			AuthenticationSuccess(
				token = token,
				user = AuthenticatedUser(
					login = user.normalizedLogin,
					displayName = user.displayName,
					roles = user.roleCodes,
				),
			),
		)
	}

	private fun recordLoginFailure(
		login: String,
		user: UserAccount?,
		command: LoginCommand,
		outcome: AuthenticationAuditOutcome,
	) {
		audit.record(
			AuthenticationAuditEvent(
				id = UUID.randomUUID(),
				eventType = AuthenticationAuditEventType.LOGIN_FAILURE,
				outcome = outcome,
				login = login,
				userId = user?.id,
				requestIp = command.requestIp,
				userAgent = command.userAgent,
				occurredAt = Instant.now(),
				details = null,
			),
		)
	}
}
