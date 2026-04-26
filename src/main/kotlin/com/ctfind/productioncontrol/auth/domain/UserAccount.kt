package com.ctfind.productioncontrol.auth.domain

import java.time.Instant
import java.util.UUID

data class UserAccount(
	val id: UUID,
	val login: String,
	val displayName: String,
	val passwordHash: String,
	val enabled: Boolean,
	val roleCodes: Set<String>,
	val createdAt: Instant,
	val updatedAt: Instant,
) {
	val normalizedLogin: String = normalizeLogin(login)

	init {
		require(normalizedLogin.isNotBlank()) { "login must not be blank" }
		require(displayName.isNotBlank()) { "displayName must not be blank" }
		require(passwordHash.isNotBlank()) { "passwordHash must not be blank" }
	}
}

fun normalizeLogin(login: String): String = login.trim().lowercase()
