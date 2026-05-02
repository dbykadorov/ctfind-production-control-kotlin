package com.ctfind.productioncontrol.auth.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import org.springframework.stereotype.Service

sealed interface RoleCatalogResult {
	data class Success(val roles: List<RoleSummary>) : RoleCatalogResult
	data object Forbidden : RoleCatalogResult
}

@Service
open class RoleCatalogUseCase(
	private val catalog: RoleCatalogPort,
) {
	open fun list(roleCodes: Set<String>): RoleCatalogResult {
		if (ADMIN_ROLE_CODE !in roleCodes)
			return RoleCatalogResult.Forbidden

		val roles = catalog.listRoles(SUPPORTED_ROLE_CODES)
			.filter { it.code in SUPPORTED_ROLE_CODES }
			.sortedBy { it.code }
		return RoleCatalogResult.Success(roles)
	}
}
