---

description: "Task list for feature 007 — Журнал действий администратора (Phase 1 §8 #10)"
---

# Tasks: Журнал действий администратора (Audit Log Viewer)

**Input**: Design documents from `/specs/007-audit-log-viewer/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/backend-audit-api.md, contracts/frontend-audit-log.md, quickstart.md

**Tests**: Explicitly specified in contracts — test tasks are included per user story (TDD: tests before implementation).

**Organization**: Tasks are grouped by user story (US1 / US2 / US3). Backend introduces a new `audit` module (read-only over existing tables) and extends the `auth` module with a user lookup endpoint. Frontend adds a new page, composable, component, route, and nav entry.

**Constitution**: Tasks preserve ERP/domain traceability (read-only over existing audit streams, no new mutations), TOC-readiness (no new state transitions), domain-centered architecture (new hexagonal `audit` module, no business rules in controllers), API-only backend (ADMIN-only, 403/401 explicit), Docker-first verification.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies).
- **[Story]**: Story label (US1 / US2 / US3) only on user-story phase tasks.
- File paths shown are relative to repo root.

## Path Conventions

- Backend: `src/main/kotlin/com/ctfind/productioncontrol/...`
- Backend tests: `src/test/kotlin/com/ctfind/productioncontrol/...`
- Frontend: `frontend/cabinet/src/...`
- Frontend tests: `frontend/cabinet/tests/unit/...`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: I18n keys and TS types needed before any user-story page or composable work.

- [X] T001 [P] Add i18n keys for audit page in `frontend/cabinet/src/i18n/ru.ts` and `frontend/cabinet/src/i18n/en.ts`: `meta.title.audit`, `nav.audit`, and the `audit.*` namespace per `contracts/frontend-audit-log.md` I18n Keys section
- [X] T002 [P] Create TypeScript types in `frontend/cabinet/src/api/types/audit-log.ts`: `AuditCategory`, `AuditLogRowResponse`, `AuditLogPageResponse`, `AuditLogFilters`, `UserSummaryResponse` per `contracts/frontend-audit-log.md` TypeScript Types section

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Backend audit module skeleton and frontend route so that US1 tests and implementation can begin.

**⚠️ CRITICAL**: User story phases cannot start until this phase is complete.

- [X] T003 [P] Create backend audit module domain + application layer: `AuditCategory` enum and `AuditLogRow` in `src/main/kotlin/com/ctfind/productioncontrol/audit/domain/AuditLogModels.kt`; `AuditLogQuery`, `AuditLogPageResult` in `src/main/kotlin/com/ctfind/productioncontrol/audit/application/AuditLogModels.kt`; `AuditLogQueryPort` interface in `src/main/kotlin/com/ctfind/productioncontrol/audit/application/AuditLogPorts.kt` — per `data-model.md` entity definitions
- [X] T004 Create frontend stub page `frontend/cabinet/src/pages/audit/AuditLogPage.vue` (renders `<h1>` from `meta.title.audit` i18n key) and register route `audit.list` at path `audit` in `frontend/cabinet/src/router/index.ts` with `meta.roles: ['ADMIN']` and `meta.title: 'meta.title.audit'`

**Checkpoint**: Foundation ready — route resolves, audit models exist, US1 can begin.

---

## Phase 3: User Story 1 — Администратор видит сводный журнал действий за неделю (Priority: P1) 🎯 MVP

**Goal**: ADMIN opens `/cabinet/audit` and sees a paginated table of unified audit events from the last 7 days across AUTH, ORDER, and PRODUCTION_TASK categories, sorted by time descending. Clicking an ORDER or PRODUCTION_TASK row navigates to the entity's detail page.

**Independent Test**: Sign in as `admin/admin`, open «Журнал действий». Table shows events from all three categories for the last 7 days. Click ORDER row → order detail. Click PRODUCTION_TASK row → task detail. AUTH rows have no link. Empty state shown if no events. Refresh button reloads data.

### Tests for User Story 1

- [X] T005 [P] [US1] Add backend use case tests in `src/test/kotlin/com/ctfind/productioncontrol/audit/application/AuditLogQueryUseCaseTests.kt`: ADMIN role → delegates to port with query, non-ADMIN → throws/returns forbidden, pagination params forwarded, result mapped correctly
- [X] T006 [P] [US1] Add backend controller tests in `src/test/kotlin/com/ctfind/productioncontrol/audit/adapter/web/AuditControllerTests.kt`: GET /api/audit returns 200 with correct JSON shape for ADMIN JWT, returns 403 for non-ADMIN JWT, returns 401 without JWT, query params (from, to, category, actorUserId, search, page, size) bind correctly to use case call, response has items/page/size/totalItems/totalPages
- [X] T007 [P] [US1] Add backend persistence adapter tests in `src/test/kotlin/com/ctfind/productioncontrol/audit/adapter/persistence/AuditPersistenceAdapterTests.kt`: 3-table merge produces unified rows, date-range filtering limits results, category filtering skips excluded tables, actorUserId filtering, search ILIKE on summary + targetId + actorLogin, sort by occurredAt DESC, in-memory pagination (correct page slice + totalItems), auth event summary generation from event_type + outcome + login per R-005, actor display name fallback chain (display_name → login → placeholder)
- [X] T008 [P] [US1] Add frontend composable tests in `frontend/cabinet/tests/unit/composables/use-audit-log.test.ts`: default GET /api/audit with from/to (7-day range) and page=0 size=50, filter params forwarded (category repeated, search trimmed, empty omitted), 403 → error.kind='forbidden' + data null, network error → error.kind='error', abort on re-fetch (CanceledError swallowed), scope dispose aborts in-flight request
- [X] T009 [P] [US1] Add frontend page tests in `frontend/cabinet/tests/unit/pages/AuditLogPage.test.ts`: table renders all 6 columns (Время, Категория, Событие, Кто, Описание, link), loading skeleton when data===null && loading, empty state «Событий за выбранный период нет» when items empty, error banner + «Обновить» button on error, forbidden state on error.kind='forbidden', RouterLink for ORDER rows → orders.detail, RouterLink for PRODUCTION_TASK rows → production-tasks.detail, no link for AUTH rows, refresh button calls refetch
- [X] T010 [P] [US1] Extend router-guard tests in `frontend/cabinet/tests/unit/router/guard.test.ts` with `audit.list` cases: ADMIN → audit.list (allowed), isAdmin=true bypass → audit.list (allowed), ORDER_MANAGER → forbidden, PRODUCTION_SUPERVISOR → forbidden, PRODUCTION_EXECUTOR → forbidden, unauthenticated → login

### Implementation for User Story 1

- [X] T011 [P] [US1] Implement backend persistence adapter in `src/main/kotlin/com/ctfind/productioncontrol/audit/adapter/persistence/AuditPersistenceAdapter.kt`: implements `AuditLogQueryPort`, uses `EntityManager` with native SQL queries to fetch from `auth_audit_event`, `order_audit_event`, `production_task_audit_event` with LEFT JOIN `app_user` for actor display name/login, applies date-range WHERE clause at SQL level, generates auth event summaries from event_type+outcome+login per R-005, applies search ILIKE and actorUserId filter in-memory, merges all results, sorts by occurredAt DESC, paginates in-memory, returns `AuditLogPageResult`
- [X] T012 [P] [US1] Implement backend use case in `src/main/kotlin/com/ctfind/productioncontrol/audit/application/AuditLogQueryUseCase.kt`: accepts authenticated actor with role codes, checks ADMIN role (throws/returns forbidden if not ADMIN), delegates to `AuditLogQueryPort.search(query)`, returns `AuditLogPageResult`
- [X] T013 [US1] Implement backend controller + DTOs in `src/main/kotlin/com/ctfind/productioncontrol/audit/adapter/web/AuditController.kt` and `src/main/kotlin/com/ctfind/productioncontrol/audit/adapter/web/AuditDtos.kt`: `@GetMapping("/api/audit")` accepting `@RequestParam` for from, to, category (List), actorUserId, search, page (default 0), size (default 50); extracts JWT principal; maps to `AuditLogQuery`; calls use case; maps `AuditLogPageResult` to `AuditLogPageResponse` with `AuditLogRowResponse` items per `contracts/backend-audit-api.md`
- [X] T014 [US1] Implement frontend composable in `frontend/cabinet/src/api/composables/use-audit-log.ts`: `useAuditLog()` returning `{ data, loading, error, refetch }` per `contracts/frontend-audit-log.md`, GET /api/audit with filter params via httpClient, AbortController for cancellation, 403 → forbidden error mapping, default 7-day from/to
- [X] T015 [US1] Replace T004 stub with full implementation of `frontend/cabinet/src/pages/audit/AuditLogPage.vue`: wire `useAuditLog`, render table with columns (Время, Категория, Событие, Кто, Описание, target link), RouterLink for ORDER → orders.detail and PRODUCTION_TASK → production-tasks.detail (no link for AUTH), loading skeleton, empty state, error banner + «Обновить», forbidden state, refresh button calling refetch, default 7-day date range computed on mount
- [X] T016 [US1] Add navigation entry «Журнал действий» in `frontend/cabinet/src/components/layout/Sidebar.vue`: visible only when `permissions.value.isAdmin` is true, icon ScrollText from lucide-vue-next, route `/cabinet/audit`, i18n key `nav.audit`, placed after production tasks entries

**Checkpoint**: US1 is the MVP — ADMIN can view the unified audit feed and navigate to entity detail pages. US2 adds filters + pagination controls. US3 verifies non-ADMIN exclusion.

---

## Phase 4: User Story 2 — Администратор сужает выборку фильтрами (Priority: P2)

**Goal**: Add the full filter panel (date range, category multi-select, actor picker, search input) and pagination controls on top of the basic feed from US1. Any filter change resets pagination to page 1. «Сбросить» returns all filters to defaults.

**Independent Test**: With the feed from US1, change each filter individually (date range, category, actor, search) and verify the table updates. Apply multiple filters, click «Сбросить» → defaults restored. Navigate between pages via pagination controls. Change a filter while on page 2+ → page resets to 1.

### Tests for User Story 2

- [X] T017 [P] [US2] Add frontend filter panel tests in `frontend/cabinet/tests/unit/pages/AuditLogPageFilters.test.ts`: date-from and date-to inputs bound to filter state, category multi-select toggles filter, actor picker emits actorUserId filter change, search input debounced (300ms), any filter change resets page to 0, «Сбросить» button clears all filters to defaults (7-day range, all categories, no actor, no search, page 0), pagination controls (next/prev/page indicator) visible when totalPages > 1, page change calls refetch with new page number
- [X] T018 [P] [US2] Add frontend actor picker component tests in `frontend/cabinet/tests/unit/components/AuditActorPicker.test.ts`: renders search input, calls fetchUsers on input with debounce (300ms), shows dropdown with displayName + login, emits update:modelValue with user ID on selection, clear button emits null, disabled prop disables input
- [X] T019 [P] [US2] Add backend user search tests: `src/test/kotlin/com/ctfind/productioncontrol/auth/application/UserQueryUseCaseTests.kt` for ADMIN-only gate + delegation; `src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserControllerTests.kt` for GET /api/users returning 200 with UserSummaryResponse[] for ADMIN, 403 for non-ADMIN, search param filters by login/displayName, limit param caps results

### Implementation for User Story 2

- [X] T020 [P] [US2] Implement backend user search use case + port: add `UserQueryPort` interface to `src/main/kotlin/com/ctfind/productioncontrol/auth/application/AuthenticationPorts.kt` with `searchUsers(search: String?, limit: Int): List<UserSummary>`, add `UserSummary` data class to application models, create `src/main/kotlin/com/ctfind/productioncontrol/auth/application/UserQueryUseCase.kt` with ADMIN role check, implement `UserQueryPort` in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/persistence/AuthPersistenceAdapters.kt` querying `app_user` with ILIKE on login+display_name, sorted by display_name, limited
- [X] T021 [US2] Implement backend user controller in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserController.kt`: `@GetMapping("/api/users")` with search and limit params, JWT principal extraction, delegates to UserQueryUseCase, returns `List<UserSummaryResponse>`; add `UserSummaryResponse` to `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/web/AuthDtos.kt`
- [X] T022 [P] [US2] Implement frontend user search composable in `frontend/cabinet/src/api/composables/use-users-search.ts`: `fetchUsers(search?, limit?)` calling GET /api/users via httpClient, returns UserSummaryResponse[]
- [X] T023 [US2] Implement frontend actor picker component in `frontend/cabinet/src/components/domain/AuditActorPicker.vue`: props `{ modelValue: string | null, disabled?: boolean }`, emit `update:modelValue`, debounced search (300ms) calling fetchUsers, dropdown list with displayName + login, clear button, follows ProductionTaskAssigneePicker pattern
- [X] T024 [US2] Add filter panel and pagination controls to `frontend/cabinet/src/pages/audit/AuditLogPage.vue`: date-from/date-to inputs, category multi-select (checkboxes for AUTH/ORDER/PRODUCTION_TASK, default all), AuditActorPicker for actor filter, search input with 300ms debounce, «Сбросить» button resetting all filters to defaults, any filter change resets page to 0 and calls refetch, pagination controls (Назад/Вперёд/page indicator/totalItems display), tablet adaptation (filter panel wraps below lg:, table overflow-x-auto)

**Checkpoint**: US1 + US2 — ADMIN can view and filter the audit feed with full pagination. US3 verifies non-ADMIN exclusion.

---

## Phase 5: User Story 3 — Журнал недоступен ролям без прав (Priority: P3)

**Goal**: Confirm that non-ADMIN users cannot see the «Журнал действий» nav entry, cannot access the page, and get a proper forbidden state. Verify no client-side role branching was added — all access control is via server 403 + router guard + nav visibility.

**Independent Test**: Sign in as `production.executor / executor` — nav has no «Журнал действий». Direct URL `/cabinet/audit` → forbidden state, no data. Same for `production.supervisor` and `order.manager`. Backend GET /api/audit with non-ADMIN JWT → 403.

### Tests for User Story 3

- [X] T025 [P] [US3] Add frontend forbidden scenario tests in `frontend/cabinet/tests/unit/pages/AuditLogPageForbidden.test.ts`: mock composable to return error.kind='forbidden', mount page, verify forbidden empty state rendered, no table/data visible, no audit data in DOM; verify page/composable source files contain zero references to `roleCodes` / `usePermissions` / role constants (no client-side role branching — access control is server-side + router guard only)

### Implementation for User Story 3

- [X] T026 [US3] Verify by code review that `AuditLogPage.vue`, `use-audit-log.ts`, and `AuditActorPicker.vue` contain zero references to `roleCodes` / `permissions` / `usePermissions` / role constants — all access control is via: (1) backend 403 for non-ADMIN, (2) router guard `meta.roles: ['ADMIN']`, (3) nav visibility via `permissions.value.isAdmin` in Sidebar.vue. No production code change required if US1 + US2 were implemented correctly.

**Checkpoint**: All three user stories testable independently. Audit log viewer is feature-complete.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verification and final review.

- [X] T027 [P] Run `pnpm --dir frontend/cabinet typecheck` + `pnpm --dir frontend/cabinet test` + `pnpm --dir frontend/cabinet build`; fix any failures from the new files
- [ ] T028 [P] Run `make backend-test-docker` — BLOCKED: .gradle lock file owned by root; deferred to manual `make backend-test-docker` — full backend suite must be green including new audit + user controller tests
- [X] T029 [P] Run the legacy runtime guard search per `specs/007-audit-log-viewer/quickstart.md` §7; expect only the existing `tests/unit/no-frappe-runtime.test.ts` matches
- [ ] T030 Run `make docker-up-detached` and `make health`; verify the full stack starts Healthy after frontend build incorporates the new page and backend incorporates the new endpoints
- [ ] T031 Execute the manual frontend smoke for the admin role per `specs/007-audit-log-viewer/quickstart.md` §6 — **deferred to manual ops (op runs on next cabinet session)**
- [ ] T032 Execute the manual frontend smoke for the executor / supervisor roles per `specs/007-audit-log-viewer/quickstart.md` §6 — **deferred to manual ops**
- [ ] T033 Execute the tablet smoke per `specs/007-audit-log-viewer/quickstart.md` §6 — **deferred to manual ops**
- [X] T034 Update the Verification Record section in `specs/007-audit-log-viewer/quickstart.md` with PASS/FAIL for each step
- [X] T035 Cross-review the spec for residual ambiguity and constitution alignment (read-only over existing audit streams, no new auditable mutations, ADMIN-only API + route + nav, API-only backend, role gates explicit, §8 #10 closure) and append the review note to `specs/007-audit-log-viewer/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: Depends on Setup; audit models + route must exist before US1 tests run.
- **US1 (Phase 3)**: Depends on Foundational. MVP slice.
- **US2 (Phase 4)**: Depends on Foundational; depends on US1's `useAuditLog` (T014) and `AuditLogPage.vue` (T015) for filter panel insertion point. Backend user search (T020–T021) is independent of US1 and can start after Foundational.
- **US3 (Phase 5)**: Depends on Foundational + US1; mostly tests + code-review verification.
- **Polish (Phase 6)**: Depends on all desired user stories.

