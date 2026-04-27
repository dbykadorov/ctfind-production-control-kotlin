# Tasks: Orders API + Frontend Wiring

**Input**: Design documents from `/specs/004-orders-api-wiring/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Included because this feature changes persistence, security/RBAC, audit/history, backend contracts, and frontend API wiring. Write the story-specific tests first and confirm they fail before implementation.

**Organization**: Tasks are grouped by user story so each story can be implemented and tested as an independent increment.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish shared files and conventions for the orders module before schema/domain work starts.

- [X] T001 [P] Create frontend order API type definitions in `frontend/cabinet/src/api/types/orders.ts`
- [X] T002 [P] Create backend order test fixtures in `src/test/kotlin/com/ctfind/productioncontrol/orders/OrderTestFixtures.kt`
- [X] T003 [P] Create order permission constants in `src/main/kotlin/com/ctfind/productioncontrol/orders/application/OrderPermissions.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core schema, domain, persistence, and local seed infrastructure required by all user stories.

**Critical**: No user story work should begin until this phase is complete.

- [X] T004 Create Flyway schema for customers, order numbers, orders, items, status changes, change diffs, and order audit links in `src/main/resources/db/migration/V4__create_order_tables.sql`
- [X] T005 [P] Add migration regression tests for order tables and constraints in `src/test/kotlin/com/ctfind/productioncontrol/orders/adapter/persistence/OrderMigrationTests.kt`
- [X] T006 [P] Implement `Customer` and `CustomerStatus` domain models in `src/main/kotlin/com/ctfind/productioncontrol/orders/domain/Customer.kt`
- [X] T007 [P] Implement `OrderStatus` enum and localized label mapping in `src/main/kotlin/com/ctfind/productioncontrol/orders/domain/OrderStatus.kt`
- [X] T008 [P] Implement `CustomerOrderItem` domain model and validation in `src/main/kotlin/com/ctfind/productioncontrol/orders/domain/CustomerOrderItem.kt`
- [X] T009 Implement `CustomerOrder` aggregate with item validation, shipped edit rules, and version metadata in `src/main/kotlin/com/ctfind/productioncontrol/orders/domain/CustomerOrder.kt`
- [X] T010 [P] Implement `OrderStatusChange` and `OrderChangeDiff` domain models in `src/main/kotlin/com/ctfind/productioncontrol/orders/domain/OrderTrace.kt`
- [X] T011 Implement direct-forward transition and order editability policies in `src/main/kotlin/com/ctfind/productioncontrol/orders/domain/OrderPolicies.kt`
- [X] T012 Define command/query models for list, detail, create, update, status change, and dashboard operations in `src/main/kotlin/com/ctfind/productioncontrol/orders/application/OrderModels.kt`
- [X] T013 Define ports for customers, orders, order numbers, trace records, and audit events in `src/main/kotlin/com/ctfind/productioncontrol/orders/application/OrderPorts.kt`
- [X] T014 Create JPA entities for customers, orders, items, status changes, and change diffs in `src/main/kotlin/com/ctfind/productioncontrol/orders/adapter/persistence/OrderJpaEntities.kt`
- [X] T015 Create Spring Data repositories for order persistence in `src/main/kotlin/com/ctfind/productioncontrol/orders/adapter/persistence/OrderJpaRepositories.kt`
- [X] T016 Implement persistence adapters for order ports in `src/main/kotlin/com/ctfind/productioncontrol/orders/adapter/persistence/OrderPersistenceAdapters.kt`
- [X] T017 Implement local seed use case and runner for customers, `ORDER_MANAGER`, and sample orders in `src/main/kotlin/com/ctfind/productioncontrol/orders/adapter/persistence/LocalOrderSeedRunner.kt`
- [X] T018 Create shared order web DTOs and error mapping in `src/main/kotlin/com/ctfind/productioncontrol/orders/adapter/web/OrderDtos.kt`

**Checkpoint**: Database, domain, ports, persistence adapters, seed data, and DTO scaffolding are ready.

---

## Phase 3: User Story 1 - View And Search Orders (Priority: P1)

**Goal**: Authenticated Phase 1 users can view real orders, search by order number/customer, filter orders, and select active customers without placeholder data.

**Independent Test**: Sign in, open the orders area, and confirm seeded orders appear with customer, delivery date, status, update time, and working search/filter behavior.

### Tests for User Story 1

