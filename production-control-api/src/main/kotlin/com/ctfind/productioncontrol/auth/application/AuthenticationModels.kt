package com.ctfind.productioncontrol.auth.application

import java.time.Instant

data class LoginCommand(
	val login: String,
	val password: String,
	val requestIp: String?,
	val userAgent: String?,
)

data class AuthenticatedUser(
	val login: String,
	val displayName: String,
	val roles: Set<String>,
)

data class IssuedToken(
	val tokenType: String,
	val accessToken: String,
	val expiresAt: Instant,
)

data class AuthenticationSuccess(
	val token: IssuedToken,
	val user: AuthenticatedUser,
)

sealed interface AuthenticationResult {
	data class Success(val value: AuthenticationSuccess) : AuthenticationResult
	data object InvalidCredentials : AuthenticationResult
	data object Disabled : AuthenticationResult
	data object Throttled : AuthenticationResult
	data class ValidationFailed(val message: String) : AuthenticationResult
}
