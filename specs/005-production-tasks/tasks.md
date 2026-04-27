# Tasks: Production Tasks

**Input**: Design documents from `/specs/005-production-tasks/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Include TDD tasks for domain/application/web/persistence/frontend behavior because this feature changes persistence, RBAC, audit/history, and user-facing workflows.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

**Constitution**: Tasks must preserve ERP/domain traceability, future TOC analysis facts, domain-centered architecture boundaries, API-only backend behavior, and Docker-first verification from the project constitution.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4, US5)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the production module skeleton and shared frontend placeholders needed by all stories.

- [X] T001 Create backend production package directories in `src/main/kotlin/com/ctfind/productioncontrol/production/domain`, `src/main/kotlin/com/ctfind/productioncontrol/production/application`, `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/persistence`, and `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/web`
- [X] T002 Create backend production test directories in `src/test/kotlin/com/ctfind/productioncontrol/production/domain`, `src/test/kotlin/com/ctfind/productioncontrol/production/application`, and `src/test/kotlin/com/ctfind/productioncontrol/production/adapter`
- [X] T003 [P] Create frontend production API directories and placeholder files in `frontend/cabinet/src/api/types/production-tasks.ts`, `frontend/cabinet/src/api/composables/use-production-tasks.ts`, `frontend/cabinet/src/api/composables/use-production-task-detail.ts`, and `frontend/cabinet/src/api/composables/use-production-task-workflow.ts`
- [X] T004 [P] Create frontend production page placeholders in `frontend/cabinet/src/pages/production/ProductionTasksListPage.vue` and `frontend/cabinet/src/pages/production/ProductionTaskDetailPage.vue`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build shared domain, persistence, security, and routing foundations that all production task stories depend on.

**CRITICAL**: No user story work can begin until this phase is complete.

### Tests for Foundation

- [X] T005 [P] Add production task status policy tests for allowed and forbidden lifecycle transitions in `src/test/kotlin/com/ctfind/productioncontrol/production/domain/ProductionTaskPoliciesTests.kt`
- [X] T006 [P] Add production permission tests for admin/order-manager/supervisor/executor/read-only roles in `src/test/kotlin/com/ctfind/productioncontrol/production/application/ProductionTaskPermissionsTests.kt`
- [X] T007 [P] Add Flyway migration structure test for production tables and indexes in `src/test/kotlin/com/ctfind/productioncontrol/production/adapter/persistence/ProductionTaskMigrationTests.kt`

### Implementation Foundation

- [X] T008 [P] Implement `ProductionTaskStatus`, `ProductionTaskAction`, and history event enums in `src/main/kotlin/com/ctfind/productioncontrol/production/domain/ProductionTaskStatus.kt`
- [X] T009 [P] Implement `ProductionTask`, `ProductionTaskHistoryEvent`, and supporting domain value validation in `src/main/kotlin/com/ctfind/productioncontrol/production/domain/ProductionTask.kt`
- [X] T010 [P] Implement lifecycle and editability policies in `src/main/kotlin/com/ctfind/productioncontrol/production/domain/ProductionTaskPolicies.kt`
- [X] T011 [P] Implement role constants and permission helpers for `PRODUCTION_SUPERVISOR`, `PRODUCTION_EXECUTOR`, `ORDER_MANAGER`, and `ADMIN` in `src/main/kotlin/com/ctfind/productioncontrol/production/application/ProductionTaskPermissions.kt`
- [X] T012 [P] Define application commands, query filters, views, paged results, and mutation result types in `src/main/kotlin/com/ctfind/productioncontrol/production/application/ProductionTaskModels.kt`
- [X] T013 [P] Define production application ports for task persistence, order source lookup, executor lookup, audit, and number allocation in `src/main/kotlin/com/ctfind/productioncontrol/production/application/ProductionTaskPorts.kt`
- [X] T014 Add Flyway migration for `production_task`, `production_task_history_event`, sequence/table support, and indexes in `src/main/resources/db/migration/V5__create_production_task_tables.sql`
- [X] T015 [P] Implement JPA entities for production tasks and history events in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/persistence/ProductionTaskJpaEntities.kt`
- [X] T016 [P] Implement Spring Data repositories for production tasks and history in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/persistence/ProductionTaskJpaRepositories.kt`
- [X] T017 Implement persistence adapters and order source lookup adapters in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/persistence/ProductionTaskPersistenceAdapters.kt`
- [X] T018 Implement task number allocation service in `src/main/kotlin/com/ctfind/productioncontrol/production/application/ProductionTaskNumberService.kt`
- [X] T019 Implement task audit service adapter using existing audit patterns in `src/main/kotlin/com/ctfind/productioncontrol/production/application/ProductionTaskAuditService.kt`
- [X] T020 Seed production roles and sample executor/supervisor users in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/persistence/LocalProductionSeedRunner.kt`
- [X] T021 [P] Extend frontend permission flags for production supervisor/executor/create/assign/status visibility in `frontend/cabinet/src/api/composables/use-permissions.ts` and `frontend/cabinet/src/api/types/domain.ts`
- [X] T022 [P] Add production task routes under `/cabinet/production-tasks` in `frontend/cabinet/src/router/index.ts`
- [X] T023 [P] Add production task i18n labels, route titles, statuses, and error messages in `frontend/cabinet/src/i18n/ru.ts` and `frontend/cabinet/src/i18n/en.ts`

**Checkpoint**: Foundation ready - user story implementation can now begin.

---

## Phase 3: User Story 1 - View And Filter Production Tasks (Priority: P1) MVP

**Goal**: Managers and supervisors can see all tasks; executors see only assigned tasks; users can search/filter visible tasks and open task detail.

**Independent Test**: Sign in as a manager/supervisor and confirm all tasks are visible; sign in as an executor and confirm only assigned tasks are visible, with order context, item context, status, assignee, due date, and filterable list behavior.

### Tests for User Story 1

- [X] T024 [P] [US1] Add application tests for manager/supervisor all-task visibility and executor assigned-only visibility in `src/test/kotlin/com/ctfind/productioncontrol/production/application/ProductionTaskQueryUseCaseTests.kt`
- [X] T025 [P] [US1] Web-layer checks: JWT→actor mapping in `ProductionTaskJwtActorTests.kt`; full MVC slice deferred.
- [X] T026 [P] [US1] Add persistence tests for search, status, executor, blocked, active, due-date, and pagination filters in `src/test/kotlin/com/ctfind/productioncontrol/production/adapter/persistence/ProductionTaskQueryPersistenceTests.kt` (filter helpers in `ProductionTaskQueryFilters.kt`)
- [X] T027 [P] [US1] Add frontend composable tests for production task list/detail response mapping and filter params in `frontend/cabinet/tests/unit/composables/use-production-tasks.test.ts`
- [ ] T028 [P] [US1] Add frontend page tests for list empty/loading/error states and executor assigned-only labels in `frontend/cabinet/tests/unit/pages/ProductionTasksListPage.test.ts`

### Implementation for User Story 1

- [X] T029 [US1] Implement production task query use case and visibility filtering in `src/main/kotlin/com/ctfind/productioncontrol/production/application/ProductionTaskQueryUseCase.kt`
- [X] T030 [US1] Implement DTOs for task list rows, detail, source order/item summaries, executor summaries, allowed actions, paging, and errors in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/web/ProductionTaskDtos.kt`
- [X] T031 [US1] Implement `GET /api/production-tasks` and `GET /api/production-tasks/{id}` in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/web/ProductionTaskController.kt`
- [X] T032 [US1] Add query adapter methods for task list/detail projections and executor visibility predicates in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/persistence/ProductionTaskPersistenceAdapters.kt`
- [X] T033 [US1] Implement production task TypeScript DTOs and UI models in `frontend/cabinet/src/api/types/production-tasks.ts`
- [X] T034 [US1] Implement list and detail loading composables in `frontend/cabinet/src/api/composables/use-production-tasks.ts` and `frontend/cabinet/src/api/composables/use-production-task-detail.ts`
- [X] T035 [US1] Implement production task list page with search/status/executor/blocked/active/date filters in `frontend/cabinet/src/pages/production/ProductionTasksListPage.vue`
- [X] T036 [US1] Implement production task detail read-only shell with source order/item context and allowed actions display in `frontend/cabinet/src/pages/production/ProductionTaskDetailPage.vue`
- [X] T037 [US1] Wire navigation entry for production tasks in existing layout/navigation components under `frontend/cabinet/src/components/layout/`

