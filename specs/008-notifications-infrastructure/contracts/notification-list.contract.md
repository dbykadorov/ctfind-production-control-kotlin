# Contract: GET /api/notifications

List notifications for the authenticated user.

## Request

```
GET /api/notifications?page={page}&size={size}&unreadOnly={unreadOnly}
Authorization: Bearer {jwt}
```

### Query Parameters

| Parameter   | Type    | Default | Constraints        |
|-------------|---------|---------|--------------------|
| page        | int     | 0       | Clamped to >= 0    |
| size        | int     | 20      | Clamped to 1..100  |
| unreadOnly  | boolean | false   |                    |

## Response: 200 OK

```json
{
  "items": [
    {
      "id": "uuid",
      "type": "TASK_ASSIGNED",
      "title": "Вам назначена задача PT-0042",
      "body": "Заказ SO-0015, позиция «Втулка медная»",
      "targetType": "PRODUCTION_TASK",
      "targetId": "PT-0042",
      "read": false,
      "readAt": null,
      "createdAt": "2026-04-28T10:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 42,
  "totalPages": 3
}
```

### Response Fields

| Field       | Type            | Nullable | Description                              |
|-------------|-----------------|----------|------------------------------------------|
| id          | UUID string     | no       | Notification ID                          |
| type        | string          | no       | NotificationType enum value              |
| title       | string          | no       | Short notification title                 |
| body        | string          | yes      | Extended notification text               |
| targetType  | string          | yes      | ORDER or PRODUCTION_TASK                 |
| targetId    | string          | yes      | Human-readable object identifier         |
| read        | boolean         | no       | Whether notification has been read       |
| readAt      | ISO-8601 string | yes      | Timestamp of first read, null if unread  |
| createdAt   | ISO-8601 string | no       | When notification was created            |

## Response: 401 Unauthorized

No JWT or expired token.

```json
{ "error": "Unauthorized" }
```

## Sorting

Fixed: `createdAt DESC` (newest first). Not configurable in Phase 1.

## Test Scenarios

- T-LIST-01: Authenticated user with 5 notifications → 200, items.length == 5
- T-LIST-02: unreadOnly=true with 3 unread → 200, items.length == 3, all read == false
- T-LIST-03: page=1, size=20, total=60 → items.length == 20, page == 1, totalPages == 3
- T-LIST-04: User B sees 0 items (User A's notifications hidden) → 200, items == []
- T-LIST-05: No JWT → 401
- T-LIST-06: page=100 (out of range) → 200, items == [], totalItems correct
- T-LIST-07: size=200 → clamped to 100
- T-LIST-08: size=-1 → uses default 20
