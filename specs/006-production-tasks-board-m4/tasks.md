---

description: "Task list for feature 006 — Production Tasks Board (M4)"
---

# Tasks: Production Tasks Board (M4)

**Input**: Design documents from `/specs/006-production-tasks-board-m4/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/frontend-production-tasks-board.md, quickstart.md

**Tests**: Explicitly requested — the spec's *Тесты* section lists frontend page tests, composable tests, and a router-guard extension. Test tasks are included alongside implementation tasks per user story.

**Organization**: Tasks are grouped by user story (US1 / US2 / US3). Backend has zero source changes for this feature; all work is in `frontend/cabinet`.

**Constitution**: Tasks preserve ERP/domain traceability (no new mutations, history/audit untouched), TOC-readiness (status/planned dates/overdue/executor remain unfiltered on the API; only the COMPLETED column has a UI cap), domain-centered architecture (frontend extension only), API-only backend behavior (reuses existing JWT-protected endpoint), and Docker-first verification.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies).
- **[Story]**: Story label (US1 / US2 / US3) only on user-story phase tasks.
- File paths shown are relative to repo root.

## Path Conventions

- Frontend: `frontend/cabinet/src/...` and `frontend/cabinet/tests/unit/...`
- No backend source touched. `make backend-test-docker` runs as a regression check only.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: i18n keys and route shell needed before any user-story page work.

- [X] T001 [P] Add i18n key `meta.title.productionTasks.board` (and any related `nav.productionTasks.board` label used by the navigation entry) in `frontend/cabinet/src/i18n/ru.ts` and the corresponding `en.ts` (if present)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Route + page shell so the router-guard test for the board route can execute and so the navigation entry has a real target. No business logic yet — the page is a stub that gets the real implementation in US1.

**⚠️ CRITICAL**: User story phases cannot start until this phase is complete.

- [X] T002 Create stub page `frontend/cabinet/src/pages/production/ProductionTasksBoardPage.vue` that imports nothing project-specific yet — only renders an `<h1>` from the `meta.title.productionTasks.board` i18n key. Real implementation comes in T010.
- [X] T003 Register a new route `production-tasks.board` at path `production-tasks/board` in `frontend/cabinet/src/router/index.ts`, lazy-loading the stub page from T002. Use the same `meta.roles` allowlist as `production-tasks.list` (`Order Manager`, `Shop Supervisor`, `Executor`, `ORDER_MANAGER`, `PRODUCTION_SUPERVISOR`, `PRODUCTION_EXECUTOR`) and add `meta.title: 'meta.title.productionTasks.board'`.

**Checkpoint**: Foundation ready — route resolves to a non-empty page, role guard can be tested, and US1 can begin.

---

## Phase 3: User Story 1 - Shop supervisor sees task distribution at a glance (Priority: P1) 🎯 MVP

**Goal**: A supervisor (or admin / order manager) opens `/cabinet/production-tasks/board`, sees four status columns populated with cards for the visible production tasks, and can click any card to open the existing detail page.

**Independent Test**: Sign in as `admin/admin` (or production supervisor seed), seed at least one task in each of NOT_STARTED / IN_PROGRESS / BLOCKED / COMPLETED, open the board, and verify each task appears in the column matching its status with the same metadata as the list view. Click any card and confirm navigation to the detail page.

### Tests for User Story 1

- [X] T004 [P] [US1] Add composable tests covering: GET `/api/production-tasks?size=200` with filter params, grouping into all four `byStatus` keys (including empty arrays), COMPLETED window cap (last 7 days × max 30 by `updatedAt desc`), `truncated` flag when `totalItems > 200`, abort/CanceledError handling, 403 mapping to `error.kind = 'forbidden'`, in `frontend/cabinet/tests/unit/composables/use-production-tasks-board.test.ts`
- [X] T005 [P] [US1] Add component tests for `ProductionTaskBoardCard` covering: task number + purpose + order context render, executor display name vs «не назначен», planned-finish formatting + overdue badge rule (date < today AND status ≠ COMPLETED), `blockedReason` clamp-to-2-lines for status BLOCKED, no `blockedReason` rendering for non-BLOCKED tasks, in `frontend/cabinet/tests/unit/components/ProductionTaskBoardCard.test.ts`
- [X] T006 [P] [US1] Add page tests for `ProductionTasksBoardPage` covering: four-column structure in fixed left-to-right order with the Russian status labels, empty state per column, loading skeleton when `data === null && loading`, error banner on generic error, 403 → forbidden empty state, truncation banner appears when `data.truncated`, RouterLink wraps each card pointing to `production-tasks.detail`, refresh button calls `refetch`, in `frontend/cabinet/tests/unit/pages/ProductionTasksBoardPage.test.ts`
- [X] T007 [P] [US1] Extend router-guard test in `frontend/cabinet/tests/unit/router/guard.test.ts` with `production-tasks.board` cases for ADMIN role, isAdmin=true bypass, PRODUCTION_SUPERVISOR, PRODUCTION_EXECUTOR, ORDER_MANAGER (all → `production-tasks.board`), and a non-allowed role → `forbidden`

### Implementation for User Story 1

- [X] T008 [US1] Implement `useProductionTasksBoard` composable in `frontend/cabinet/src/api/composables/use-production-tasks-board.ts` per `contracts/frontend-production-tasks-board.md`: fetch with `size=200`, group by `status`, COMPLETED window cap, `truncated` flag, abort + scope dispose, error/forbidden mapping. Reuse helpers from `use-production-tasks.ts` where possible.
- [X] T009 [P] [US1] Implement `ProductionTaskBoardCard` component in `frontend/cabinet/src/components/domain/ProductionTaskBoardCard.vue` per `contracts/frontend-production-tasks-board.md`: props `{ row: ProductionTaskListRowResponse }`, layout per FR-003, BLOCKED reason clamped to 2 lines via Tailwind `line-clamp-2`, overdue rule shared with list view (extract a helper if needed).
- [X] T010 [US1] Replace the T002 stub with the full implementation of `ProductionTasksBoardPage.vue` in `frontend/cabinet/src/pages/production/ProductionTasksBoardPage.vue`: wire `useProductionTasksBoard`, render four columns with cards in the order NOT_STARTED, IN_PROGRESS, BLOCKED, COMPLETED, embed `ProductionTaskBoardCard` inside `RouterLink` to `production-tasks.detail`, manual «Обновить» button, truncation banner when `data.truncated`, forbidden empty state when `error.kind === 'forbidden'`, basic search input bound to `filters.search`, tablet-friendly layout (`overflow-x-auto` row + `min-w-[18rem]` columns below `lg:`).
- [X] T011 [US1] Add navigation entry «Доска» to the cabinet layout (existing component(s) under `frontend/cabinet/src/components/layout/`) so all four allowed roles see it next to «Список задач»; ordering: list first, then board.

**Checkpoint**: US1 is the MVP — supervisors and admins can use the board end-to-end. US2 (filters beyond search) and US3 (executor scenario) extend it.

---

## Phase 4: User Story 2 - Filter the board to focus on a slice of work (Priority: P2)

**Goal**: Add the full filter panel (executor picker, planned-finish date range, «только просроченные» toggle) on top of the basic search introduced in US1, with corresponding tests in dedicated files to avoid conflicting with US1 test files.

**Independent Test**: With ≥10 tasks across columns, apply each filter individually (executor pick, date range, overdue toggle) and verify columns shrink to exactly the matching subset while preserving column structure.

### Tests for User Story 2

- [X] T012 [P] [US2] Add page tests for the extended filter panel in a separate file `frontend/cabinet/tests/unit/pages/ProductionTasksBoardFilters.test.ts`: assert executor picker mounts, date-range inputs are bound, «только просроченные» toggle filters cards client-side, filters changes call `refetch` (debounced for search) without losing column structure.
- [X] T013 [P] [US2] Add composable tests for filter wiring + `overdueOnly` in a separate file `frontend/cabinet/tests/unit/composables/use-production-tasks-board-filters.test.ts`: server params include the active filter set, `overdueOnly` is applied client-side after grouping but before COMPLETED cap and yields an empty COMPLETED column by definition.

### Implementation for User Story 2

- [X] T014 [US2] Add the full filter panel (executor picker reusing `ProductionTaskAssigneePicker`, planned-finish from/to date inputs, «только просроченные» checkbox) to `frontend/cabinet/src/pages/production/ProductionTasksBoardPage.vue`. Persist filter state in component refs; re-fetch on change with the same debounce pattern used by `ProductionTasksListPage`.
- [X] T015 [US2] Add the `overdueOnly` client-side filter inside `frontend/cabinet/src/api/composables/use-production-tasks-board.ts`: after server response is grouped, drop non-overdue tasks across all columns when `overdueOnly` is true; the COMPLETED column ends up empty by the overdue rule.

**Checkpoint**: US1 + US2 — supervisors can narrow the board to any slice. US3 only extends executor visibility tests.

---

## Phase 5: User Story 3 - Executor view of the board (Priority: P3)

**Goal**: Confirm the executor sees the same four-column layout, populated only with their assigned tasks, and that no client-side role-aware logic was added (server-side filter from feature 005 is the only visibility gate).

**Independent Test**: Sign in as `production.executor / executor`, open the board, and verify only that executor's assigned tasks appear in the columns matching their statuses; other executors' tasks are absent. Direct GET against another executor's task ID still returns 403 from the detail endpoint (regression).

### Tests for User Story 3

- [X] T016 [P] [US3] Add page tests for the executor scenario in `frontend/cabinet/tests/unit/pages/ProductionTasksBoardExecutor.test.ts`: mock the composable to return a board response containing only assigned tasks across two columns, mount the page, verify all four columns render (with empty states for the two with no tasks), verify no UI element is hidden behind a role check (the page treats the data as authoritative), and verify the nav entry from T011 is reachable from an executor session via the existing guard tests.

### Implementation for User Story 3

- [X] T017 [US3] No production code change required — verify by code review that `ProductionTasksBoardPage.vue`, `ProductionTaskBoardCard.vue`, and `useProductionTasksBoard.ts` contain zero references to `roleCodes` / `permissions.canViewAllProductionTasks` / `usePermissions` (no client-side role branching). Add a brief comment in the page or composable noting that visibility is server-side only.

**Checkpoint**: All three user stories testable independently. Board is feature-complete.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verification and final review.

- [ ] T018 [P] Run `pnpm --dir frontend/cabinet typecheck` + `pnpm --dir frontend/cabinet test` + `pnpm --dir frontend/cabinet build`; fix any failures from the new files
- [ ] T019 [P] Run `make backend-test-docker` as regression — must remain green since no backend code changed
- [ ] T020 Run `make docker-up-detached` and `make health`; verify the full stack starts Healthy after the frontend build incorporates the new page
- [ ] T021 Execute the manual frontend smoke for the supervisor / admin role per `specs/006-production-tasks-board-m4/quickstart.md` §6
- [ ] T022 Execute the manual frontend smoke for the executor role per `specs/006-production-tasks-board-m4/quickstart.md` §6
- [ ] T023 Execute the tablet smoke per `specs/006-production-tasks-board-m4/quickstart.md` §6 (DevTools tablet preset or real 10–12" device in landscape)
- [ ] T024 [P] Run the legacy runtime guard search per `specs/006-production-tasks-board-m4/quickstart.md` §7; expect only the existing `tests/unit/no-frappe-runtime.test.ts` matches
- [ ] T025 Update the Verification Record section in `specs/006-production-tasks-board-m4/quickstart.md` with PASS/FAIL for each step
- [ ] T026 Cross-review the spec for residual ambiguity and constitution alignment (TOC readiness facts preserved, no new auditable mutations, API-only behavior intact, role gates explicit) and append the review note to `specs/006-production-tasks-board-m4/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: Depends on Setup; route + stub page must exist before US1 tests run.
- **US1 (Phase 3)**: Depends on Foundational. MVP slice.
- **US2 (Phase 4)**: Depends on Foundational; depends on US1's `useProductionTasksBoard` (T008) and `ProductionTasksBoardPage.vue` (T010) for the filter panel insertion point.
- **US3 (Phase 5)**: Depends on Foundational + US1; mostly tests + a code-review verification of the no-role-branch invariant.
- **Polish (Phase 6)**: Depends on all desired user stories.

