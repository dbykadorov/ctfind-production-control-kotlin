package com.ctfind.productioncontrol.auth.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RoleCatalogUseCaseTests {

	@Test
	fun `returns supported role catalog for admin`() {
		val useCase = RoleCatalogUseCase(
			catalog = object : RoleCatalogPort {
				override fun listRoles(codes: Set<String>): List<RoleSummary> =
					codes.sorted().map { code -> RoleSummary(code = code, name = "name-$code") }
			},
		)

		val result = useCase.list(setOf(ADMIN_ROLE_CODE))
		val success = assertIs<RoleCatalogResult.Success>(result)

		assertEquals(SUPPORTED_ROLE_CODES.size, success.roles.size)
		assertEquals(SUPPORTED_ROLE_CODES.sorted(), success.roles.map { it.code })
	}

	@Test
	fun `returns forbidden for non-admin`() {
		val useCase = RoleCatalogUseCase(
			catalog = object : RoleCatalogPort {
				override fun listRoles(codes: Set<String>): List<RoleSummary> = emptyList()
			},
		)

		val result = useCase.list(setOf(WAREHOUSE_ROLE_CODE))
		assertIs<RoleCatalogResult.Forbidden>(result)
	}
}
