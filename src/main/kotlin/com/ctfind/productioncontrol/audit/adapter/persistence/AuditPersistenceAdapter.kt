package com.ctfind.productioncontrol.audit.adapter.persistence

import com.ctfind.productioncontrol.audit.application.AuditLogPageResult
import com.ctfind.productioncontrol.audit.application.AuditLogQuery
import com.ctfind.productioncontrol.audit.application.AuditLogQueryPort
import com.ctfind.productioncontrol.audit.domain.AuditCategory
import com.ctfind.productioncontrol.audit.domain.AuditLogRow
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class JpaAuditLogAdapter(
    private val em: EntityManager,
) : AuditLogQueryPort {

    override fun search(query: AuditLogQuery): AuditLogPageResult {
        val categories = query.categories ?: AuditCategory.entries.toSet()
        val rows = mutableListOf<AuditLogRow>()

        if (AuditCategory.AUTH in categories) rows += fetchAuthEvents(query)
        if (AuditCategory.ORDER in categories) rows += fetchOrderEvents(query)
        if (AuditCategory.PRODUCTION_TASK in categories) rows += fetchProductionTaskEvents(query)

        val filtered = applySearchFilter(rows, query.search)
        val sorted = filtered.sortedByDescending { it.occurredAt }
        return paginateRows(sorted, query.page, query.size)
    }

    private fun fetchAuthEvents(query: AuditLogQuery): List<AuditLogRow> {
        val sql = buildString {
            append(
                """
                SELECT e.id, e.occurred_at, e.event_type, e.outcome, e.login,
                       e.user_id, u.display_name, u.login AS actor_login
                FROM auth_audit_event e
                LEFT JOIN app_user u ON u.id = e.user_id
                WHERE e.occurred_at >= ?1 AND e.occurred_at < ?2
                """.trimIndent(),
            )
            if (query.actorUserId != null) {
                append(" AND e.user_id = ?3")
            }
        }

        val nq = em.createNativeQuery(sql)
        nq.setParameter(1, query.from)
        nq.setParameter(2, query.to)
        if (query.actorUserId != null) {
            nq.setParameter(3, query.actorUserId)
        }

        @Suppress("UNCHECKED_CAST")
        val results = nq.resultList as List<Array<Any?>>

        return results.map { row ->
            val eventType = row[2] as String
            val outcome = row[3] as String
            val login = row[4] as String?
            val userId = row[5]?.let { toUUID(it) }
            val displayName = row[6] as String?
            val actorLogin = row[7] as String?

            AuditLogRow(
                id = toUUID(row[0]!!),
                occurredAt = toInstant(row[1]!!),
                category = AuditCategory.AUTH,
                eventType = eventType,
                actorUserId = userId,
                actorDisplayName = resolveActorDisplayName(displayName, actorLogin ?: login),
                actorLogin = actorLogin ?: login,
                summary = authEventSummary(eventType, outcome, login),
                targetType = null,
                targetId = null,
            )
        }
    }

    private fun fetchOrderEvents(query: AuditLogQuery): List<AuditLogRow> {
        return fetchGenericAuditEvents(
            tableName = "order_audit_event",
            category = AuditCategory.ORDER,
            query = query,
        )
    }

    private fun fetchProductionTaskEvents(query: AuditLogQuery): List<AuditLogRow> {
        return fetchGenericAuditEvents(
            tableName = "production_task_audit_event",
            category = AuditCategory.PRODUCTION_TASK,
            query = query,
        )
    }

    private fun fetchGenericAuditEvents(
        tableName: String,
        category: AuditCategory,
        query: AuditLogQuery,
    ): List<AuditLogRow> {
        val sql = buildString {
            append(
                """
                SELECT e.id, e.event_at, e.event_type, e.actor_user_id,
                       e.target_type, e.target_id, e.summary,
                       u.display_name, u.login AS actor_login
                FROM $tableName e
                LEFT JOIN app_user u ON u.id = e.actor_user_id
                WHERE e.event_at >= ?1 AND e.event_at < ?2
                """.trimIndent(),
            )
            if (query.actorUserId != null) {
                append(" AND e.actor_user_id = ?3")
            }
        }

        val nq = em.createNativeQuery(sql)
        nq.setParameter(1, query.from)
        nq.setParameter(2, query.to)
        if (query.actorUserId != null) {
            nq.setParameter(3, query.actorUserId)
        }

        @Suppress("UNCHECKED_CAST")
        val results = nq.resultList as List<Array<Any?>>

        return results.map { row ->
            val displayName = row[7] as String?
            val actorLogin = row[8] as String?

            AuditLogRow(
                id = toUUID(row[0]!!),
                occurredAt = toInstant(row[1]!!),
                category = category,
                eventType = row[2] as String,
                actorUserId = toUUID(row[3]!!),
                actorDisplayName = resolveActorDisplayName(displayName, actorLogin),
                actorLogin = actorLogin,
                summary = row[6] as String,
                targetType = row[4] as String,
                targetId = toUUID(row[5]!!),
            )
        }
    }

    private fun toUUID(value: Any): UUID = when (value) {
        is UUID -> value
        is String -> UUID.fromString(value)
        else -> UUID.fromString(value.toString())
    }

    private fun toInstant(value: Any): Instant = when (value) {
        is Instant -> value
        is java.sql.Timestamp -> value.toInstant()
        is java.time.OffsetDateTime -> value.toInstant()
        else -> Instant.parse(value.toString())
    }
}