### User Story Dependencies

- **US1 P1**: Foundation only. MVP.
- **US2 P2**: Foundation + US1 frontend (extends the page and composable). Backend user search is independent.
- **US3 P3**: Foundation + US1 (purely verification of the non-ADMIN scenario).

### Within Each User Story

- Tests written first, intended to fail before implementation.
- Backend: persistence adapter + use case can be parallel (different files, both depend on T003 models); controller depends on use case.
- Frontend: composable → page → nav entry (sequential).

### Parallel Opportunities

- Phase 1: T001 / T002 in parallel (different files).
- Phase 2: T003 is parallel with T001/T002; T004 depends on T001 (i18n key for page title).
- Phase 3 tests: T005 / T006 / T007 / T008 / T009 / T010 all in parallel (different files).
- Phase 3 implementation: T011 (adapter) / T012 (use case) in parallel; T013 (controller) depends on T012; T014 (frontend composable) parallel with backend; T015 (page) depends on T014; T016 (nav) depends on T015.
- Phase 4 tests: T017 / T018 / T019 all in parallel (different files).
- Phase 4 implementation: T020 (backend user search) / T022 (frontend user composable) in parallel; T021 (user controller) depends on T020; T023 (actor picker) depends on T022; T024 (filter panel) depends on T023.
- Phase 5: T025 is a single test task; T026 is code review.
- Phase 6: T027 / T028 / T029 in parallel; T030 depends on T027+T028; T031–T033 deferred; T034 / T035 after all checks.

