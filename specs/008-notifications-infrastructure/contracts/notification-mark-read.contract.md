# Contract: PATCH /api/notifications/{id}/read

Mark a single notification as read. Idempotent — calling on an already-read notification succeeds without changing readAt.

## Request

```
PATCH /api/notifications/{id}/read
Authorization: Bearer {jwt}
```

### Path Parameters

| Parameter | Type | Description       |
|-----------|------|-------------------|
| id        | UUID | Notification ID   |

No request body.

## Response: 200 OK

```json
{
  "id": "uuid",
  "read": true,
  "readAt": "2026-04-28T10:35:00Z"
}
```

### Response Fields

| Field  | Type            | Description                              |
|--------|-----------------|------------------------------------------|
| id     | UUID string     | Notification ID                          |
| read   | boolean         | Always true after this call              |
| readAt | ISO-8601 string | Timestamp of first read (never changes)  |

## Response: 404 Not Found

Notification does not exist OR belongs to another user.

```json
{
  "error": "Not Found",
  "message": "Notification not found"
}
```

## Response: 401 Unauthorized

```json
{ "error": "Unauthorized" }
```

## Test Scenarios

- T-READ-01: Unread notification → 200, read == true, readAt set
- T-READ-02: Already-read notification (readAt = T1) → 200, readAt still == T1
- T-READ-03: Notification of another user → 404
- T-READ-04: Non-existent UUID → 404
- T-READ-05: No JWT → 401
