# Quickstart: 008-notifications-infrastructure

## Prerequisites

- Docker Compose stack running: `make docker-up-detached`
- Backend built and running: `make backend-run` (or via Docker)
- Seeded admin user: `admin / admin`

## Step 1: Get JWT token

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"admin"}' | jq -r '.accessToken')
echo $TOKEN
```

## Step 2: Check unread count (should be 0 on fresh DB)

```bash
curl -s http://localhost:8080/api/notifications/unread-count \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected: `{ "count": 0 }`

## Step 3: List notifications (empty on fresh DB)

```bash
curl -s "http://localhost:8080/api/notifications?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected: `{ "items": [], "page": 0, "size": 20, "totalItems": 0, "totalPages": 0 }`

## Step 4: Create a test notification via port (requires test or seed)

Since notification creation is internal (no public REST endpoint), test via:
- Backend unit/integration tests: `make backend-test`
- Or add a temporary seed in `LocalNotificationSeedRunner` (if created for dev)

## Step 5: Verify notification appears after creation

After a notification is created (e.g., via seed or test):

```bash
# Unread count should increase
curl -s http://localhost:8080/api/notifications/unread-count \
  -H "Authorization: Bearer $TOKEN" | jq .

# List should show the notification
curl -s "http://localhost:8080/api/notifications" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

## Step 6: Mark notification as read

```bash
# Replace NOTIFICATION_ID with actual UUID from Step 5
curl -s -X PATCH http://localhost:8080/api/notifications/NOTIFICATION_ID/read \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected: `{ "id": "...", "read": true, "readAt": "2026-..." }`

## Step 7: Mark all read

```bash
curl -s -X POST http://localhost:8080/api/notifications/mark-all-read \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected: `{ "updated": N }`

## Step 8: Verify 401 without token

```bash
curl -s http://localhost:8080/api/notifications/unread-count | jq .
```

Expected: 401 Unauthorized

## Verification Checklist

- [ ] Unread count returns 0 on fresh DB
- [ ] List returns empty page on fresh DB
- [ ] After notification creation: count increases, notification appears in list
- [ ] Mark-read sets read=true and readAt
- [ ] Mark-read is idempotent (readAt doesn't change on second call)
- [ ] Mark-all-read returns correct updated count
- [ ] Unread count returns 0 after mark-all-read
- [ ] 401 returned without JWT
- [ ] User A cannot see User B's notifications
- [ ] `make backend-test` passes
- [ ] `make backend-build` succeeds
- [ ] `make health` returns UP
