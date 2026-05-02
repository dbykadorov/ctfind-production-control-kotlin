package com.ctfind.productioncontrol.auth.application

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginThrottlePolicyTests {

	@Test
	fun `repeated failures for same login and ip become temporarily throttled`() {
		val throttle = LoginThrottleService(maxFailures = 2)
		val keyLogin = "admin"
		val keyIp = "127.0.0.1"

		assertFalse(throttle.isThrottled(keyLogin, keyIp))

		throttle.recordFailure(keyLogin, keyIp)
		assertFalse(throttle.isThrottled(keyLogin, keyIp))

		throttle.recordFailure(keyLogin, keyIp)
		assertTrue(throttle.isThrottled(keyLogin, keyIp))
	}
}