- [X] T019 [P] [US1] Add customer search controller tests for `GET /api/customers` in `src/test/kotlin/com/ctfind/productioncontrol/orders/adapter/web/CustomerControllerTests.kt`
- [X] T020 [P] [US1] Add order list/detail controller tests for `GET /api/orders` and `GET /api/orders/{id}` in `src/test/kotlin/com/ctfind/productioncontrol/orders/adapter/web/OrderControllerQueryTests.kt`
- [X] T021 [P] [US1] Add order query application tests for search, status, active, overdue, and delivery-date filters in `src/test/kotlin/com/ctfind/productioncontrol/orders/application/OrderQueryUseCaseTests.kt`
- [X] T022 [P] [US1] Add frontend customer API mapping tests in `frontend/cabinet/tests/unit/composables/use-customers.test.ts`
- [X] T023 [P] [US1] Add frontend order list API mapping tests in `frontend/cabinet/tests/unit/composables/use-orders.test.ts`

### Implementation for User Story 1

- [X] T024 [US1] Implement customer search query use case in `src/main/kotlin/com/ctfind/productioncontrol/orders/application/CustomerQueryUseCase.kt`
- [X] T025 [US1] Implement order list/detail query use case in `src/main/kotlin/com/ctfind/productioncontrol/orders/application/OrderQueryUseCase.kt`
- [X] T026 [US1] Implement customer search endpoint in `src/main/kotlin/com/ctfind/productioncontrol/orders/adapter/web/CustomerController.kt`
- [X] T027 [US1] Implement order list and detail endpoints in `src/main/kotlin/com/ctfind/productioncontrol/orders/adapter/web/OrderController.kt`
- [X] T028 [US1] Wire customer search to Spring API in `frontend/cabinet/src/api/composables/use-customers.ts`
- [X] T029 [US1] Wire order list/detail loading to Spring API in `frontend/cabinet/src/api/composables/use-orders.ts`
- [X] T030 [US1] Update customer/order response types consumed by UI in `frontend/cabinet/src/api/types/domain.ts`
- [X] T031 [US1] Update order list empty/loading/error behavior for real API data in `frontend/cabinet/src/pages/office/OrdersListPage.vue`

**Checkpoint**: User Story 1 is independently usable with seeded data and no order/customer placeholders.

---

## Phase 4: User Story 2 - Create Customer Order (Priority: P1)

**Goal**: Order managers and administrators can create a new order for an active existing customer with one or more items, while read-only users cannot create orders.

**Independent Test**: Create a new order from the UI using an existing customer and at least one item; verify it receives status `NEW`, a generated order number, and appears in list/detail views.

### Tests for User Story 2

- [X] T032 [P] [US2] Add order creation domain/application tests for validation, generated order number, initial status, and audit event in `src/test/kotlin/com/ctfind/productioncontrol/orders/application/CreateOrderUseCaseTests.kt`
- [X] T033 [P] [US2] Add order creation controller tests for `POST /api/orders`, `401`, `403`, and validation errors in `src/test/kotlin/com/ctfind/productioncontrol/orders/adapter/web/OrderControllerCreateTests.kt`
- [X] T034 [P] [US2] Add frontend order creation mapping and validation error tests in `frontend/cabinet/tests/unit/composables/use-orders.test.ts`
- [X] T035 [P] [US2] Add order new page permission and submit-flow tests in `frontend/cabinet/tests/unit/pages/order-new-page.test.ts`

### Implementation for User Story 2

- [X] T036 [US2] Implement database-backed order number allocation in `src/main/kotlin/com/ctfind/productioncontrol/orders/application/OrderNumberService.kt`
- [X] T037 [US2] Implement create order use case with validation, item ordering, audit, and trace records in `src/main/kotlin/com/ctfind/productioncontrol/orders/application/CreateOrderUseCase.kt`
- [X] T038 [US2] Add `POST /api/orders` handling and write-permission checks in `src/main/kotlin/com/ctfind/productioncontrol/orders/adapter/web/OrderController.kt`
- [X] T039 [US2] Wire order creation API call in `frontend/cabinet/src/api/composables/use-orders.ts`
- [X] T040 [US2] Wire active customer selection and submit flow in `frontend/cabinet/src/pages/office/OrderNewPage.vue`
- [X] T041 [US2] Update customer picker to use active Spring customers in `frontend/cabinet/src/components/domain/CustomerPicker.vue`
- [X] T042 [US2] Update order item form validation display in `frontend/cabinet/src/components/domain/OrderItemsTable.vue`

