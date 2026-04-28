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

Verification run on 2026-04-28 against branch `005-production-tasks`.

- **Backend tests (T087)**: PASS. `make backend-test-docker` (uses
  `gradle:9.4.1-jdk21`). All test classes compile and pass; warnings
  about `ProductionOrderSourcePort` named-parameter shadowing in test
  stubs are not failures.
- **Frontend tests (T088)**: PASS. `pnpm --dir frontend/cabinet test`
  → 40 test files / 278 tests green, including new T028, T040–T041,
  T052–T053, T065–T066, T074–T077, T085, T086.
- **Frontend build (T088)**: PASS. `pnpm --dir frontend/cabinet build`
  via `vue-tsc --noEmit && vite build` clean.
- **Docker startup (T089)**: PASS. `make docker-up-detached` brings
  postgres, app, frontend up with all three healthchecks reporting
  Healthy. Backend builds with `gradle:9.4.1-jdk21` builder stage
  (replaces older eclipse-temurin builder that re-downloaded the
  gradle wrapper distribution per build).
- **API smoke checks (T090)**: PASS. Verified end-to-end against the
  live Docker stack:
  - login `admin/admin` → `/api/auth/login` returns Bearer JWT with
    role `ADMIN`.
  - `GET /api/orders?size=1` → fetches an existing order.
  - `POST /api/production-tasks/from-order` → 201 with
    `taskNumber=PT-000002`.
  - `GET /api/production-tasks/{id}` → 200 with full detail and
    enriched history (CREATED, ASSIGNED with previous/new executor and
    planning before/after).
  - `PUT /api/production-tasks/{id}/assignment` → 200 with version+1
    and ASSIGNED history entry naming the executor and planning dates.
  - `POST /api/production-tasks/{id}/status` → 200 for legal
    transitions (NOT_STARTED→IN_PROGRESS, IN_PROGRESS→BLOCKED,
    BLOCKED→IN_PROGRESS, IN_PROGRESS→COMPLETED). 422 with
    `invalid_task_status_transition` for BLOCKED→COMPLETED skip
    attempt.
  - Block stores `previousActiveStatus=IN_PROGRESS` + `blockedReason`;
    unblock clears both and preserves the unblock note.
  - Completed task returns `allowedActions=[]`.
  - Bogus `orderId` → 400 `validation_failed` with `details.orderId`.
  - Missing task → 404. Missing token → 401. Executor session sees
    only their assigned task; 403 with `forbidden` code on tasks not
    assigned to them.
  - Bug fixed during smoke: detail/list endpoints needed
    `@Transactional(readOnly=true)` because `spring.jpa.open-in-view=
    false` made `order.customer.displayName` lazy-load fail. Fix is
    in `ProductionTaskQueryUseCase`.
- **Manual frontend smoke (T091)**: Pending — requires browser session
  at `http://localhost:5173/cabinet/login`. Use steps in §9 above.
- **Legacy runtime search (T092)**: PASS. `rg "/api/method|frappeCall|
  frappe.client|frappe.auth|frappe-client|X-Frappe|socket.io"
  frontend/cabinet/src frontend/cabinet/tests` returns only the guard
  definitions inside `tests/unit/no-frappe-runtime.test.ts`, which
  intentionally lists those patterns as forbidden. No runtime call
  sites.
- **Architecture review (T084)**: PASS. `ProductionArchitectureTests`
  asserts: domain has no Spring/JPA imports; application avoids JPA
  entities, web/persistence packages, and other modules' adapters;
  controller wires all five use cases and never touches repositories.

## Final Spec Review (T094)

Cross-check across `spec.md`, `plan.md`, `data-model.md`,
`contracts/production-tasks-api.md`,
`contracts/frontend-production-tasks.md`, and `tasks.md`:

- **TOC-readiness facts.** `data-model.md` keeps status,
  `previousActiveStatus` for blocked interrupts, `blockedReason`,
  `executorUserId`, `plannedStartDate`, `plannedFinishDate`,
  `createdAt`, `updatedAt`, `version`, plus history events with
  `previousExecutorUserId` / `newExecutorUserId` and planning
  before/after dates. The DTO + persistence layers preserve all of
  these (verified via T079, T080, smoke `PT-000002` history).
  Work-area/team/capacity are explicitly out of scope and the model
  stays open to future extension.
- **Audit coverage.** Each successful path emits a paired audit +
  history record:
  - create → `PRODUCTION_TASK_CREATED` audit + `CREATED` history
    (covered by T038, T042–T045, T074, T075).
  - assign / plan → `PRODUCTION_TASK_ASSIGNED` /
    `PRODUCTION_TASK_PLANNING_UPDATED` audit + `ASSIGNED` /
    `PLANNING_UPDATED` history (T050, T078, T074).
  - status → `PRODUCTION_TASK_STATUS_*` audit + `STATUS_CHANGED` /
    `BLOCKED` / `UNBLOCKED` / `COMPLETED` history (T063, T067, T074).
  - failed validations, forbidden writes, invalid transitions, and
    stale writes do not mutate or audit (T038, T050, T063 negatives).
- **API-only 401/403.** Confirmed in tests and live smoke:
  - 401 from unauthenticated `/api/production-tasks` request
    (verified T090).
  - 403 from executors hitting tasks not assigned to them
    (controller tests T039/T051/T064 + smoke).
  - 403 from non-creator role on `POST /from-order`,
    non-assigner role on `PUT /assignment`, non-status role on
    `POST /status` (T039, T051, T064).
- **Stale update handling.** 409 + `stale_production_task_version`
  on mismatched `expectedVersion` for assignment and status updates;
  no mutation occurs (T050, T051, T063, T064; frontend toast
  exercised in T052 / T065).
- **Role negative scenarios.** Application-layer tests assert:
  - executor can only update / view tasks assigned to them
    (T063, T024 visibility, smoke).
  - viewer-style role forbidden from create / assign / status
    (T039, T051, T064).
  - completed tasks reject assign / replan / status changes
    (T050 completed-task validation, T063 completed-task validation).
  - block requires reason, BLOCKED→COMPLETED skip rejected as
    `invalid_task_status_transition` (T063, smoke 422).