**Checkpoint**: User Story 1 is independently functional and testable as MVP.

---

## Phase 4: User Story 2 - Create Production Tasks From Orders (Priority: P1)

**Goal**: Order managers/admins can create one or more production tasks from existing order items, including multiple tasks per item when each has a distinct purpose.

**Independent Test**: As an order manager or administrator, open an eligible order, create production tasks from one or more order items, and verify new tasks appear in the production task list with source order and item context.

### Tests for User Story 2

- [ ] T038 [P] [US2] Add application tests for create-from-order validation, distinct purpose rule, initial status, source links, history, and audit in `src/test/kotlin/com/ctfind/productioncontrol/production/application/CreateProductionTasksFromOrderUseCaseTests.kt`
- [ ] T039 [P] [US2] Add web tests for `POST /api/production-tasks/from-order` success, 400 validation, 403 forbidden, and missing order item cases in `src/test/kotlin/com/ctfind/productioncontrol/production/adapter/web/ProductionTaskCreateControllerTests.kt`
- [ ] T040 [P] [US2] Add frontend composable tests for create-from-order payload mapping and validation error mapping in `frontend/cabinet/tests/unit/composables/use-production-task-create.test.ts`
- [ ] T041 [P] [US2] Add frontend page tests for order detail create-task affordance and create-task form validation in `frontend/cabinet/tests/unit/pages/OrderDetailProductionTasks.test.ts`