**Checkpoint**: User Story 2 creates real orders through the backend and preserves data for US1 list/detail.

---

## Phase 5: User Story 3 - Edit Order Details (Priority: P2)

**Goal**: Order managers can edit non-shipped orders, stale updates are rejected, shipped orders are read-only, and key business-field diffs appear in history.

**Independent Test**: Edit a non-shipped order, reload it, confirm changed fields and history diffs; attempt stale update and shipped-order edit to confirm data remains unchanged.

### Tests for User Story 3

- [X] T043 [P] [US3] Add update use case tests for editable fields, item replacement, stale version, shipped read-only, audit, and diffs in `src/test/kotlin/com/ctfind/productioncontrol/orders/application/UpdateOrderUseCaseTests.kt`
- [X] T044 [P] [US3] Add update controller tests for `PUT /api/orders/{id}`, `403`, `409`, and shipped-order rejection in `src/test/kotlin/com/ctfind/productioncontrol/orders/adapter/web/OrderControllerUpdateTests.kt`
- [X] T045 [P] [US3] Add frontend update and stale-version tests in `frontend/cabinet/tests/unit/composables/use-orders.test.ts`
- [X] T046 [P] [US3] Add order detail read-only/editability tests in `frontend/cabinet/tests/unit/composables/use-order-editability.test.ts`

### Implementation for User Story 3

- [X] T047 [US3] Implement order business-field diff generation in `src/main/kotlin/com/ctfind/productioncontrol/orders/application/OrderDiffService.kt`
- [X] T048 [US3] Implement update order use case with optimistic version checks and diff/audit persistence in `src/main/kotlin/com/ctfind/productioncontrol/orders/application/UpdateOrderUseCase.kt`
- [X] T049 [US3] Add `PUT /api/orders/{id}` handling and stale-version mapping in `src/main/kotlin/com/ctfind/productioncontrol/orders/adapter/web/OrderController.kt`
- [X] T050 [US3] Wire update order API call and stale-version error handling in `frontend/cabinet/src/api/composables/use-orders.ts`
- [X] T051 [US3] Wire detail edit flow, history diff rendering, and shipped read-only state in `frontend/cabinet/src/pages/office/OrderDetailPage.vue`
- [X] T052 [US3] Update order timeline to show key business-field diffs in `frontend/cabinet/src/components/domain/OrderTimeline.vue`

**Checkpoint**: User Story 3 supports safe edits and reviewable history without silent overwrites.

---

## Phase 6: User Story 4 - Change Order Status (Priority: P2)

**Goal**: Order managers and administrators can move orders only through direct forward lifecycle transitions, with each transition recorded in status history.

**Independent Test**: Move an order through `NEW -> IN_WORK -> READY -> SHIPPED`, verify each status change is recorded, and verify skipped/reverse transitions are rejected.

### Tests for User Story 4

- [X] T053 [P] [US4] Add status transition policy tests for direct forward, skipped, reverse, and repeated transitions in `src/test/kotlin/com/ctfind/productioncontrol/orders/domain/OrderPoliciesTests.kt`
- [X] T054 [P] [US4] Add status change use case tests for version checks, audit, and history persistence in `src/test/kotlin/com/ctfind/productioncontrol/orders/application/ChangeOrderStatusUseCaseTests.kt`
- [X] T055 [P] [US4] Add status endpoint tests for `POST /api/orders/{id}/status`, `403`, `409`, and `422` in `src/test/kotlin/com/ctfind/productioncontrol/orders/adapter/web/OrderControllerStatusTests.kt`
- [X] T056 [P] [US4] Add frontend workflow transition tests in `frontend/cabinet/tests/unit/composables/use-workflow.test.ts`

### Implementation for User Story 4

- [X] T057 [US4] Implement change status use case with direct-forward validation and status history persistence in `src/main/kotlin/com/ctfind/productioncontrol/orders/application/ChangeOrderStatusUseCase.kt`
- [X] T058 [US4] Add `POST /api/orders/{id}/status` handling and invalid-transition error mapping in `src/main/kotlin/com/ctfind/productioncontrol/orders/adapter/web/OrderController.kt`
- [X] T059 [US4] Wire workflow transition loading and apply calls to Spring API in `frontend/cabinet/src/api/composables/use-workflow.ts`
- [X] T060 [US4] Wire status action buttons and transition errors in `frontend/cabinet/src/pages/office/OrderDetailPage.vue`
- [X] T061 [US4] Update status badge mapping for backend status codes in `frontend/cabinet/src/components/domain/OrderStatusBadge.vue`