### User Story Dependencies

- **US1 P1**: Foundation only.
- **US2 P2**: Foundation + US1 (extends the page and the composable).
- **US3 P3**: Foundation + US1 (purely verification of the executor scenario).

### Within Each User Story

- Tests written and intended to fail before implementation.
- Composable / component / page implementation can proceed in parallel (different files) until the page integrates the composable + component.
- Navigation entry (T011) is the last task of US1 to avoid lighting up the menu before the page works.

### Parallel Opportunities

- Phase 1 has only T001.
- Phase 2 tasks T002 / T003 are sequential (route imports the stub page).
- Phase 3 tests T004 / T005 / T006 / T007 can all run in parallel — different files.
- Phase 3 implementation: T009 (component) is parallel-able with T008 (composable); T010 (page) depends on both; T011 (nav entry) depends on T010.
- Phase 4 tests T012 / T013 in parallel (different files).
- Phase 5 has a single test task T016 plus a no-code verification T017.
- Phase 6: T018 / T019 / T024 in parallel; T020–T023 sequential against the live stack; T025 / T026 documentation last.

---

## Parallel Example: User Story 1

```bash
# Tests in parallel (different files):
Task: "T004 [US1] composable tests in frontend/cabinet/tests/unit/composables/use-production-tasks-board.test.ts"
Task: "T005 [US1] component tests in frontend/cabinet/tests/unit/components/ProductionTaskBoardCard.test.ts"
Task: "T006 [US1] page tests in frontend/cabinet/tests/unit/pages/ProductionTasksBoardPage.test.ts"
Task: "T007 [US1] router-guard tests in frontend/cabinet/tests/unit/router/guard.test.ts"

# Implementation: composable + component in parallel, then page integrates them:
Task: "T008 [US1] useProductionTasksBoard composable"
Task: "T009 [US1] ProductionTaskBoardCard component"   # parallel with T008
# After T008 + T009 land:
Task: "T010 [US1] ProductionTasksBoardPage"
Task: "T011 [US1] navigation entry"   # after T010
```

