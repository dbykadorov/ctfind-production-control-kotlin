package com.ctfind.productioncontrol.config

import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.test.assertFalse

class SecurityConfigTests {

	@Test
	fun `API security does not enable browser login or basic auth`() {
		val securityConfig = Path("src/main/kotlin/com/ctfind/productioncontrol/config/SecurityConfig.kt")
			.readText()

		assertFalse(securityConfig.contains(".httpBasic("), "API backend must not enable HTTP Basic auth")
		assertFalse(securityConfig.contains(".formLogin("), "API backend must not enable browser form login")
	}

	@Test
	fun `API security enables bearer JWT resource server`() {
		val securityConfig = Path("src/main/kotlin/com/ctfind/productioncontrol/config/SecurityConfig.kt")
			.readText()

		kotlin.test.assertTrue(
			securityConfig.contains("oauth2ResourceServer"),
			"Protected APIs must authenticate Bearer JWT tokens through Spring Security",
		)
	}
}