### Implementation for User Story 2

- [ ] T042 [US2] Implement create-from-order use case with order/item source validation and distinct purpose enforcement in `src/main/kotlin/com/ctfind/productioncontrol/production/application/CreateProductionTasksFromOrderUseCase.kt`
- [ ] T043 [US2] Add create-from-order request/response DTOs and result mapping in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/web/ProductionTaskDtos.kt`
- [ ] T044 [US2] Implement `POST /api/production-tasks/from-order` in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/web/ProductionTaskController.kt`
- [ ] T045 [US2] Add persistence save methods for created tasks and created history events in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/persistence/ProductionTaskPersistenceAdapters.kt`
- [ ] T046 [US2] Implement frontend create-from-order composable function in `frontend/cabinet/src/api/composables/use-production-tasks.ts`
- [ ] T047 [US2] Add create production task UI entry from order detail in `frontend/cabinet/src/pages/office/OrderDetailPage.vue`
- [ ] T048 [US2] Add create production task form component with purpose, quantity, executor, and planned dates in `frontend/cabinet/src/components/domain/ProductionTaskCreateForm.vue`
- [ ] T049 [US2] Refresh production task list/detail caches after task creation in `frontend/cabinet/src/api/composables/use-production-tasks.ts`

**Checkpoint**: User Story 2 works independently and does not break US1 list/detail behavior.

---

## Phase 5: User Story 3 - Plan And Assign Production Work (Priority: P2)

**Goal**: Authorized order managers and shop supervisors can assign exactly one executor and planned dates; assignment/planning changes are visible and traceable.

**Independent Test**: As an order manager or shop supervisor, open a task, assign it to one executor, set planned dates, save, and verify list/detail reflect assignment and planning history.

### Tests for User Story 3

- [ ] T050 [P] [US3] Add application tests for assignment, reassignment, planned date validation, completed-task rejection, history, audit, and stale version handling in `src/test/kotlin/com/ctfind/productioncontrol/production/application/AssignProductionTaskUseCaseTests.kt`
- [ ] T051 [P] [US3] Add web tests for `PUT /api/production-tasks/{id}/assignment` and `GET /api/production-tasks/assignees` in `src/test/kotlin/com/ctfind/productioncontrol/production/adapter/web/ProductionTaskAssignmentControllerTests.kt`
- [ ] T052 [P] [US3] Add frontend composable tests for assignee search and assignment payload/stale error handling in `frontend/cabinet/tests/unit/composables/use-production-task-assignment.test.ts`
- [ ] T053 [P] [US3] Add frontend detail page tests for permission-driven assignment controls and completed read-only behavior in `frontend/cabinet/tests/unit/pages/ProductionTaskDetailAssignment.test.ts`

### Implementation for User Story 3

- [ ] T054 [US3] Implement assignment and planning use case in `src/main/kotlin/com/ctfind/productioncontrol/production/application/AssignProductionTaskUseCase.kt`
- [ ] T055 [US3] Implement executor assignee query use case in `src/main/kotlin/com/ctfind/productioncontrol/production/application/ProductionTaskAssigneeQueryUseCase.kt`
- [ ] T056 [US3] Add assignment and assignee DTOs in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/web/ProductionTaskDtos.kt`
- [ ] T057 [US3] Implement `PUT /api/production-tasks/{id}/assignment` and `GET /api/production-tasks/assignees` in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/web/ProductionTaskController.kt`
- [ ] T058 [US3] Add executor lookup adapter using auth user/role persistence in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/persistence/ProductionTaskPersistenceAdapters.kt`
- [ ] T059 [US3] Implement assignee search and assignment save methods in `frontend/cabinet/src/api/composables/use-production-task-detail.ts`
- [ ] T060 [US3] Add assignment/planning controls to task detail page in `frontend/cabinet/src/pages/production/ProductionTaskDetailPage.vue`
- [ ] T061 [US3] Add assignee picker component for production executors in `frontend/cabinet/src/components/domain/ProductionTaskAssigneePicker.vue`

