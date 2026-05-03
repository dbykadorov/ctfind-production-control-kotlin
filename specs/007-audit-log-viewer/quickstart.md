# Quickstart: Журнал действий администратора (Phase 1 §8 #10)

This is the local verification flow for feature 007. Both backend and frontend have source changes; the backend introduces a new `audit` module (read-only over existing tables) and a `GET /api/users` endpoint in the auth module.

## 1. Pre-flight

```bash
git status
```

You should be on branch `007-audit-log-viewer` with a clean tree (or expected work-in-progress). `main` is up to date.

## 2. Backend tests

```bash
make backend-test-docker
```

Runs the full backend test suite inside `gradle:9.4.1-jdk21`. New tests for this feature:

- `AuditLogQueryUseCaseTests` — ADMIN-only gate, query delegation, pagination
- `AuditControllerTests` — HTTP 200/401/403, filter param binding, response shape
- `AuditPersistenceAdapterTests` — 3-table merge, date filtering, search, actor filter, sort order, pagination

All tests must be green.

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
- All Vitest test files green, including new files for this feature:
  - `tests/unit/composables/use-audit-log.test.ts`
  - `tests/unit/pages/AuditLogPage.test.ts`
  - `tests/unit/pages/AuditLogPageFilters.test.ts`
  - `tests/unit/pages/AuditLogPageForbidden.test.ts`
  - `tests/unit/components/AuditActorPicker.test.ts`
  - additions in `tests/unit/router/guard.test.ts`
- `vite build` finishes without warnings related to the new files.

## 4. Docker stack

```bash
make docker-up-detached
make health
```

Expected: postgres, app, frontend healthchecks all report Healthy. `curl http://localhost:8080/actuator/health` returns `{"status":"UP",...}`.

## 5. API smoke

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"admin","password":"admin"}' | jq -r .accessToken)

# Audit endpoint — admin should get 200
curl -s -o /dev/null -w '%{http_code}' \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/audit"
# Expected: 200

# Audit endpoint — response shape
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/audit?size=5" | jq '{totalItems, totalPages, page, itemCount: (.items|length)}'
# Expected: JSON with totalItems >= 0, page = 0

# Audit endpoint with category filter
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/audit?category=AUTH&size=5" | jq '{totalItems, page}'

# Users endpoint — admin should get 200
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/users?limit=5" | jq '.[0] | {id, login, displayName}'
# Expected: user object

# Non-admin should get 403
EXEC_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"production.executor","password":"executor"}' | jq -r .accessToken)

curl -s -o /dev/null -w '%{http_code}' \
  -H "Authorization: Bearer $EXEC_TOKEN" \
  "http://localhost:8080/api/audit"
# Expected: 403
```

## 6. Manual frontend smoke

Open the cabinet in a browser at `http://localhost:5173/cabinet/login`.

### As `admin / admin`

1. After login, the sidebar shows «Журнал действий» entry (in the admin section).
2. Click «Журнал действий». URL is `/cabinet/audit`. Page title shows «Журнал действий».
3. A table renders with columns: Время, Категория, Событие, Кто, Описание, and a link column.
4. Events are sorted by time descending (newest first). Default range is last 7 days.
5. Events from at least AUTH category are visible (login events from seed data).
6. Click on a row with category ORDER → navigates to order detail page. Back button returns to audit.
7. Click on a row with category PRODUCTION_TASK → navigates to task detail page.
8. AUTH rows have no clickable link.
9. Change date range filter → table updates with matching events.
10. Select a single category in the multi-select → only events of that category shown.
11. Select an actor in the picker → only events from that actor shown.
12. Type in search box → table filters after debounce (300ms).
13. Click «Сбросить» → all filters return to defaults, table repopulates.
14. If more than 50 events → pagination shows page controls. Navigate between pages.
15. Apply a filter while on page 2+ → page resets to 1.
16. Click «Обновить» → table reloads with current filters.

### As `production.executor / executor`

1. After login, sidebar does NOT show «Журнал действий».
2. Navigate directly to `/cabinet/audit` → forbidden state shown, no audit data visible.

### As `production.supervisor / supervisor`

1. Sidebar does NOT show «Журнал действий».
2. Direct URL `/cabinet/audit` → forbidden state.

### Tablet smoke

1. On a 10-12" tablet in landscape (or DevTools tablet preset), the page displays correctly.
2. Filter panel wraps or collapses on narrow viewports.
3. Table scrolls horizontally if columns don't fit.

## 7. Legacy runtime guard

```bash
rg "/api/method|frappeCall|frappe\\.client|frappe\\.auth|frappe-client|X-Frappe|socket\\.io" \
  frontend/cabinet/src frontend/cabinet/tests
```

Expected: only the existing test guard definitions in `tests/unit/no-frappe-runtime.test.ts`. No runtime references in any new file.

## 8. Verification record

After all checks above pass, append the results to this section:

- Backend tests — DEFERRED on 2026-05-03. Owner: Manual QA. Reason: original Docker-specific `make backend-test-docker` was blocked by a root-owned Gradle lock. Evidence available: root `make backend-test` passed during Phase 1 closeout. Sign-off impact: Docker-specific backend-test evidence remains a follow-up before release-candidate sign-off.
- Frontend tests + build — PASS (387 tests / 51 files; typecheck clean; vite build 15.56s)
- Docker startup — PASS on 2026-05-03. Evidence: root `make docker-up-detached` completed and `make health` returned UP during Phase 1 closeout.
- API smoke (admin 200, executor 403) — DEFERRED on 2026-05-03. Owner: Manual QA. Reason: audit-specific role smoke was not rerun during documentation alignment. Sign-off impact: role-smoke evidence remains a follow-up before release-candidate sign-off.
- Manual frontend smoke (admin) — DEFERRED on 2026-05-03. Owner: Manual QA. Target: next cabinet manual QA session. Sign-off impact: admin audit-page UX smoke remains a release-candidate follow-up.
- Manual frontend smoke (executor / supervisor) — DEFERRED on 2026-05-03. Owner: Manual QA. Target: next cabinet manual QA session. Sign-off impact: non-admin audit exclusion smoke remains a release-candidate follow-up.
- Tablet smoke — DEFERRED on 2026-05-03. Owner: Manual QA. Target: next tablet/responsive QA pass. Sign-off impact: tablet layout smoke remains a release-candidate follow-up.
- Legacy runtime guard — PASS (only `tests/unit/no-frappe-runtime.test.ts` matches)