**Checkpoint**: User Story 4 moves orders through the allowed lifecycle and records every transition.

---

## Phase 7: User Story 5 - Dashboard Reflects Orders (Priority: P3)

**Goal**: Dashboard widgets use real order metrics, recent activity, and trend data from Spring endpoints.

**Independent Test**: Open the dashboard with seeded and newly created orders; verify KPI counts, status distribution, overdue count, recent activity, and trend widgets reflect backend data.

### Tests for User Story 5

- [X] T062 [P] [US5] Add dashboard summary use case tests for totals, active, overdue, status counts, recent changes, and trends in `src/test/kotlin/com/ctfind/productioncontrol/orders/application/OrderDashboardUseCaseTests.kt`
- [X] T063 [P] [US5] Add dashboard endpoint tests for `GET /api/orders/dashboard` in `src/test/kotlin/com/ctfind/productioncontrol/orders/adapter/web/OrderDashboardControllerTests.kt`
- [X] T064 [P] [US5] Add frontend dashboard stats mapping tests in `frontend/cabinet/tests/unit/composables/use-dashboard-stats.test.ts`
- [X] T065 [P] [US5] Add frontend trend and recent activity mapping tests in `frontend/cabinet/tests/unit/composables/use-trend-data.test.ts`
- [X] T066 [P] [US5] Add frontend recent activity mapping tests in `frontend/cabinet/tests/unit/composables/use-recent-activity.test.ts`

### Implementation for User Story 5

- [X] T067 [US5] Implement dashboard summary query use case in `src/main/kotlin/com/ctfind/productioncontrol/orders/application/OrderDashboardUseCase.kt`
- [X] T068 [US5] Implement dashboard endpoint in `src/main/kotlin/com/ctfind/productioncontrol/orders/adapter/web/OrderDashboardController.kt`
- [X] T069 [US5] Wire dashboard KPI/status distribution API data in `frontend/cabinet/src/api/composables/use-dashboard-stats.ts`
- [X] T070 [US5] Wire dashboard trend API data in `frontend/cabinet/src/api/composables/use-trend-data.ts`
- [X] T071 [US5] Wire recent activity API data in `frontend/cabinet/src/api/composables/use-recent-activity.ts`
- [X] T072 [US5] Update dashboard page loading/empty/error states for real order data in `frontend/cabinet/src/pages/office/DashboardPage.vue`
- [X] T073 [US5] Update recent orders widget to display backend order summaries in `frontend/cabinet/src/components/domain/RecentOrdersWidget.vue`

**Checkpoint**: User Story 5 shows real dashboard signals and no placeholder order/customer/dashboard data remains.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Validate the whole feature, harden contracts, and update verification records.

