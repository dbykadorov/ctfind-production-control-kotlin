package com.ctfind.productioncontrol.audit.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.audit.domain.AuditCategory
import com.ctfind.productioncontrol.audit.domain.AuditLogRow
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuditLogQueryUseCaseTests {

	private val now = Instant.parse("2026-04-28T12:00:00Z")

	private val sampleRow = AuditLogRow(
		id = UUID.fromString("10000000-0000-0000-0000-000000000001"),
		occurredAt = now,
		category = AuditCategory.AUTH,
		eventType = "LOGIN_SUCCESS",
		actorUserId = UUID.fromString("20000000-0000-0000-0000-000000000002"),
		actorDisplayName = "Admin User",
		actorLogin = "admin",
		summary = "Вход в систему: admin",
		targetType = null,
		targetId = null,
	)

	private val samplePage = AuditLogPageResult(
		items = listOf(sampleRow),
		page = 0,
		size = 50,
		totalItems = 1,
	)

	private val sampleQuery = AuditLogQuery(
		from = Instant.parse("2026-04-01T00:00:00Z"),
		to = Instant.parse("2026-04-28T23:59:59Z"),
	)

	@Test
	fun `ADMIN role delegates to port and returns Success`() {
		val uc = useCase(returnPage = samplePage)
		val result = uc.list(sampleQuery, roleCodes = setOf(ADMIN_ROLE_CODE))

		val success = assertIs<AuditLogQueryResult.Success>(result)
		assertEquals(samplePage, success.page)
		assertEquals(1, success.page.items.size)
		assertEquals(sampleRow.id, success.page.items.first().id)
	}

	@Test
	fun `non-ADMIN role returns Forbidden`() {
		val uc = useCase(returnPage = samplePage)
		val result = uc.list(sampleQuery, roleCodes = setOf("ORDER_MANAGER"))

		assertIs<AuditLogQueryResult.Forbidden>(result)
	}

	@Test
	fun `empty roles returns Forbidden`() {
		val uc = useCase(returnPage = samplePage)
		val result = uc.list(sampleQuery, roleCodes = emptySet())

		assertIs<AuditLogQueryResult.Forbidden>(result)
	}

	@Test
	fun `query params are forwarded to port`() {
		var capturedQuery: AuditLogQuery? = null
		val port = object : AuditLogQueryPort {
			override fun search(query: AuditLogQuery): AuditLogPageResult {
				capturedQuery = query
				return samplePage
			}
		}
		val uc = AuditLogQueryUseCase(port)

		val specificQuery = AuditLogQuery(
			from = Instant.parse("2026-03-01T00:00:00Z"),
			to = Instant.parse("2026-03-31T23:59:59Z"),
			categories = setOf(AuditCategory.AUTH, AuditCategory.ORDER),
			actorUserId = UUID.fromString("30000000-0000-0000-0000-000000000003"),
			search = "login",
			page = 2,
			size = 25,
		)

		uc.list(specificQuery, roleCodes = setOf(ADMIN_ROLE_CODE))

		assertEquals(specificQuery, capturedQuery)
	}

	private fun useCase(returnPage: AuditLogPageResult): AuditLogQueryUseCase {
		val port = object : AuditLogQueryPort {
			override fun search(query: AuditLogQuery): AuditLogPageResult = returnPage
		}
		return AuditLogQueryUseCase(port)
	}
}
