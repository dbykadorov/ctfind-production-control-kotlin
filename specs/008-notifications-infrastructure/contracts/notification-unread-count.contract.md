# Contract: GET /api/notifications/unread-count

Lightweight endpoint returning the number of unread notifications for the authenticated user. Designed for frequent polling (every 30s) from the frontend.

## Request

```
GET /api/notifications/unread-count
Authorization: Bearer {jwt}
```

No query parameters.

## Response: 200 OK

```json
{
  "count": 3
}
```

### Response Fields

| Field | Type | Description                                    |
|-------|------|------------------------------------------------|
| count | int  | Number of unread notifications for current user |

## Response: 401 Unauthorized

```json
{ "error": "Unauthorized" }
```

## Test Scenarios

- T-COUNT-01: User with 3 unread → 200, count == 3
- T-COUNT-02: User with 0 unread → 200, count == 0
- T-COUNT-03: User B sees count of own unread only (User A's not counted)
- T-COUNT-04: No JWT → 401
