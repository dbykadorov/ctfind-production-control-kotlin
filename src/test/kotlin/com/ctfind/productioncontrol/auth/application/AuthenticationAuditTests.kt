package com.ctfind.productioncontrol.auth.application

import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEventType
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditOutcome
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AuthenticationAuditTests {

	@Test
	fun `audit service records failure without password values`() {
		val port = RecordingAuditPort()
		val service = AuthenticationAuditService(
			audit = port,
			clock = Clock.fixed(Instant.parse("2026-04-26T16:00:00Z"), ZoneOffset.UTC),
		)

		service.loginFailure(
			login = "admin",
			outcome = AuthenticationAuditOutcome.INVALID_CREDENTIALS,
			userId = null,
			requestIp = "127.0.0.1",
			userAgent = "test",
		)

		val event = port.events.single()
		assertEquals(AuthenticationAuditEventType.LOGIN_FAILURE, event.eventType)
		assertEquals(AuthenticationAuditOutcome.INVALID_CREDENTIALS, event.outcome)
		assertFalse(event.toString().contains("wrong-password"))
	}
}

private class RecordingAuditPort : AuthenticationAuditPort {
	val events = mutableListOf<AuthenticationAuditEvent>()

	override fun record(event: AuthenticationAuditEvent): AuthenticationAuditEvent {
		events += event
		return event
	}
}