---

## Implementation Strategy

### MVP First (US1 only)

1. Phase 1 + Phase 2 (Setup + Foundational).
2. Phase 3 US1: composable, component, page, nav.
3. Validate via `pnpm test` and a manual click-through against the dev server.
4. Demo / merge — at this point supervisor and admin can use the board.

### Incremental Delivery

1. Ship US1 → MVP unblocks supervisor's at-a-glance view.
2. Add US2 → richer filtering for noisy datasets.
3. Add US3 → confirm executor scenario; no UX divergence.
4. Phase 6 polish — verification record + final review.

### Parallel Team Strategy

- Setup + Foundational together.
- US1 split across two devs: composable + component in parallel; page integrates.
- US2 starts when `useProductionTasksBoard` and `ProductionTasksBoardPage.vue` are stable.
- US3 can be picked up by anyone after US1 (it's tests + code review).

---

## Notes

- No backend source changes anywhere in the feature. `make backend-test-docker` runs only as a regression check.
- Tests must fail before implementation (TDD discipline). Source-text inspection is acceptable for navigation-entry tests where it would otherwise require a complex layout mount.
- Card content is the only place where the BLOCKED reason can be visible without further interaction (FR-016) — keep the truncate-to-two-lines rule consistent across page and component tests.
- COMPLETED column cap (last 7 days × max 30) lives in `useProductionTasksBoard` so both pages and tests see one source of truth (R-002 in research.md).
- Stop and validate at every checkpoint; commit after each task or logical group.
