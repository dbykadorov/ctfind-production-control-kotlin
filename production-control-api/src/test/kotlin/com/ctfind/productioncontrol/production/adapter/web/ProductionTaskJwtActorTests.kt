package com.ctfind.productioncontrol.production.adapter.web

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import org.springframework.security.oauth2.jwt.Jwt

class ProductionTaskJwtActorTests {

	@Test
	fun `toProductionActor maps jwt claims`() {
		val uid = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
		val jwt = Jwt.withTokenValue("tok")
			.header("alg", "none")
			.subject("user1")
			.claim("userId", uid.toString())
			.claim("roles", listOf("ADMIN", "ORDER_MANAGER"))
			.claim("displayName", "Tester")
			.build()

		val actor = jwt.toProductionActor()

		assertEquals(uid, actor.userId)
		assertEquals("user1", actor.login)
		assertEquals("Tester", actor.displayName)
		assertEquals(setOf("ADMIN", "ORDER_MANAGER"), actor.roleCodes)
	}
}