internal fun authEventSummary(eventType: String, outcome: String, login: String?): String {
    val displayLogin = login ?: "?"
    return when {
        eventType == "LOGIN_SUCCESS" && outcome == "SUCCESS" ->
            "Вход в систему: $displayLogin"
        eventType == "LOGIN_FAILURE" && outcome == "INVALID_CREDENTIALS" ->
            "Неудачный вход: $displayLogin — неверные учётные данные"
        eventType == "LOGIN_FAILURE" && outcome == "THROTTLED" ->
            "Неудачный вход: $displayLogin — превышен лимит попыток"
        eventType == "LOGIN_FAILURE" && outcome == "DISABLED" ->
            "Неудачный вход: $displayLogin — учётная запись отключена"
        eventType == "LOGOUT" && outcome == "LOGGED_OUT" ->
            "Выход из системы: $displayLogin"
        eventType == "LOCAL_SEED" && outcome == "SEEDED" ->
            "Инициализация учётных данных: $displayLogin"
        eventType == "LOCAL_SEED" && outcome == "SKIPPED_EXISTING" ->
            "Пропуск инициализации (уже существует): $displayLogin"
        else ->
            "$eventType: $displayLogin"
    }
}

internal fun resolveActorDisplayName(displayName: String?, login: String?): String =
    displayName ?: login ?: "Удалённый пользователь"

internal fun applySearchFilter(rows: List<AuditLogRow>, search: String?): List<AuditLogRow> {
    if (search.isNullOrBlank()) return rows
    val term = search.trim().lowercase()
    return rows.filter { row ->
        row.summary.lowercase().contains(term) ||
            row.targetId?.toString()?.lowercase()?.contains(term) == true ||
            row.actorLogin?.lowercase()?.contains(term) == true
    }
}

internal fun paginateRows(sorted: List<AuditLogRow>, page: Int, size: Int): AuditLogPageResult {
    val safePage = page.coerceAtLeast(0)
    val safeSize = size.coerceAtLeast(1)
    val fromIndex = (safePage * safeSize).coerceAtMost(sorted.size)
    val toIndex = (fromIndex + safeSize).coerceAtMost(sorted.size)
    return AuditLogPageResult(
        items = sorted.subList(fromIndex, toIndex),
        page = safePage,
        size = safeSize,
        totalItems = sorted.size.toLong(),
    )
}
