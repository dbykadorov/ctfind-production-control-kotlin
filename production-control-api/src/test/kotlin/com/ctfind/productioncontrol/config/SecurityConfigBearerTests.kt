package com.ctfind.productioncontrol.config

import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.test.assertTrue

class SecurityConfigBearerTests {

	@Test
	fun `protected APIs use bearer JWT resource server while login stays public`() {
		val securityConfig = Path("src/main/kotlin/com/ctfind/productioncontrol/config/SecurityConfig.kt")
			.readText()

		assertTrue(securityConfig.contains("requestMatchers(\"/api/auth/login\").permitAll()"))
		assertTrue(securityConfig.contains("oauth2ResourceServer"))
		assertTrue(securityConfig.contains("SessionCreationPolicy.STATELESS"))
		assertTrue(securityConfig.contains("HttpStatus.UNAUTHORIZED"))
	}
}
