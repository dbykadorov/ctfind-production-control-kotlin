package com.ctfind.productioncontrol.orders.domain

import java.time.Instant
import java.util.UUID

enum class CustomerStatus {
	ACTIVE,
	INACTIVE,
}

data class Customer(
	val id: UUID,
	val displayName: String,
	val status: CustomerStatus,
	val contactPerson: String? = null,
	val phone: String? = null,
	val email: String? = null,
	val createdAt: Instant,
	val updatedAt: Instant,
) {
	init {
		require(displayName.isNotBlank()) { "customer display name must not be blank" }
	}

	val active: Boolean get() = status == CustomerStatus.ACTIVE
}
