package com.ctfind.productioncontrol.audit.adapter.web

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.audit.application.AuditLogPageResult
import com.ctfind.productioncontrol.audit.application.AuditLogQuery
import com.ctfind.productioncontrol.audit.application.AuditLogQueryResult
import com.ctfind.productioncontrol.audit.domain.AuditCategory
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class AuditControllerTests {

	private val actorId = UUID.fromString("30000000-0000-0000-0000-000000000003")

	@Test
	fun `GET audit returns 200 with page response for ADMIN`() {
		val row = sampleAuditLogRow()
		val page = sampleAuditLogPage(items = listOf(row))

		val controller = AuditController(
			useCase = stubAuditQueryUseCase { _, _ ->
				AuditLogQueryResult.Success(page)
			},
		)

		val response = controller.list(
			from = Instant.parse("2026-04-01T00:00:00Z"),
			to = Instant.parse("2026-04-28T23:59:59Z"),
			category = null,
			actorUserId = null,
			search = null,
			page = 0,
			size = 50,
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)

		assertEquals(HttpStatus.OK, response.statusCode)
		val body = assertIs<AuditLogPageResponse>(response.body)
		assertEquals(1, body.items.size)
		assertEquals(row.id, body.items.first().id)
		assertEquals("AUTH", body.items.first().category)
		assertEquals("LOGIN_SUCCESS", body.items.first().eventType)
		assertEquals("Admin User", body.items.first().actorDisplayName)
		assertEquals("admin", body.items.first().actorLogin)
		assertEquals("Вход в систему: admin", body.items.first().summary)
		assertEquals(0, body.page)
		assertEquals(50, body.size)
		assertEquals(1, body.totalItems)
		assertEquals(1, body.totalPages)
	}

	@Test
	fun `GET audit returns 403 for non-ADMIN`() {
		val controller = AuditController(
			useCase = stubAuditQueryUseCase { _, _ ->
				AuditLogQueryResult.Forbidden
			},
		)

		val response = controller.list(
			from = Instant.parse("2026-04-01T00:00:00Z"),
			to = Instant.parse("2026-04-28T23:59:59Z"),
			category = null,
			actorUserId = null,
			search = null,
			page = 0,
			size = 50,
			jwt = jwtFor(actorId, setOf("ORDER_MANAGER")),
		)

		assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
		val err = assertIs<AuditApiErrorResponse>(response.body)
		assertEquals("forbidden", err.code)
	}

	@Test
	fun `query params map to AuditLogQuery correctly`() {
		var capturedQuery: AuditLogQuery? = null
		val page = sampleAuditLogPage()

		val controller = AuditController(
			useCase = stubAuditQueryUseCase { query, _ ->
				capturedQuery = query
				AuditLogQueryResult.Success(page)
			},
		)

		val fromParam = Instant.parse("2026-03-15T00:00:00Z")
		val toParam = Instant.parse("2026-04-15T23:59:59Z")
		val actorUserIdParam = UUID.fromString("40000000-0000-0000-0000-000000000004")

		controller.list(
			from = fromParam,
			to = toParam,
			category = listOf("AUTH", "ORDER"),
			actorUserId = actorUserIdParam,
			search = "login attempt",
			page = 3,
			size = 25,
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)

		assertNotNull(capturedQuery)
		assertEquals(fromParam, capturedQuery!!.from)
		assertEquals(toParam, capturedQuery!!.to)
		assertEquals(setOf(AuditCategory.AUTH, AuditCategory.ORDER), capturedQuery!!.categories)
		assertEquals(actorUserIdParam, capturedQuery!!.actorUserId)
		assertEquals("login attempt", capturedQuery!!.search)
		assertEquals(3, capturedQuery!!.page)
		assertEquals(25, capturedQuery!!.size)
	}
}
