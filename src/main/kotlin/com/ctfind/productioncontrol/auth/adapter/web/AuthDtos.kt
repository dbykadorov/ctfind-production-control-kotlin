package com.ctfind.productioncontrol.auth.adapter.web

import com.ctfind.productioncontrol.auth.application.AuthenticationSuccess
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class LoginRequest(
	@field:NotBlank
	val login: String,
	@field:NotBlank
	val password: String,
)

data class LoginResponse(
	val tokenType: String,
	val accessToken: String,
	val expiresAt: Instant,
	val user: AuthenticatedUserResponse,
)

data class AuthenticatedUserResponse(
	val login: String,
	val displayName: String,
	val roles: Set<String>,
)

data class MeResponse(
	val login: String,
	val displayName: String,
	val roles: Set<String>,
	val expiresAt: Instant?,
)

fun AuthenticationSuccess.toLoginResponse(): LoginResponse =
	LoginResponse(
		tokenType = token.tokenType,
		accessToken = token.accessToken,
		expiresAt = token.expiresAt,
		user = AuthenticatedUserResponse(
			login = user.login,
			displayName = user.displayName,
			roles = user.roles,
		),
	)