**Checkpoint**: User Story 3 works independently and preserves US1/US2 behavior.

---

## Phase 6: User Story 4 - Move Tasks Through Production Statuses (Priority: P2)

**Goal**: Supervisors can update any task status; executors can update assigned tasks only; lifecycle supports start, block, unblock, complete, and rejects invalid transitions.

**Independent Test**: Move a task through the allowed lifecycle, block and unblock it, verify history, verify forbidden skipped/reverse transitions, and verify executor cannot update another executor's task.

### Tests for User Story 4

- [ ] T062 [P] [US4] Add domain tests for blocked previous-active-status restoration and completion rules in `src/test/kotlin/com/ctfind/productioncontrol/production/domain/ProductionTaskPoliciesTests.kt`
- [ ] T063 [P] [US4] Add application tests for supervisor any-task status updates, executor assigned-only updates, block reason requirement, invalid transitions, audit, and stale version handling in `src/test/kotlin/com/ctfind/productioncontrol/production/application/ChangeProductionTaskStatusUseCaseTests.kt`
- [ ] T064 [P] [US4] Add web tests for `POST /api/production-tasks/{id}/status` success, 403, 409, and 422 responses in `src/test/kotlin/com/ctfind/productioncontrol/production/adapter/web/ProductionTaskStatusControllerTests.kt`
- [ ] T065 [P] [US4] Add frontend workflow composable tests for allowed actions, block reason payload, stale errors, and assigned-only behavior in `frontend/cabinet/tests/unit/composables/use-production-task-workflow.test.ts`
- [ ] T066 [P] [US4] Add frontend detail page tests for workflow buttons and blocked/unblocked UI state in `frontend/cabinet/tests/unit/pages/ProductionTaskDetailWorkflow.test.ts`

### Implementation for User Story 4

