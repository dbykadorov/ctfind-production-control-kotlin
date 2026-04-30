package com.ctfind.productioncontrol.auth.adapter.web

import com.ctfind.productioncontrol.auth.application.AuthenticationSuccess
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
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

data class UserSummaryResponse(
	val id: java.util.UUID,
	val login: String,
	val displayName: String,
	val roles: List<RoleSummaryResponse> = emptyList(),
)

data class RoleSummaryResponse(
	val code: String,
	val name: String,
)

data class CreateUserRequest(
	@field:NotBlank
	val login: String,
	@field:NotBlank
	val displayName: String,
	@field:NotBlank
	val initialPassword: String,
	@field:NotEmpty
	val roleCodes: Set<String>,
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
