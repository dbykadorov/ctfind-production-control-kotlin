package com.ctfind.productioncontrol.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AuthUsersSecurityIntegrationTest : IntegrationTestSupport() {

	@Test
	@DisplayName("Auth/users integration: ${ScenarioCoverage.AUTH_USERS}")
	fun `bootstrap login current user and users api security are wired`() {
		val adminCount = jdbc.queryForObject(
			"""
			SELECT COUNT(*)
			FROM app_user u
			JOIN app_user_role ur ON ur.user_id = u.id
			JOIN app_role r ON r.id = ur.role_id
			WHERE u.enabled = true AND r.code = 'ADMIN'
			""".trimIndent(),
			Long::class.java,
		)
		assertEquals(1L, adminCount)

		val adminToken = login(SUPERADMIN_LOGIN, SUPERADMIN_PASSWORD)
		val me = json(getJson("/api/auth/me", adminToken).assertOk())
		assertEquals(SUPERADMIN_LOGIN, me["login"].asText())
		assertTrue(me["roles"].map { it.asText() }.contains("ADMIN"))

		val nonAdmin = createScenarioUser("warehouse.auth", setOf("WAREHOUSE"))

		val users = json(getJson("/api/users", adminToken).assertOk())
		assertTrue(users.any { it["login"].asText() == SUPERADMIN_LOGIN })
		assertTrue(users.any { it["login"].asText() == nonAdmin.login })

		getJson("/api/users", nonAdmin.token).assertForbidden()
		getJson("/api/auth/me").assertUnauthorized()
		getJson("/api/users").assertUnauthorized()
	}
}