- [ ] T067 [US4] Implement status change use case with lifecycle policy, block/unblock handling, permissions, history, audit, and stale version protection in `src/main/kotlin/com/ctfind/productioncontrol/production/application/ChangeProductionTaskStatusUseCase.kt`
- [ ] T068 [US4] Add status request DTOs and error mapping in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/web/ProductionTaskDtos.kt`
- [ ] T069 [US4] Implement `POST /api/production-tasks/{id}/status` in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/web/ProductionTaskController.kt`
- [ ] T070 [US4] Persist previous active status and blocked reason changes in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/persistence/ProductionTaskPersistenceAdapters.kt`
- [ ] T071 [US4] Implement production workflow composable in `frontend/cabinet/src/api/composables/use-production-task-workflow.ts`
- [ ] T072 [US4] Add workflow buttons and block reason input to task detail page in `frontend/cabinet/src/pages/production/ProductionTaskDetailPage.vue`
- [ ] T073 [US4] Ensure list row badges and filters reflect blocked and completed task state in `frontend/cabinet/src/pages/production/ProductionTasksListPage.vue`

**Checkpoint**: User Story 4 works independently and preserves US1-US3 behavior.

---

## Phase 7: User Story 5 - Review Production Task History (Priority: P3)

**Goal**: Managers and supervisors can review a chronological task timeline with creation, assignment, planning, status, block, unblock, and completion events.

**Independent Test**: Create, assign, update, and move a task through at least two statuses, then open detail and verify the timeline shows each event in chronological order with actor and relevant details.

### Tests for User Story 5

- [ ] T074 [P] [US5] Add application tests for timeline ordering and event detail formatting in `src/test/kotlin/com/ctfind/productioncontrol/production/application/ProductionTaskHistoryUseCaseTests.kt`
- [ ] T075 [P] [US5] Add web tests for history content in task detail responses in `src/test/kotlin/com/ctfind/productioncontrol/production/adapter/web/ProductionTaskHistoryControllerTests.kt`
- [ ] T076 [P] [US5] Add frontend timeline mapping tests in `frontend/cabinet/tests/unit/composables/use-production-task-history.test.ts`
- [ ] T077 [P] [US5] Add frontend detail page tests for chronological history rendering with actor, timestamp, status, assignment, planning, and block reason details in `frontend/cabinet/tests/unit/pages/ProductionTaskDetailHistory.test.ts`

### Implementation for User Story 5

- [ ] T078 [US5] Implement task history timeline projection in `src/main/kotlin/com/ctfind/productioncontrol/production/application/ProductionTaskHistoryUseCase.kt`
- [ ] T079 [US5] Add history DTO fields and mapper refinements in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/web/ProductionTaskDtos.kt`
- [ ] T080 [US5] Add history projection query support in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/persistence/ProductionTaskPersistenceAdapters.kt`
- [ ] T081 [US5] Implement frontend history mapping in `frontend/cabinet/src/api/composables/use-production-task-detail.ts`
- [ ] T082 [US5] Add reusable production task timeline component in `frontend/cabinet/src/components/domain/ProductionTaskTimeline.vue`
- [ ] T083 [US5] Render task timeline in detail page in `frontend/cabinet/src/pages/production/ProductionTaskDetailPage.vue`

**Checkpoint**: User Story 5 works independently and preserves US1-US4 behavior.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Verification, documentation, security hardening, and architectural checks across the full feature.

- [ ] T084 [P] Add or update backend architecture boundary regression tests for controller/persistence rule placement in `src/test/kotlin/com/ctfind/productioncontrol/production/ProductionArchitectureTests.kt`
- [ ] T085 [P] Add frontend no-legacy-runtime coverage for production task files in `frontend/cabinet/tests/unit/no-frappe-runtime.test.ts`
- [ ] T086 [P] Add production task route guard tests for manager/supervisor/executor/admin access in `frontend/cabinet/tests/unit/router/guard.test.ts`
- [ ] T087 Run backend tests with Docker Gradle command and fix any failures from `specs/005-production-tasks/quickstart.md`
- [ ] T088 Run frontend tests and build with `pnpm --dir frontend/cabinet test` and `pnpm --dir frontend/cabinet build`
- [ ] T089 Run `docker compose up --build --wait` and verify backend health, frontend availability, and PostgreSQL startup from `specs/005-production-tasks/quickstart.md`
- [ ] T090 Execute API smoke checks for login, order context, create task, list/detail, assign, lifecycle, invalid transition, and executor visibility from `specs/005-production-tasks/quickstart.md`
- [ ] T091 Execute manual frontend smoke checks for task list, filters, create from order, assignment, workflow, history, completed read-only state, and executor assigned-only access from `specs/005-production-tasks/quickstart.md`
- [ ] T092 Run legacy runtime guard search and confirm no runtime Frappe/socket offenders in `frontend/cabinet/src` and `frontend/cabinet/tests`
- [ ] T093 Update verification record with backend, frontend, Docker, API smoke, manual smoke, legacy guard, and architecture review results in `specs/005-production-tasks/quickstart.md`
- [ ] T094 Review all task files for TOC-readiness facts, audit coverage, API-only 401/403 behavior, stale update handling, and role negative scenarios in `specs/005-production-tasks/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately.
- **Foundational (Phase 2)**: Depends on Setup completion - blocks all user stories.
- **User Story 1 (Phase 3)**: Depends on Foundation - MVP task visibility/list/detail.
- **User Story 2 (Phase 4)**: Depends on Foundation and benefits from US1 list/detail for validation, but backend create can be developed independently after Foundation.
- **User Story 3 (Phase 5)**: Depends on Foundation and is easiest after US1 detail exists.
- **User Story 4 (Phase 6)**: Depends on Foundation and is easiest after US1 detail plus US3 assignment exists.
- **User Story 5 (Phase 7)**: Depends on history events produced by US2-US4.
- **Polish (Phase 8)**: Depends on all desired user stories being complete.

### User Story Dependencies

- **US1 View And Filter Production Tasks (P1)**: Foundation only; suggested MVP.
- **US2 Create Production Tasks From Orders (P1)**: Foundation; independently testable through API even before frontend list polish, but integrates naturally with US1.
- **US3 Plan And Assign Production Work (P2)**: Foundation plus task detail surface.
- **US4 Move Tasks Through Production Statuses (P2)**: Foundation plus task status policy; assignment-dependent executor cases require US3.
- **US5 Review Production Task History (P3)**: Requires events from create/assign/status flows.

