# Contract: POST /api/notifications/mark-all-read

Mark all unread notifications of the authenticated user as read. Returns the number of actually updated notifications.

## Request

```
POST /api/notifications/mark-all-read
Authorization: Bearer {jwt}
```

No request body.

## Response: 200 OK

```json
{
  "updated": 5
}
```

### Response Fields

| Field   | Type | Description                                        |
|---------|------|----------------------------------------------------|
| updated | int  | Number of notifications that changed from unread to read |

If all notifications are already read, returns `{ "updated": 0 }`.

## Response: 401 Unauthorized

```json
{ "error": "Unauthorized" }
```

## Test Scenarios

- T-MARKALL-01: 5 unread, 3 read → 200, updated == 5; subsequent call → updated == 0
- T-MARKALL-02: 0 unread → 200, updated == 0
- T-MARKALL-03: Does not affect other users' notifications
- T-MARKALL-04: After mark-all, unread-count returns 0
- T-MARKALL-05: No JWT → 401
- T-MARKALL-06: Each updated notification gets readAt set to current timestamp
