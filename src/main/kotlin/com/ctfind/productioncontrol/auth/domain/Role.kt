package com.ctfind.productioncontrol.auth.domain

import java.time.Instant
import java.util.UUID

const val ADMIN_ROLE_CODE = "ADMIN"

data class Role(
	val id: UUID,
	val code: String,
	val name: String,
	val createdAt: Instant,
) {
	init {
		require(code.isNotBlank()) { "role code must not be blank" }
		require(code == code.uppercase()) { "role code must be uppercase" }
		require(name.isNotBlank()) { "role name must not be blank" }
	}
}