---

## Parallel Example: User Story 1

```bash
# Tests in parallel (all different files):
Task: "T005 [US1] backend use case tests"
Task: "T006 [US1] backend controller tests"
Task: "T007 [US1] backend persistence adapter tests"
Task: "T008 [US1] frontend composable tests"
Task: "T009 [US1] frontend page tests"
Task: "T010 [US1] frontend router guard tests"

# Backend implementation: adapter + use case in parallel, then controller:
Task: "T011 [US1] AuditPersistenceAdapter"
Task: "T012 [US1] AuditLogQueryUseCase"        # parallel with T011
# After T011 + T012 land:
Task: "T013 [US1] AuditController + AuditDtos"

# Frontend implementation: sequential composable → page → nav:
Task: "T014 [US1] useAuditLog composable"
Task: "T015 [US1] AuditLogPage"                 # after T014
Task: "T016 [US1] nav entry in Sidebar"          # after T015
```

---

## Implementation Strategy

### MVP First (US1 only)

1. Phase 1 + Phase 2 (Setup + Foundational).
2. Phase 3 US1: backend endpoint + frontend page with basic table.
3. Validate via `pnpm test`, `make backend-test-docker`, and a manual click-through against the dev server.
4. Demo / merge — at this point ADMIN can view the unified audit feed.

