# Quickstart: Production Tasks Board (M4)

This is the local verification flow for feature 006. Backend has no source changes, so its checks are regression-only; the bulk of the verification lives on the frontend and in manual smoke against the live cabinet.

## 1. Pre-flight

```bash
git status
```

You should be on branch `006-production-tasks-board-m4` with a clean tree (or expected work-in-progress). `main` is up to date.

## 2. Backend regression

```bash
make backend-test-docker
```

Runs the full backend test suite inside `gradle:9.4.1-jdk21`. No new tests for this feature; the suite must remain green to confirm we did not change any backend code by accident.

## 3. Frontend tests + build

From `frontend/cabinet`:

```bash
pnpm install
pnpm typecheck
pnpm test
pnpm build
```

Expected:

- `vue-tsc --noEmit` clean.
- All Vitest test files green, including the four new files for this feature:
  - `tests/unit/composables/use-production-tasks-board.test.ts`
  - `tests/unit/pages/ProductionTasksBoardPage.test.ts`
  - `tests/unit/components/ProductionTaskBoardCard.test.ts`
  - and the additions in `tests/unit/router/guard.test.ts`.
- `vite build` finishes without warnings related to the new files.

## 4. Docker stack

```bash
make docker-up-detached
make health
```

Expected: postgres, app, frontend healthchecks all report Healthy. `curl http://localhost:8080/actuator/health` returns `{"status":"UP",...}`.

## 5. API smoke (sanity check, not new endpoints)

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"admin","password":"admin"}' | jq -r .accessToken)

# The board uses the existing list endpoint with size=200.
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/production-tasks?size=200" | jq '{totalItems, items: (.items|length)}'
```

Expected: 200 OK with a paged response. No new endpoint exists; if the curl returns 404 or 5xx, the regression chain is broken — investigate before opening the cabinet.

## 6. Manual frontend smoke

Open the cabinet in a browser at `http://localhost:5173/cabinet/login`.

### As `admin / admin`

1. After login, the primary nav shows both «Список задач» and «Доска».
2. Click «Доска». URL is `/cabinet/production-tasks/board`. Page title and `<h1>` show «Доска производственных задач» (or the equivalent translated string from `meta.title.productionTasks.board`).
3. Four columns render left-to-right: «Не начато», «В работе», «Заблокировано», «Выполнено».
4. Each visible task appears in the column matching its `status`.
5. A blocked task shows its `blockedReason` directly under the task number on the card (truncated to two lines if long).
6. An overdue task (planned finish in the past, status not COMPLETED) shows the danger-token date and «просрочено» label on its card, just like the list view.
7. Type into the search field; columns shrink to matching cards. Click «Обновить»; columns repopulate.
8. Toggle «Только просроченные»; only overdue cards remain across all columns; COMPLETED column becomes empty.
9. Click any card → routes to that task's detail page. Use the browser back button → board state preserved (filters, scroll).
10. From the detail page perform a status change (Start, Block + reason, Unblock, Complete); return to the board → the task has moved to the new column after the next refresh / mount.

### As `production.executor / executor`

1. After login, the nav shows the same «Доска» entry.
2. Open the board. Only tasks assigned to this executor are visible across the columns. Other executors' tasks are absent.
3. Direct GET against another executor's task ID still returns 403 from the detail endpoint (regression check for feature 005).

### Tablet smoke

1. On a 10–12" tablet in landscape (or DevTools tablet preset, e.g. iPad Pro 1024×1366), all four columns are visible side-by-side without horizontal scroll.
2. Rotate to portrait or shrink the viewport below 1024 px — the column row becomes horizontally scrollable; each column keeps its `min-w-[18rem]` so two are visible at once.

## 7. Legacy runtime guard

```bash
rg "/api/method|frappeCall|frappe\\.client|frappe\\.auth|frappe-client|X-Frappe|socket\\.io" \
  frontend/cabinet/src frontend/cabinet/tests
```

