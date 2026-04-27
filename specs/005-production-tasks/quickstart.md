# Quickstart: Production Tasks

This quickstart verifies Feature 005 after implementation.

## 1. Start Local Runtime

From the repository root:

```bash
docker compose up --build --wait
```

Expected services:

- Backend API on `http://localhost:8080`
- Frontend cabinet on `http://localhost:5173/cabinet/`
- PostgreSQL exposed on host port `15432`

Check backend health:

```bash
curl http://localhost:8080/actuator/health
```

Expected response includes `"status":"UP"`.

## 2. Log In

Open:

```text
http://localhost:5173/cabinet/
```

Use local credentials:

```text
login: admin
password: admin
```

Expected:

- Login succeeds.
- The cabinet opens without session-expired overlay.
- Production task pages are reachable.

## 3. Prepare Token

```bash
TOKEN="$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"admin","password":"admin"}' | jq -r '.accessToken')"
```

## 4. Smoke Test Order Context

List orders and choose an order/item for task creation:

```bash
curl -s "http://localhost:8080/api/orders?size=10" \
  -H "Authorization: Bearer ${TOKEN}" | jq
```

Expected:

- Response includes at least one order with one or more items available through order detail.
- The selected order and item ids can be used as production task source context.

## 5. Smoke Test Production Task Creation

Create a production task from an order item:

```bash
curl -s -X POST http://localhost:8080/api/production-tasks/from-order \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{
    "orderId": "REPLACE_WITH_ORDER_ID",
    "tasks": [
      {
        "orderItemId": "REPLACE_WITH_ORDER_ITEM_ID",
        "purpose": "Раскрой",
        "quantity": 1,
        "uom": "шт",
        "plannedStartDate": "2026-05-01",
        "plannedFinishDate": "2026-05-03"
      }
    ]
  }' | jq
```

Expected:

- Response status is `201`.
- Returned task has status `NOT_STARTED`.
- Returned task has a unique `taskNumber`.
- Returned task has `version`.

## 6. Smoke Test Production Task List And Detail

List production tasks:

```bash
curl -s "http://localhost:8080/api/production-tasks?size=10" \
  -H "Authorization: Bearer ${TOKEN}" | jq
```

Load task detail:

```bash
curl -s "http://localhost:8080/api/production-tasks/REPLACE_WITH_TASK_ID" \
  -H "Authorization: Bearer ${TOKEN}" | jq
```

Expected:

- List row includes task number, order number, customer, item, purpose, status, assignee, planned dates, and version.
- Detail includes source order/item context, allowed actions, and history.

## 7. Smoke Test Assignment

Search executor assignees:

```bash
curl -s "http://localhost:8080/api/production-tasks/assignees?limit=10" \
  -H "Authorization: Bearer ${TOKEN}" | jq
```

Assign a task:

```bash
curl -s -X PUT "http://localhost:8080/api/production-tasks/REPLACE_WITH_TASK_ID/assignment" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{
    "expectedVersion": 0,
    "executorUserId": "REPLACE_WITH_EXECUTOR_USER_ID",
    "plannedStartDate": "2026-05-01",
    "plannedFinishDate": "2026-05-03",
    "note": "Планирование из quickstart"
  }' | jq
```

Expected:

- Response status is `200`.
- Task has exactly one executor.
- History includes assignment/planning event.
- Version increments.

## 8. Smoke Test Status Workflow

Start work:

```bash
curl -s -X POST "http://localhost:8080/api/production-tasks/REPLACE_WITH_TASK_ID/status" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{
    "expectedVersion": 1,
    "toStatus": "IN_PROGRESS",
    "note": "Started from quickstart"
  }' | jq
```

Block work:

```bash
curl -s -X POST "http://localhost:8080/api/production-tasks/REPLACE_WITH_TASK_ID/status" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{
    "expectedVersion": 2,
    "toStatus": "BLOCKED",
    "reason": "Нет материала"
  }' | jq
```

Unblock back to active status:

```bash
curl -s -X POST "http://localhost:8080/api/production-tasks/REPLACE_WITH_TASK_ID/status" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{
    "expectedVersion": 3,
    "toStatus": "IN_PROGRESS",
    "note": "Материал получен"
  }' | jq
```

Complete:

```bash
curl -s -X POST "http://localhost:8080/api/production-tasks/REPLACE_WITH_TASK_ID/status" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{
    "expectedVersion": 4,
    "toStatus": "COMPLETED",
    "note": "Работа завершена"
  }' | jq
```

Expected:

- Each response status is `200`.
- History includes start, block, unblock, and complete events.
- Completed task no longer exposes regular workflow actions.

Attempt an invalid skipped transition:

```bash
curl -i -X POST "http://localhost:8080/api/production-tasks/REPLACE_WITH_NEW_TASK_ID/status" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{
    "expectedVersion": 0,
    "toStatus": "COMPLETED"
  }'
```

Expected:

- Response is `422`.
- Existing task status remains unchanged.

## 9. Smoke Test Frontend

In the cabinet UI:

1. Open the production task list.
2. Confirm tasks appear with task number, source order, customer, item, purpose, status, assignee, and planned dates.
3. Search by task/order/customer/item/purpose.
4. Filter by status, blocked-only, active-only, executor, and date range.
5. Create tasks from an existing order item with distinct purposes.
6. Open a task detail and confirm source order/item, assignment, workflow actions, history, and versioned update behavior.
7. Assign a task to one executor and verify assignment history.
8. Move the task through `не начато -> в работе -> заблокировано -> в работе -> выполнено`.
9. Confirm completed task regular actions are read-only.
10. Confirm executor users see and can update only assigned tasks.

## 10. Verification Commands

Backend:

```bash
docker run --rm \
  -e GRADLE_USER_HOME=/tmp/gradle-home \
  -v "$PWD":/workspace \
  -w /workspace \
  eclipse-temurin:21-jdk \
  ./gradlew --project-cache-dir /tmp/gradle-project-cache test
```

Frontend:

```bash
pnpm --dir frontend/cabinet test
pnpm --dir frontend/cabinet build
```

Legacy runtime guard:

```bash
rg "/api/method|frappeCall|frappe.client|frappe.auth|frappe-client|X-Frappe|socket.io" frontend/cabinet/src frontend/cabinet/tests
```

Expected:

- Backend tests pass.
- Frontend tests pass.
- Frontend build passes.
- Docker startup succeeds.
- Legacy runtime search returns no runtime offenders except intentional test guard definitions if present.

## Verification Record

Fill this section after executing [`tasks.md`](./tasks.md) verification tasks (around T087–T093). Until then, entries remain pending by design.

- Backend tests: Pending until implementation.
- Frontend tests: Pending until implementation.
- Frontend build: Pending until implementation.
- Docker startup: Pending until implementation.
- API smoke checks: Pending until implementation.
- Manual frontend smoke: Pending until implementation.
- Legacy runtime search: Pending until implementation.