### Incremental Delivery

1. Ship US1 → MVP: ADMIN sees the feed with target links and refresh.
2. Add US2 → Rich filtering (dates, categories, actor picker, search) + pagination controls.
3. Add US3 → Confirm non-ADMIN exclusion; no UX divergence.
4. Phase 6 polish — verification record + final review.

### Parallel Team Strategy

- Setup + Foundational together.
- US1 backend: adapter + use case in parallel; controller after.
- US1 frontend: composable → page → nav (sequential).
- US2 backend (user search) can start in parallel with US1 frontend.
- US2 frontend: user composable → actor picker → filter panel (sequential).
- US3 can be picked up by anyone after US1 (tests + code review).

---

## Notes

- Backend uses `EntityManager` native SQL queries in the audit persistence adapter (R-001 in research.md) — no new JPA entities for audit tables, no entity mapping conflicts with existing modules.
- Auth event summaries are generated in the persistence adapter from `event_type` + `outcome` + `login` (R-005) since `auth_audit_event` has no `summary` column.
- The `GET /api/users` endpoint (T020–T021) lives in the auth module and follows the same pattern as the existing `ProductionTaskAssigneeQueryUseCase` but without role filtering.
- The three source tables have different timestamp column names: `occurred_at` (auth) vs `event_at` (order/production) — the adapter normalizes to `occurredAt`.
- No Flyway migration required (R-008) — all tables already exist from V2/V4/V5 migrations.
- Tests must fail before implementation (TDD discipline).
- Stop and validate at every checkpoint; commit after each task or logical group.
