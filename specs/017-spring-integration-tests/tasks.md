# Tasks: Phase 1 Spring Integration Scenarios

**Input**: Design documents from `/specs/017-spring-integration-tests/`
**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: Tests are required because this feature is an integration test suite. Scenario tasks create Spring integration tests rather than production business behavior.

**Organization**: Tasks are grouped by user story so each scenario can be implemented and verified independently.

**Constitution**: Preserve domain/application boundaries, API-only backend behavior, explicit 401/403 semantics, traceable business-state changes, and Docker-backed verification.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add integration-test source-set support and root commands without changing the fast unit/slice test loop.

- [X] T001 Configure `integrationTest` source set, Testcontainers PostgreSQL dependencies, JUnit Platform task wiring, and `check` integration in production-control-api/build.gradle.kts
- [X] T002 [P] Add `backend-integration-test` and optional `backend-check` targets with help text in Makefile
- [X] T003 [P] Add integration-test Spring profile defaults for JWT, Flyway, JPA validation, and superadmin bootstrap in production-control-api/src/integrationTest/resources/application-integration-test.properties

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared Spring/Testcontainers/HTTP fixture support that every scenario depends on.

**CRITICAL**: No user story scenario should start until this phase is complete.

- [X] T004 Create `@SpringBootTest`, `@AutoConfigureMockMvc`, Testcontainers PostgreSQL, `@DynamicPropertySource`, database cleanup, and real-login token support in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/IntegrationTestSupport.kt
- [X] T005 Add MockMvc JSON request helpers, bearer-token helpers, response extraction, and common 401/403/409 assertions in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/IntegrationHttpDsl.kt
- [X] T006 Add scenario fixture helpers for creating users with canonical roles, customers, orders, materials, BOM lines, stock receipts, production tasks, and notifications through real application paths where required in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/ScenarioFixtures.kt
- [X] T007 [P] Add scenario coverage/residual-risk helper constants for all six scenario classes in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/ScenarioCoverage.kt
- [X] T008 Verify the empty integration source set compiles through the new Gradle task and record the expected command contract in specs/017-spring-integration-tests/quickstart.md

**Checkpoint**: Foundation ready; user story scenarios can now be implemented.

---

## Phase 3: User Story 1 - Auth and Users Security Integration (Priority: P1) MVP

**Goal**: Prove administrator bootstrap, real login, current-user lookup, and users API authorization work through the full Spring stack.

**Independent Test**: Run only `AuthUsersSecurityIntegrationTest` through the integration Gradle task and verify admin success, non-admin 403, and unauthenticated 401 behavior.

### Tests for User Story 1

- [X] T009 [US1] Create `AuthUsersSecurityIntegrationTest` with coverage/residual-risk note and clean database bootstrap assertion in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/AuthUsersSecurityIntegrationTest.kt
- [X] T010 [US1] Implement real `/api/auth/login` and `/api/auth/me` assertions for the bootstrapped `ADMIN` user in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/AuthUsersSecurityIntegrationTest.kt
- [X] T011 [US1] Implement `/api/users` success for `ADMIN` and 403 rejection for a non-admin actor created through the admin API in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/AuthUsersSecurityIntegrationTest.kt
- [X] T012 [US1] Implement unauthenticated 401 assertions for protected `/api/auth/me` and `/api/users` requests in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/AuthUsersSecurityIntegrationTest.kt

**Checkpoint**: Auth/users security scenario is independently runnable and satisfies MVP coverage.

---

## Phase 4: User Story 2 - Production Task Lifecycle Integration (Priority: P1) MVP

**Goal**: Prove order-to-production task creation, assignment, lifecycle, visibility, history, stale-version conflict, and security wiring.

**Independent Test**: Run only `ProductionTaskLifecycleIntegrationTest` and verify the task lifecycle plus executor isolation, stale version 409, and unauthenticated 401.

### Tests for User Story 2

- [X] T013 [US2] Create `ProductionTaskLifecycleIntegrationTest` with coverage/residual-risk note and minimum scenario actors for `ADMIN`, `PRODUCTION_SUPERVISOR`, and two `PRODUCTION_EXECUTOR` users in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/ProductionTaskLifecycleIntegrationTest.kt
- [X] T014 [US2] Implement order creation and `/api/production-tasks/from-order` flow with order linkage and supervisor/admin task visibility assertions in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/ProductionTaskLifecycleIntegrationTest.kt
- [X] T015 [US2] Implement assignment to executor and lifecycle assertions for `NOT_STARTED -> IN_PROGRESS -> BLOCKED -> IN_PROGRESS -> COMPLETED`, including detail/history events in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/ProductionTaskLifecycleIntegrationTest.kt
- [X] T016 [US2] Implement executor visibility and authorization checks proving an executor sees only assigned tasks and cannot read or mutate another executor's task in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/ProductionTaskLifecycleIntegrationTest.kt
- [X] T017 [US2] Implement stale expected-version 409 and unauthenticated 401 checks for production-task mutation/read endpoints in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/ProductionTaskLifecycleIntegrationTest.kt