- [ ] T074 [P] Review controllers for business-rule leakage and keep rules in domain/application code in `src/main/kotlin/com/ctfind/productioncontrol/orders/adapter/web/OrderController.kt`
- [ ] T075 [P] Review persistence adapters for domain terminology and mapping isolation in `src/main/kotlin/com/ctfind/productioncontrol/orders/adapter/persistence/OrderPersistenceAdapters.kt`
- [ ] T076 [P] Update frontend no-legacy-runtime regression expectations if needed in `frontend/cabinet/tests/unit/no-frappe-runtime.test.ts`
- [ ] T077 Run backend tests and record result in `specs/004-orders-api-wiring/quickstart.md`
- [ ] T078 Run frontend tests and build, then record result in `specs/004-orders-api-wiring/quickstart.md`
- [ ] T079 Run Docker startup, API smoke checks, and manual frontend smoke checks, then record result in `specs/004-orders-api-wiring/quickstart.md`
- [ ] T080 Search for forbidden legacy runtime patterns and record result in `specs/004-orders-api-wiring/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: No dependencies.
- **Phase 2 Foundation**: Depends on Phase 1; blocks all user stories.
- **US1 View/Search (Phase 3)**: Depends on Phase 2.
- **US2 Create (Phase 4)**: Depends on Phase 2; benefits from US1 for visible list/detail verification.
- **US3 Edit (Phase 5)**: Depends on US2-created or seeded orders and Phase 2 optimistic version support.
- **US4 Status (Phase 6)**: Depends on Phase 2 and order detail/query infrastructure from US1.
- **US5 Dashboard (Phase 7)**: Depends on Phase 2 and is most useful after US2/US4 generate data.
- **Phase 8 Polish**: Depends on all implemented target stories.

### User Story Dependencies

- **US1 (P1)**: MVP read/search slice; can be implemented immediately after foundation.
- **US2 (P1)**: Can be implemented after foundation; demo value increases when US1 is complete.
- **US3 (P2)**: Requires order update/version infrastructure and order detail UI from US1/US2.
- **US4 (P2)**: Requires order lifecycle policy and order detail UI; can run in parallel with US3 after US1/US2 basics.
- **US5 (P3)**: Requires order data and status history; implement after US1/US2/US4 for meaningful metrics.

### Within Each User Story

- Write story tests first and confirm failure.
- Implement domain/application behavior before web controllers and frontend wiring.
- Backend API contract before frontend integration.
- Frontend composable mapping before page/widget updates.
- Validate each story independently before starting the next priority story.

---

## Parallel Execution Examples

### User Story 1

```bash
Task: "Add customer search controller tests in src/test/kotlin/com/ctfind/productioncontrol/orders/adapter/web/CustomerControllerTests.kt"
Task: "Add order query application tests in src/test/kotlin/com/ctfind/productioncontrol/orders/application/OrderQueryUseCaseTests.kt"
Task: "Add frontend customer API mapping tests in frontend/cabinet/tests/unit/composables/use-customers.test.ts"
Task: "Add frontend order list API mapping tests in frontend/cabinet/tests/unit/composables/use-orders.test.ts"
```

### User Story 2

```bash
Task: "Add order creation use case tests in src/test/kotlin/com/ctfind/productioncontrol/orders/application/CreateOrderUseCaseTests.kt"
Task: "Add order creation controller tests in src/test/kotlin/com/ctfind/productioncontrol/orders/adapter/web/OrderControllerCreateTests.kt"
Task: "Add frontend order creation mapping tests in frontend/cabinet/tests/unit/composables/use-orders.test.ts"
```

### User Story 3

```bash
Task: "Add update use case tests in src/test/kotlin/com/ctfind/productioncontrol/orders/application/UpdateOrderUseCaseTests.kt"
Task: "Add update controller tests in src/test/kotlin/com/ctfind/productioncontrol/orders/adapter/web/OrderControllerUpdateTests.kt"
Task: "Add frontend stale-version tests in frontend/cabinet/tests/unit/composables/use-orders.test.ts"
```

### User Story 4

```bash
Task: "Add status transition policy tests in src/test/kotlin/com/ctfind/productioncontrol/orders/domain/OrderPoliciesTests.kt"
Task: "Add status endpoint tests in src/test/kotlin/com/ctfind/productioncontrol/orders/adapter/web/OrderControllerStatusTests.kt"
Task: "Add frontend workflow transition tests in frontend/cabinet/tests/unit/composables/use-workflow.test.ts"
```

### User Story 5

```bash
Task: "Add dashboard summary use case tests in src/test/kotlin/com/ctfind/productioncontrol/orders/application/OrderDashboardUseCaseTests.kt"
Task: "Add dashboard endpoint tests in src/test/kotlin/com/ctfind/productioncontrol/orders/adapter/web/OrderDashboardControllerTests.kt"
Task: "Add dashboard composable tests in frontend/cabinet/tests/unit/composables/use-dashboard-stats.test.ts"
```

---

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Complete US1 so real seeded orders and customers are visible.
3. Complete US2 so new orders can enter the system.
4. Stop and validate list, detail, create, RBAC, seed data, and no-legacy-runtime behavior.

### Incremental Delivery

1. US1: read/search foundation visible in the frontend.
2. US2: order creation and order number generation.
3. US3: safe edits, stale update protection, and key business-field history.
4. US4: status lifecycle and status history.
5. US5: dashboard metrics and operational signals.

### Verification Targets

- Backend tests: `docker run --rm -v "$PWD":/workspace -w /workspace eclipse-temurin:21-jdk ./gradlew test`
- Frontend tests: `pnpm --dir frontend/cabinet test`
- Frontend build: `pnpm --dir frontend/cabinet build`
- Local runtime: `docker compose up --build --wait`
- Smoke checks: follow `specs/004-orders-api-wiring/quickstart.md`