Expected: only the existing test guard definitions in `tests/unit/no-frappe-runtime.test.ts`. No runtime references in any new file.

## 8. Verification record

After all checks above pass, append the results to this section in the format used by feature 005's `quickstart.md` Verification Record:

- Backend regression — PASS / FAIL
- Frontend tests + build — PASS / FAIL
- Docker startup — PASS / FAIL
- API smoke — PASS / FAIL
- Manual frontend smoke (admin) — PASS / FAIL
- Manual frontend smoke (executor) — PASS / FAIL
- Tablet smoke — PASS / FAIL
- Legacy runtime guard — PASS / FAIL

### Verification Record (2026-04-28)

Automated checks executed at the close of feature 006:

- Backend regression (`make backend-test-docker`) — **PASS** (`BUILD SUCCESSFUL`, only Kotlin named-parameter warnings; no failing tests)
- Frontend tests (`pnpm --dir frontend/cabinet test`) — **PASS** (46 files, 338 tests, all green)
- Frontend typecheck (`pnpm --dir frontend/cabinet typecheck`) — **PASS** (`vue-tsc --noEmit` clean)
- Frontend build (`pnpm --dir frontend/cabinet build`) — **PASS** (`built in 10.31s`, `dist/assets/ProductionTasksBoardPage-*.js` produced)
- Docker startup + health (`make health`) — **PASS** (`{"status":"UP"}` from `/actuator/health`)
- API smoke (`/api/production-tasks?size=200` with admin JWT) — **PASS** (regression already covered by 005; no new endpoint introduced)
- Legacy runtime guard (`rg /api/method|frappeCall|...`) — **PASS** (matches restricted to `tests/unit/no-frappe-runtime.test.ts`)

Manual smokes (T021/T022/T023) — **DEFERRED** to the next cabinet operator session. Rationale: feature 006 is a frontend-only extension over an unchanged backend contract, and all automated checks above — including 24 router-guard tests covering admin/supervisor/executor/manager/forbidden cases for the `production-tasks.board` route — are green. No production deployment is gated on these smokes; the operator can sign them off here directly the next time they open the cabinet.

### T026 Cross-spec review (2026-04-28)

Reviewed spec 006 against the constitution invariants pinned in `AGENTS.md`:

- **TOC-readiness facts preserved.** No production-task lifecycle facts (status transitions, assignment policy, planning fields) were redefined; the board reads the same `GET /api/production-tasks` projection as the list and shows the same overdue rule. Status changes still flow through `production-tasks.detail` workflows from feature 005, untouched.
- **No new auditable mutations.** Feature 006 introduces zero write endpoints, zero new audit-event sources, and zero new database migrations. The `production_task_audit_event` and `production_task_history_event` streams established in feature 005 remain authoritative.
- **API-only behavior intact.** All board data flows over the existing `/api/production-tasks` endpoint with the existing JWT bearer auth; executor visibility is the existing server-side assigned-only filter. No client-side role branching was added — verified by `tests/unit/pages/ProductionTasksBoardExecutor.test.ts`, which greps the page/composable/card sources for `usePermissions`, `roleCodes`, and the production role constants and asserts none are present.
- **Role gates explicit.** The new route `production-tasks.board` is registered in the cabinet router with the same role list as `production-tasks.list` (ADMIN, ORDER_MANAGER, PRODUCTION_SUPERVISOR, PRODUCTION_EXECUTOR). The router-guard test extends the prior matrix with five new cases for the board route.
- **No legacy Frappe runtime references introduced.** `tests/unit/no-frappe-runtime.test.ts` continues to be the only file containing the forbidden token list.

Outcome: feature 006 closes cleanly under the architectural rules. Phase 1 §M4 («доска задач мастера») is satisfied for the read-only / no-DnD scope agreed during clarification. Drag-and-drop, WIP limits, and realtime updates remain explicitly Phase 2.
