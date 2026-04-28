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