**Checkpoint**: Production task lifecycle scenario is independently runnable and satisfies MVP coverage.

---

## Phase 5: User Story 3 - Warehouse, BOM, and Consumption Integration (Priority: P1) MVP

**Goal**: Prove materials, receipts, order BOM, consumption, usage totals, stock limits, shipped-order restrictions, and warehouse role checks work together.

**Independent Test**: Run only `WarehouseConsumptionIntegrationTest` and verify stock usage plus insufficient-stock, shipped-order, and unauthorized-role rejections.

### Tests for User Story 3

- [X] T018 [US3] Create `WarehouseConsumptionIntegrationTest` with coverage/residual-risk note and actors for `WAREHOUSE`, `ADMIN`, and an unauthorized role in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/WarehouseConsumptionIntegrationTest.kt
- [X] T019 [US3] Implement material creation, stock receipt, order creation, BOM line creation, partial consumption, and usage required/consumed/remaining assertions in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/WarehouseConsumptionIntegrationTest.kt
- [X] T020 [US3] Implement insufficient-stock rejection and post-failure stock/usage consistency assertions in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/WarehouseConsumptionIntegrationTest.kt
- [X] T021 [US3] Implement shipped-order restrictions for BOM mutation and material consumption after order status reaches `SHIPPED` in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/WarehouseConsumptionIntegrationTest.kt
- [X] T022 [US3] Implement WAREHOUSE/ADMIN allowed checks and non-warehouse/non-admin forbidden checks for warehouse operations in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/WarehouseConsumptionIntegrationTest.kt

**Checkpoint**: Warehouse/BOM/consumption scenario is independently runnable and MVP is complete.

---

## Phase 6: User Story 4 - Order Lifecycle Integration (Priority: P2)

**Goal**: Prove order create/list/detail/update/status lifecycle and shipped-order restrictions through the real application path.

**Independent Test**: Run only `OrderLifecycleIntegrationTest` and verify writer success, non-writer denial, shipped mutation rejection, and invalid transition rejection.

### Tests for User Story 4

- [X] T023 [US4] Create `OrderLifecycleIntegrationTest` with coverage/residual-risk note and actors for `ADMIN`, `ORDER_MANAGER`, and a non-writer role in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/OrderLifecycleIntegrationTest.kt
- [X] T024 [US4] Implement order create, list, detail, update, and version assertions for an authorized writer in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/OrderLifecycleIntegrationTest.kt
- [X] T025 [US4] Implement `NEW -> IN_WORK -> READY -> SHIPPED` status lifecycle and shipped-order edit rejection assertions in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/OrderLifecycleIntegrationTest.kt
- [X] T026 [US4] Implement non-writer create/update/status 403 checks and invalid status transition rejection in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/OrderLifecycleIntegrationTest.kt

**Checkpoint**: Order lifecycle follow-up scenario is independently runnable.

---

## Phase 7: User Story 5 - Notifications Integration (Priority: P2)

**Goal**: Prove production events create notifications for the correct recipients and notification read state is user-isolated.

**Independent Test**: Run only `NotificationsIntegrationTest` and verify assignment/status/overdue notifications, read-state transitions, duplicate overdue guard, and user isolation.

### Tests for User Story 5

- [X] T027 [US5] Create `NotificationsIntegrationTest` with coverage/residual-risk note and actors for supervisor/admin plus two executors in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/NotificationsIntegrationTest.kt
- [X] T028 [US5] Implement task assignment and status-change flows that assert `TASK_ASSIGNED` and `STATUS_CHANGED` notifications for the intended recipient in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/NotificationsIntegrationTest.kt
- [X] T029 [US5] Implement overdue task setup and overdue job trigger with `TASK_OVERDUE` assertion and duplicate-overdue guard in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/NotificationsIntegrationTest.kt
- [X] T030 [US5] Implement notification list, unread count, mark-read, mark-all-read, repeat-read idempotency, and User A/User B isolation checks in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/NotificationsIntegrationTest.kt

**Checkpoint**: Notifications follow-up scenario is independently runnable.

---

## Phase 8: User Story 6 - Audit Feed Integration (Priority: P2)

**Goal**: Prove auth, order, production, and inventory actions appear in the admin audit feed with real filters and admin-only security.

**Independent Test**: Run only `AuditFeedIntegrationTest` and verify admin feed data, filters, 403 for non-admin, 401 for unauthenticated, and explicit residual-risk note for unimplemented categories.

### Tests for User Story 6

