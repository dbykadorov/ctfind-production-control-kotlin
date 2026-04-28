# Contract: Backend Audit API

**Feature**: 007-audit-log-viewer
**Date**: 2026-04-28

## Endpoint: GET /api/audit

### Authorization

- **ADMIN**: 200 OK with paginated results
- **Non-ADMIN authenticated**: 403 Forbidden
- **Unauthenticated**: 401 Unauthorized

### Request

```
GET /api/audit?from={ISO-8601}&to={ISO-8601}&category={cat}&actorUserId={UUID}&search={text}&page={int}&size={int}
Authorization: Bearer {JWT}
```

| Parameter | Type | Required | Default | Notes |
|-----------|------|----------|---------|-------|
| from | ISO-8601 datetime | No | 7 days ago (00:00:00 UTC) | Inclusive lower bound |
| to | ISO-8601 datetime | No | now | Exclusive upper bound |
| category | String (repeatable) | No | all | One or more of: `AUTH`, `ORDER`, `PRODUCTION_TASK` |
| actorUserId | UUID | No | — | Filter events by actor |
| search | String | No | — | ILIKE substring on summary, targetId, actorLogin |
| page | Int | No | 0 | Zero-based page index |
| size | Int | No | 50 | Page size, capped at 100 |

### Response: 200 OK

```json
{
  "items": [
    {
      "id": "uuid",
      "occurredAt": "2026-04-28T10:15:30Z",
      "category": "ORDER",
      "eventType": "ORDER_CREATED",
      "actorDisplayName": "Администратор",
      "actorLogin": "admin",
      "summary": "Создан заказ №ORD-001 для клиента ООО «Пример»",
      "targetType": "ORDER",
      "targetId": "uuid-of-order"
    },
    {
      "id": "uuid",
      "occurredAt": "2026-04-28T10:14:00Z",
      "category": "AUTH",
      "eventType": "LOGIN_SUCCESS",
      "actorDisplayName": "Администратор",
      "actorLogin": "admin",
      "summary": "Вход в систему: admin",
      "targetType": null,
      "targetId": null
    }
  ],
  "page": 0,
  "size": 50,
  "totalItems": 142,
  "totalPages": 3
}
```

### Response DTOs

```kotlin
data class AuditLogPageResponse(
    val items: List<AuditLogRowResponse>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
)

data class AuditLogRowResponse(
    val id: UUID,
    val occurredAt: Instant,
    val category: String,       // "AUTH" | "ORDER" | "PRODUCTION_TASK"
    val eventType: String,
    val actorDisplayName: String,
    val actorLogin: String?,
    val summary: String,
    val targetType: String?,    // "ORDER" | "PRODUCTION_TASK" | null
    val targetId: UUID?,
)
```

### Error Responses

- **401**: `{"error": "unauthorized"}` — no valid JWT
- **403**: `{"error": "forbidden", "message": "Access denied"}` — valid JWT but not ADMIN

### Sort Order

Fixed: `occurredAt DESC` (most recent first). No client-controlled sorting in Phase 1.

### Implementation Notes

- Controller extracts JWT principal, delegates to `AuditLogQueryUseCase`
- Use case checks ADMIN role, then calls `AuditLogQueryPort.search(query)`
- Persistence adapter:
  1. Queries each of the 3 audit tables with SQL-level `WHERE occurred_at/event_at BETWEEN from AND to` + optional `actor_user_id` filter
  2. Skips tables whose category is excluded by the filter
  3. LEFT JOINs `app_user` for actor display name/login
  4. Maps to `AuditLogRow` (auth events get generated summary from event_type + outcome + login)
  5. Applies search filter (ILIKE on summary + targetId::text + actorLogin) in-memory
  6. Merges all results, sorts by `occurredAt` DESC
  7. Paginates in-memory: `items = merged[page*size .. (page+1)*size]`, `totalItems = merged.size`

---

## Endpoint: GET /api/users

### Authorization

- **ADMIN**: 200 OK
- **Non-ADMIN authenticated**: 403 Forbidden
- **Unauthenticated**: 401 Unauthorized

### Request

```
GET /api/users?search={text}&limit={int}
Authorization: Bearer {JWT}
```

| Parameter | Type | Required | Default | Notes |
|-----------|------|----------|---------|-------|
| search | String | No | — | ILIKE on login + displayName |
| limit | Int | No | 50 | Max results, capped at 100 |

### Response: 200 OK

```json
[
  {
    "id": "uuid",
    "login": "admin",
    "displayName": "Администратор"
  },
  {
    "id": "uuid",
    "login": "production.supervisor",
    "displayName": "Мастер участка"
  }
]
```

### Response DTO

```kotlin
data class UserSummaryResponse(
    val id: UUID,
    val login: String,
    val displayName: String,
)
```

### Implementation Notes

- Lives in the `auth` module (user management is an auth concern)
- Queries `app_user` table with optional ILIKE search on login + display_name
- Returns all matching users (not filtered by role), sorted by display_name
- Pattern mirrors existing `ProductionTaskAssigneeQueryUseCase` but without role filter
