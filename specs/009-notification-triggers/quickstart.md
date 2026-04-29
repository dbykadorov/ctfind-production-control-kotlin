# Quickstart: Триггеры генерации уведомлений

## Prerequisites

- Docker stack running: `make docker-up-detached && make health`
- Seeded users: admin, order.manager, production.supervisor, production.executor

## Step 1: Login as supervisor and executor

```bash
# Login as supervisor (creates tasks)
SUPERVISOR_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"production.supervisor","password":"admin"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# Login as executor (receives assignment notifications)
EXECUTOR_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"production.executor","password":"admin"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

echo "Supervisor token: ${#SUPERVISOR_TOKEN} chars"
echo "Executor token: ${#EXECUTOR_TOKEN} chars"
```

## Step 2: Verify TASK_ASSIGNED trigger

```bash
# Find a task to assign (get first unassigned task)
TASK=$(curl -s -H "Authorization: Bearer $SUPERVISOR_TOKEN" \
  'http://localhost:8080/api/production-tasks?size=1' | python3 -c "
import sys, json
data = json.load(sys.stdin)
items = data.get('items', [])
if items:
    t = items[0]
    print(f\"id={t['id']} number={t['taskNumber']} version={t['version']} executor={t.get('executorUserId','none')}\")
else:
    print('NO_TASKS')
")
echo "Task: $TASK"

# Get executor user ID
EXECUTOR_ID=$(curl -s -H "Authorization: Bearer $SUPERVISOR_TOKEN" \
  'http://localhost:8080/api/production-tasks/executors?limit=1' | python3 -c "
import sys, json
data = json.load(sys.stdin)
if data: print(data[0]['id'])
")
echo "Executor ID: $EXECUTOR_ID"

# Assign task to executor (replace TASK_ID and VERSION from above)
# curl -s -X PUT http://localhost:8080/api/production-tasks/{TASK_ID}/assign \
#   -H "Authorization: Bearer $SUPERVISOR_TOKEN" \
#   -H 'Content-Type: application/json' \
#   -d '{"executorUserId":"'$EXECUTOR_ID'","expectedVersion":VERSION}'

# Check executor's notifications — should see TASK_ASSIGNED
curl -s -H "Authorization: Bearer $EXECUTOR_TOKEN" \
  'http://localhost:8080/api/notifications?size=5' | python3 -m json.tool
```

**Expected**: Notification with `type=TASK_ASSIGNED`, `targetType=PRODUCTION_TASK`.

## Step 3: Verify STATUS_CHANGED trigger

```bash
# Change task status (executor starts work)
# curl -s -X PUT http://localhost:8080/api/production-tasks/{TASK_ID}/status \
#   -H "Authorization: Bearer $EXECUTOR_TOKEN" \
#   -H 'Content-Type: application/json' \
#   -d '{"toStatus":"IN_PROGRESS","expectedVersion":NEW_VERSION}'

# Check supervisor's notifications — should see STATUS_CHANGED
curl -s -H "Authorization: Bearer $SUPERVISOR_TOKEN" \
  'http://localhost:8080/api/notifications?size=5' | python3 -m json.tool
```

**Expected**: Notification with `type=STATUS_CHANGED`, title contains task number and new status.

## Step 4: Verify self-notification suppression

```bash
# If supervisor changes status of own task → NO notification should be created
# Verify unread-count didn't change
curl -s -H "Authorization: Bearer $SUPERVISOR_TOKEN" \
  'http://localhost:8080/api/notifications/unread-count'
```

**Expected**: Count does not increase when actor === createdByUserId.

## Step 5: Verify TASK_OVERDUE (scheduled)

TASK_OVERDUE triggers run every 15 minutes via @Scheduled.
To verify manually, create/find a task with plannedFinishDate in the past and wait for the next job run.

```bash
# Check notifications after 15 minutes for overdue tasks
curl -s -H "Authorization: Bearer $EXECUTOR_TOKEN" \
  'http://localhost:8080/api/notifications?size=10' | python3 -c "
import sys, json
data = json.load(sys.stdin)
overdue = [n for n in data['items'] if n['type'] == 'TASK_OVERDUE']
print(f'TASK_OVERDUE notifications: {len(overdue)}')
for n in overdue:
    print(f\"  - {n['title']} (targetId={n.get('targetId','?')})\")
"
```

**Expected**: TASK_OVERDUE notifications for tasks with past plannedFinishDate.

## Step 6: Verify deduplication

```bash
# Wait for another scheduled run (15 min) — no new TASK_OVERDUE should appear
# for the same task
curl -s -H "Authorization: Bearer $EXECUTOR_TOKEN" \
  'http://localhost:8080/api/notifications?size=50' | python3 -c "
import sys, json
from collections import Counter
data = json.load(sys.stdin)
overdue = [n['targetId'] for n in data['items'] if n['type'] == 'TASK_OVERDUE']
dupes = {k: v for k, v in Counter(overdue).items() if v > 1}
if dupes:
    print(f'FAIL: duplicate TASK_OVERDUE: {dupes}')
else:
    print(f'PASS: {len(overdue)} unique TASK_OVERDUE notifications, no duplicates')
"
```

**Expected**: No duplicate TASK_OVERDUE for the same targetId.

## Verification Checklist

- [ ] `make backend-test-docker` — all tests pass
- [ ] `make docker-up-detached && make health` — stack healthy
- [ ] TASK_ASSIGNED appears after assigning executor
- [ ] STATUS_CHANGED appears after status transition (to creator, not self)
- [ ] TASK_OVERDUE appears for overdue tasks (executor + creator)
- [ ] No duplicate TASK_OVERDUE after repeated job runs
- [ ] Error in notification does not break main operation