- [X] T031 [US6] Create `AuditFeedIntegrationTest` with coverage/residual-risk note and actors for `ADMIN` and non-admin roles in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/AuditFeedIntegrationTest.kt
- [X] T032 [US6] Implement login, order, production, and inventory actions that produce real audit feed rows visible through `/api/audit` for `ADMIN` in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/AuditFeedIntegrationTest.kt
- [X] T033 [US6] Implement category, date range, actor, and search filter assertions against real `/api/audit` data in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/AuditFeedIntegrationTest.kt
- [X] T034 [US6] Implement non-admin 403, unauthenticated 401, and explicit assertion or residual-risk note for any missing Phase 1 audit category in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/AuditFeedIntegrationTest.kt

**Checkpoint**: Audit feed follow-up scenario is independently runnable.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, verification evidence, and suite-boundary checks.

- [X] T035 [P] Update integration scenario contract with implemented scenario class names, MVP/full-suite boundary, and residual-risk ownership in specs/017-spring-integration-tests/contracts/integration-scenarios.contract.md
- [X] T036 [P] Update backend integration command contract with final Gradle task name, Make target behavior, Docker/Testcontainers requirement, and failure expectations in specs/017-spring-integration-tests/contracts/backend-integration-command.contract.md
- [X] T037 Run `make backend-test` and record result, date, and any residual fast-test risk in specs/017-spring-integration-tests/quickstart.md
- [X] T038 Run `make backend-integration-test` and record scenario classes executed, result, date, and any Docker/Testcontainers environment notes in specs/017-spring-integration-tests/quickstart.md
- [X] T039 Verify the suite contains exactly 3 MVP scenario classes, no more than 6 planned scenario classes, and no broad unit-test matrix duplication; record the review in specs/017-spring-integration-tests/quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies; can start immediately.
- **Foundational (Phase 2)**: Depends on Setup completion; blocks all user story scenario work.
- **MVP User Stories (Phases 3-5)**: Depend on Foundational completion; can be implemented independently after shared helpers exist.
- **Follow-Up User Stories (Phases 6-8)**: Depend on Foundational completion; may reuse MVP fixtures but should remain independently runnable.
- **Polish (Phase 9)**: Depends on whichever scenarios are implemented for the current delivery increment.

### User Story Dependencies

- **US1 (P1 Auth and Users Security)**: Starts after Phase 2; no dependency on other stories.
- **US2 (P1 Production Task Lifecycle)**: Starts after Phase 2; uses order fixtures but should not depend on US4 implementation.
- **US3 (P1 Warehouse/BOM/Consumption)**: Starts after Phase 2; uses order fixtures but should not depend on US4 implementation.
- **US4 (P2 Order Lifecycle)**: Starts after Phase 2; validates order flow independently.
- **US5 (P2 Notifications)**: Starts after Phase 2; benefits from US2 fixture helpers but remains its own scenario.
- **US6 (P2 Audit Feed)**: Starts after Phase 2; creates its own minimal multi-module events.

### Within Each User Story

- Create the scenario class and coverage note first.
- Implement the happy path before negative/security/conflict checks.
- Authenticate actors through `/api/auth/login` except narrowly documented setup helpers.
- Verify HTTP response behavior and persisted state through public API paths where the behavior under test is runtime wiring.

---

## Parallel Opportunities

- T002 and T003 can run in parallel with T001 edits because they touch separate files, but validation waits for T001.
- T007 can run in parallel with T004-T006 after package path is agreed because it is a pure metadata/helper file.
- After Phase 2, US1, US2, and US3 can be implemented in parallel by separate owners because they write different scenario files.
- After Phase 2, US4, US5, and US6 can also be implemented in parallel by separate owners, with coordination around shared fixture helpers.
- T035 and T036 can run in parallel during polish because they update different contract documents.

---

## Parallel Example: MVP Scenario Work

```text
Task: "T009-T012 AuthUsersSecurityIntegrationTest in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/AuthUsersSecurityIntegrationTest.kt"
Task: "T013-T017 ProductionTaskLifecycleIntegrationTest in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/ProductionTaskLifecycleIntegrationTest.kt"
Task: "T018-T022 WarehouseConsumptionIntegrationTest in production-control-api/src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/WarehouseConsumptionIntegrationTest.kt"
```

---

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Implement US1, US2, and US3.
3. Run `make backend-test`.
4. Run `make backend-integration-test`.
5. Record evidence and residual risks in [quickstart.md](./quickstart.md).

### Incremental Delivery

1. Add US1 and validate auth/users wiring.
2. Add US2 and validate production task cross-module wiring.
3. Add US3 and validate warehouse/BOM/consumption wiring.
4. Add P2 scenarios one at a time: US4, US5, US6.
5. Keep each scenario independently runnable and small.

### Scope Guard

- Do not add controller-by-controller integration tests under this feature.
- Do not move business rules into controllers or persistence adapters to satisfy tests.
- Do not duplicate exhaustive unit-test matrices; each scenario should keep one happy flow and 2-4 high-value negative/security/conflict checks.
- Do not require the local Docker Compose app stack for `make backend-integration-test`; Testcontainers owns PostgreSQL setup.
