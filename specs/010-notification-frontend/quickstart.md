# Quickstart: Фронтенд уведомлений

## Prerequisites

- Docker stack running: `make docker-up-detached && make health`
- Frontend dev server: `cd frontend/cabinet && pnpm dev`
- Seeded users: admin/admin, production.executor/executor, production.supervisor/supervisor

## Step 1: Verify badge appears

1. Open browser: http://localhost:5173/cabinet/login
2. Login as `admin` / `admin`
3. Look at TopBar — bell icon should be visible
4. If there are unread notifications, a badge number appears next to the bell

## Step 2: Create a notification via backend

```bash
# Login as admin
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"admin","password":"admin"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# Find an unassigned task
TASK=$(curl -s -H "Authorization: Bearer $TOKEN" \
  'http://localhost:8080/api/production-tasks?size=1' | python3 -c "
import sys, json
data = json.load(sys.stdin)
t = data['items'][0]
print(f\"{t['id']}|{t['version']}\")
")
TASK_ID=$(echo "$TASK" | cut -d'|' -f1)
VERSION=$(echo "$TASK" | cut -d'|' -f2)

# Assign executor (generates TASK_ASSIGNED notification)
EXECUTOR_ID=$(curl -s -H "Authorization: Bearer $TOKEN" \
  'http://localhost:8080/api/production-tasks/assignees?limit=1' | python3 -c "
import sys, json; print(json.load(sys.stdin)['items'][0]['id'])")

curl -s -X PUT "http://localhost:8080/api/production-tasks/${TASK_ID}/assignment" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"executorUserId\":\"$EXECUTOR_ID\",\"expectedVersion\":$VERSION}"
```

## Step 3: Verify badge updates

1. Login as `production.executor` / `executor` in browser
2. Wait up to 30 seconds for polling cycle
3. Badge should show "1" (or increment)

## Step 4: Verify dropdown

1. Click the bell icon
2. Dropdown opens showing TASK_ASSIGNED notification
3. Click the notification → navigates to production task detail page
4. Notification is marked as read
5. Badge count decreases

## Step 5: Verify "Mark all read"

1. Click bell icon
2. Click "Отметить все прочитанными"
3. All notifications marked read
4. Badge disappears

## Step 6: Verify notifications page

1. Click "Уведомления" in sidebar (or "Все уведомления" link in dropdown)
2. Full list with pagination loads
3. Toggle "Только непрочитанные" filter
4. Click a notification → navigates to object

## Verification Checklist

- [ ] Bell icon visible in TopBar for all roles
- [ ] Badge shows unread count, hidden when 0
- [ ] Badge updates within 30 seconds of new notification
- [ ] Dropdown shows last 10 notifications on click
- [ ] Click notification → mark read + navigate to object
- [ ] "Отметить все прочитанными" works in dropdown
- [ ] "Все уведомления" link navigates to /cabinet/notifications
- [ ] Notifications page with pagination (20/page)
- [ ] "Только непрочитанные" filter works
- [ ] "Уведомления" sidebar item visible and navigates correctly
- [ ] Empty state shown when no notifications
