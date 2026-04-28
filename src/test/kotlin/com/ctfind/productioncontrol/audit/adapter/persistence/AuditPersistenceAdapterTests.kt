package com.ctfind.productioncontrol.audit.adapter.persistence

import com.ctfind.productioncontrol.audit.domain.AuditCategory
import com.ctfind.productioncontrol.audit.domain.AuditLogRow
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuditPersistenceAdapterTests {

	// ─── auth event summary generation ───────────────────────────────────────

	@Test
	fun `auth summary for LOGIN_SUCCESS and SUCCESS`() {
		val summary = authEventSummary(
			eventType = "LOGIN_SUCCESS",
			outcome = "SUCCESS",
			login = "admin",
		)
		assertEquals("Вход в систему: admin", summary)
	}

	@Test
	fun `auth summary for LOGIN_FAILURE and INVALID_CREDENTIALS`() {
		val summary = authEventSummary(
			eventType = "LOGIN_FAILURE",
			outcome = "INVALID_CREDENTIALS",
			login = "admin",
		)
		assertEquals("Неудачный вход: admin — неверные учётные данные", summary)
	}

	@Test
	fun `auth summary for LOGOUT and LOGGED_OUT`() {
		val summary = authEventSummary(
			eventType = "LOGOUT",
			outcome = "LOGGED_OUT",
			login = "admin",
		)
		assertEquals("Выход из системы: admin", summary)
	}

	@Test
	fun `auth summary for LOCAL_SEED and SEEDED`() {
		val summary = authEventSummary(
			eventType = "LOCAL_SEED",
			outcome = "SEEDED",
			login = "admin",
		)
		assertEquals("Инициализация учётных данных: admin", summary)
	}

	// ─── actor display name fallback ─────────────────────────────────────────

	@Test
	fun `actor display name uses display_name when available`() {
		val name = resolveActorDisplayName(displayName = "Иванов И.И.", login = "ivanov")
		assertEquals("Иванов И.И.", name)
	}

	@Test
	fun `actor display name falls back to login when display_name is null`() {
		val name = resolveActorDisplayName(displayName = null, login = "ivanov")
		assertEquals("ivanov", name)
	}

	@Test
	fun `actor display name falls back to default when both null`() {
		val name = resolveActorDisplayName(displayName = null, login = null)
		assertEquals("Удалённый пользователь", name)
	}

	// ─── search filter ───────────────────────────────────────────────────────

	@Test
	fun `search filter matches summary substring`() {
		val rows = listOf(
			sampleRow(summary = "Вход в систему: admin"),
			sampleRow(summary = "Создание заказа ORD-5"),
			sampleRow(summary = "Выход из системы: user1"),
		)
		val filtered = applySearchFilter(rows, "заказ")
		assertEquals(1, filtered.size)
		assertEquals("Создание заказа ORD-5", filtered.first().summary)
	}

	@Test
	fun `search filter is case-insensitive`() {
		val rows = listOf(
			sampleRow(summary = "Вход в систему: Admin"),
		)
		val filtered = applySearchFilter(rows, "admin")
		assertEquals(1, filtered.size)
	}

	@Test
	fun `search filter matches targetId`() {
		val targetId = UUID.fromString("99999999-9999-9999-9999-999999999999")
		val rows = listOf(
			sampleRow(summary = "Some event", targetId = targetId),
			sampleRow(summary = "Other event", targetId = null),
		)
		val filtered = applySearchFilter(rows, "99999999")
		assertEquals(1, filtered.size)
		assertEquals(targetId, filtered.first().targetId)
	}

	@Test
	fun `search filter matches actorLogin`() {
		val rows = listOf(
			sampleRow(summary = "Some event", actorLogin = "supervisor"),
			sampleRow(summary = "Other event", actorLogin = "worker"),
		)
		val filtered = applySearchFilter(rows, "superv")
		assertEquals(1, filtered.size)
		assertEquals("supervisor", filtered.first().actorLogin)
	}

	@Test
	fun `search filter returns all rows when search is null`() {
		val rows = listOf(
			sampleRow(summary = "Event 1"),
			sampleRow(summary = "Event 2"),
		)
		val filtered = applySearchFilter(rows, null)
		assertEquals(2, filtered.size)
	}

	// ─── sort ────────────────────────────────────────────────────────────────

	@Test
	fun `sort by occurredAt descending`() {
		val t1 = Instant.parse("2026-04-28T08:00:00Z")
		val t2 = Instant.parse("2026-04-28T10:00:00Z")
		val t3 = Instant.parse("2026-04-28T09:00:00Z")

		val rows = listOf(
			sampleRow(occurredAt = t1, summary = "first"),
			sampleRow(occurredAt = t2, summary = "second"),
			sampleRow(occurredAt = t3, summary = "third"),
		)

		val sorted = rows.sortedByDescending { it.occurredAt }
		assertEquals("second", sorted[0].summary)
		assertEquals("third", sorted[1].summary)
		assertEquals("first", sorted[2].summary)
	}

	// ─── pagination ──────────────────────────────────────────────────────────

	@Test
	fun `pagination returns correct slice`() {
		val rows = (1..15).map { i ->
			sampleRow(
				id = UUID.nameUUIDFromBytes("item-$i".toByteArray()),
				summary = "Event $i",
			)
		}

		val result = paginateRows(rows, page = 1, size = 5)

		assertEquals(5, result.items.size)
		assertEquals("Event 6", result.items.first().summary)
		assertEquals("Event 10", result.items.last().summary)
		assertEquals(1, result.page)
		assertEquals(5, result.size)
		assertEquals(15, result.totalItems)
		assertEquals(3, result.totalPages)
	}

	@Test
	fun `pagination returns empty for out-of-range page`() {
		val rows = (1..5).map { i ->
			sampleRow(summary = "Event $i")
		}

		val result = paginateRows(rows, page = 3, size = 5)
		assertTrue(result.items.isEmpty())
		assertEquals(5, result.totalItems)
	}

	@Test
	fun `pagination returns partial last page`() {
		val rows = (1..7).map { i ->
			sampleRow(summary = "Event $i")
		}

		val result = paginateRows(rows, page = 1, size = 5)
		assertEquals(2, result.items.size)
		assertEquals("Event 6", result.items.first().summary)
		assertEquals("Event 7", result.items.last().summary)
		assertEquals(7, result.totalItems)
		assertEquals(2, result.totalPages)
	}

	// ─── helpers ─────────────────────────────────────────────────────────────

	private fun sampleRow(
		id: UUID = UUID.randomUUID(),
		occurredAt: Instant = Instant.parse("2026-04-28T10:00:00Z"),
		category: AuditCategory = AuditCategory.AUTH,
		eventType: String = "LOGIN_SUCCESS",
		actorUserId: UUID? = UUID.fromString("20000000-0000-0000-0000-000000000002"),
		actorDisplayName: String = "Tester",
		actorLogin: String? = "tester",
		summary: String = "Test event",
		targetType: String? = null,
		targetId: UUID? = null,
	): AuditLogRow = AuditLogRow(
		id = id,
		occurredAt = occurredAt,
		category = category,
		eventType = eventType,
		actorUserId = actorUserId,
		actorDisplayName = actorDisplayName,
		actorLogin = actorLogin,
		summary = summary,
		targetType = targetType,
		targetId = targetId,
	)
}
