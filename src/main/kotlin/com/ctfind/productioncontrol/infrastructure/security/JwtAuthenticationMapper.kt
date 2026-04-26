package com.ctfind.productioncontrol.infrastructure.security

import com.ctfind.productioncontrol.auth.adapter.web.AuthenticatedUserResponse
import com.ctfind.productioncontrol.auth.adapter.web.MeResponse
import org.springframework.security.oauth2.jwt.Jwt

class JwtAuthenticationMapper {

	fun toMeResponse(jwt: Jwt): MeResponse =
		MeResponse(
			login = jwt.subject,
			displayName = jwt.claims["displayName"] as? String ?: jwt.subject,
			roles = roles(jwt),
			expiresAt = jwt.expiresAt,
		)

	fun toAuthenticatedUser(jwt: Jwt): AuthenticatedUserResponse =
		AuthenticatedUserResponse(
			login = jwt.subject,
			displayName = jwt.claims["displayName"] as? String ?: jwt.subject,
			roles = roles(jwt),
		)

	@Suppress("UNCHECKED_CAST")
	private fun roles(jwt: Jwt): Set<String> =
		when (val raw = jwt.claims["roles"]) {
			is Collection<*> -> raw.filterIsInstance<String>().toSet()
			is String -> setOf(raw)
			else -> emptySet()
		}
}