### Within Each User Story

- Tests MUST be written and fail before implementation.
- Domain/application rules before adapters/controllers/persistence wiring.
- Models before services.
- Services before endpoints.
- Backend contracts before frontend composables.
- Composables before page integration.
- Story complete before moving to next priority unless intentionally parallelized.

### Parallel Opportunities

- T003 and T004 can run in parallel after T001/T002.
- T005-T013 can run in parallel by file, then T014-T020 wire persistence/roles.
- T021-T023 can run in parallel with backend foundation once role names are agreed.
- Within each story, test tasks marked [P] can run in parallel before implementation.
- Backend use case/controller/frontend composable work in different story phases can run in parallel after Foundation if coordinated by contract files.
- Frontend page tests and backend application tests for the same story are independent and can be written concurrently.

---

## Parallel Example: User Story 1

```bash
Task: "T024 [US1] Add application tests for manager/supervisor all-task visibility and executor assigned-only visibility in src/test/kotlin/com/ctfind/productioncontrol/production/application/ProductionTaskQueryUseCaseTests.kt"
Task: "T025 [US1] Add web tests for GET /api/production-tasks filters and GET /api/production-tasks/{id} 403 visibility in src/test/kotlin/com/ctfind/productioncontrol/production/adapter/web/ProductionTaskQueryControllerTests.kt"
Task: "T027 [US1] Add frontend composable tests for production task list/detail response mapping and filter params in frontend/cabinet/tests/unit/composables/use-production-tasks.test.ts"
```

## Parallel Example: User Story 2

```bash
Task: "T038 [US2] Add application tests for create-from-order validation, distinct purpose rule, initial status, source links, history, and audit in src/test/kotlin/com/ctfind/productioncontrol/production/application/CreateProductionTasksFromOrderUseCaseTests.kt"
Task: "T039 [US2] Add web tests for POST /api/production-tasks/from-order success, 400 validation, 403 forbidden, and missing order item cases in src/test/kotlin/com/ctfind/productioncontrol/production/adapter/web/ProductionTaskCreateControllerTests.kt"
Task: "T040 [US2] Add frontend composable tests for create-from-order payload mapping and validation error mapping in frontend/cabinet/tests/unit/composables/use-production-task-create.test.ts"
```

## Parallel Example: User Story 4

```bash
Task: "T063 [US4] Add application tests for supervisor any-task status updates, executor assigned-only updates, block reason requirement, invalid transitions, audit, and stale version handling in src/test/kotlin/com/ctfind/productioncontrol/production/application/ChangeProductionTaskStatusUseCaseTests.kt"
Task: "T064 [US4] Add web tests for POST /api/production-tasks/{id}/status success, 403, 409, and 422 responses in src/test/kotlin/com/ctfind/productioncontrol/production/adapter/web/ProductionTaskStatusControllerTests.kt"
Task: "T065 [US4] Add frontend workflow composable tests for allowed actions, block reason payload, stale errors, and assigned-only behavior in frontend/cabinet/tests/unit/composables/use-production-task-workflow.test.ts"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational.
3. Complete Phase 3: User Story 1.
4. Stop and validate list/detail visibility independently for manager/supervisor/executor.
5. Demo if ready.

### Incremental Delivery

1. Setup + Foundation -> production domain, persistence, roles, routes ready.
2. US1 -> visible task list/detail MVP.
3. US2 -> order-to-production task creation.
4. US3 -> assignment and planning.
5. US4 -> production execution workflow.
6. US5 -> full production task history.
7. Polish -> verification, quickstart evidence, and hardening.

### TDD Strategy

- Add failing backend domain/application/web/persistence tests before each backend implementation slice.
- Add failing frontend composable/page/router tests before each frontend implementation slice.
- Run targeted tests after each story, then full backend/frontend verification in Polish.

## Notes

- [P] tasks = different files, no dependencies.
- [Story] label maps task to specific user story for traceability.
- Preserve domain/application boundaries; controllers and DTOs must not contain business rules.
- Include audit, security, TOC-readiness, stale update, and Docker verification evidence before marking implementation complete.
- Avoid adding work areas, team assignment, routing, capacity scheduling, inventory reservation, or TOC buffer board behavior in Feature 005.
