# Feature Specification: Phase 1 Spring Integration Scenarios

**Feature Branch**: `017-spring-integration-tests`  
**Created**: 2026-05-03  
**Status**: Draft  
**Input**: User description: "Добавить минимальный набор Spring integration сценариев для Phase 1: auth/users, orders, production tasks, warehouse/BOM/consumption, notifications, audit feed. Проверить реальные сквозные потоки через HTTP, security, controllers, use cases, persistence, migrations, PostgreSQL-compatible database; не дублировать unit tests; приоритет MVP: AuthUsersSecurityIntegrationTest, ProductionTaskLifecycleIntegrationTest, WarehouseConsumptionIntegrationTest."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Auth and Users Security Integration (Priority: P1)

As a release reviewer, I need one real application-level scenario that proves administrator bootstrap, authentication, current-user lookup, and users API authorization work together before any operational flows are trusted.

**Why this priority**: Authentication and user administration gate every cabinet workflow. If bootstrap, login, role extraction, or ADMIN-only access is wired incorrectly, the rest of Phase 1 cannot be trusted.

**Independent Test**: Start from a clean application test database, bootstrap a production-like administrator, authenticate through the real login path, then verify current-user and users API access for admin, non-admin, and unauthenticated sessions.

**Acceptance Scenarios**:

1. **Given** a clean system with production-like administrator bootstrap credentials, **When** the application starts for the scenario, **Then** exactly one administrator account is available for login.
2. **Given** the administrator signs in through the application login path, **When** the current-user endpoint is requested, **Then** the response identifies the administrator and includes the `ADMIN` role.
3. **Given** an `ADMIN` session, **When** the users API is requested, **Then** user data is returned successfully.
4. **Given** a non-admin authenticated session, **When** the same users API is requested, **Then** access is rejected with unauthorized-role behavior.
5. **Given** no authenticated session, **When** protected users or current-user resources are requested, **Then** unauthenticated behavior is returned.

---

### User Story 2 - Production Task Lifecycle Integration (Priority: P1)

As a production reviewer, I need a real cross-module scenario that proves an order can become production tasks, those tasks can be assigned and executed, and role visibility/history stay correct across the full task lifecycle.

**Why this priority**: Production tasks are the highest-risk Phase 1 workflow because they cross orders, production, security, history, notifications, and persistence.

**Independent Test**: Create an order, create production tasks from order items, assign an executor, exercise the lifecycle from not started through blocked and completed, then verify role visibility, stale-version handling, unauthenticated access, and history events.

**Acceptance Scenarios**:

1. **Given** an order with at least one item, **When** production tasks are created from that order, **Then** the resulting task is linked to the order and visible to roles with production visibility.
2. **Given** a task assigned to an executor, **When** the executor lists tasks, **Then** only tasks assigned to that executor are visible.
3. **Given** supervisor or administrator access, **When** the same tasks are listed, **Then** the reviewer sees tasks according to the broader production visibility rules.
4. **Given** a production task, **When** its lifecycle moves through `NOT_STARTED -> IN_PROGRESS -> BLOCKED -> IN_PROGRESS -> COMPLETED`, **Then** each accepted transition is persisted and visible in detail.
5. **Given** task detail and history are requested after assignment and status changes, **Then** creation, assignment/planning, and status events are present.
6. **Given** an executor tries to access or update another executor's task, **When** the request is submitted, **Then** access is rejected.
7. **Given** a stale expected version is submitted, **When** a mutation is attempted, **Then** a conflict response is returned.

---

### User Story 3 - Warehouse, BOM, and Consumption Integration (Priority: P1)

As an operations reviewer, I need a real scenario proving that materials, receipts, order BOM, consumption, and stock usage stay consistent across inventory and orders.

**Why this priority**: Warehouse consumption combines inventory, orders, role permissions, stock movement rules, and shipped-order restrictions; unit tests alone do not prove the modules are wired together.

**Independent Test**: Create a material, receive stock, create an order, add a BOM line, consume stock against the order, verify usage totals, and exercise insufficient-stock and shipped-order restrictions.

**Acceptance Scenarios**:

1. **Given** a material exists, **When** stock is received, **Then** current stock increases and the movement is persisted.
2. **Given** an order and a material, **When** a BOM line is added, **Then** the order usage view includes required quantity.
3. **Given** stock exists and a BOM line requires material, **When** partial stock is consumed for the order, **Then** usage shows required, consumed, and remaining quantities correctly.
4. **Given** a request consumes more material than available, **When** the request is submitted, **Then** the operation is rejected and stock remains consistent.
5. **Given** an order has been shipped, **When** a user attempts to edit BOM or consume stock for that order, **Then** the operation is rejected.
6. **Given** warehouse or administrator access, **When** warehouse operations are requested, **Then** access is allowed; users without allowed roles are rejected.

---

### User Story 4 - Order Lifecycle Integration (Priority: P2)

As a release reviewer, I need an order lifecycle scenario that proves order creation, detail/list retrieval, editing, status changes, and shipped-order restrictions are connected through the real application path.

**Why this priority**: Orders are central to Phase 1, but their business rules are already covered by unit tests. This scenario fills the application-wiring gap after the higher-risk MVP flows.

**Independent Test**: Authenticate as a writer, create an order with items, retrieve list/detail, update editable fields, move through the lifecycle to shipped, and verify mutation restrictions and role denial cases.

**Acceptance Scenarios**:

1. **Given** an authorized order writer, **When** an order with items is created, **Then** it appears in list and detail views with the expected customer and items.
2. **Given** an editable order, **When** it is updated, **Then** the new values and version are visible in detail.
3. **Given** an order in `NEW`, **When** it moves through `IN_WORK`, `READY`, and `SHIPPED`, **Then** the direct forward lifecycle is persisted.
4. **Given** a shipped order, **When** an edit is attempted, **Then** the edit is rejected.
5. **Given** a role without order-write permission, **When** it attempts to create, edit, or change order status, **Then** access is rejected.
6. **Given** an invalid status transition, **When** it is requested, **Then** the transition is rejected.

---

### User Story 5 - Notifications Integration (Priority: P2)

As a production reviewer, I need one scenario proving that production events create notifications for the right recipients and that notification read state is isolated by user.

**Why this priority**: Notification infrastructure has unit coverage, but the important Phase 1 risk is whether production events actually create and expose the expected notifications.

**Independent Test**: Assign a production task, change task status, run overdue processing for an overdue task, then verify notification list, unread count, mark-read behavior, mark-all behavior, and user isolation.

**Acceptance Scenarios**:

1. **Given** a production task is assigned to an executor, **When** the assignment completes, **Then** the executor receives a `TASK_ASSIGNED` notification.
2. **Given** a task status changes, **When** the status change completes, **Then** the intended recipient receives a `STATUS_CHANGED` notification.
3. **Given** an overdue task exists, **When** overdue processing runs, **Then** the intended recipients receive `TASK_OVERDUE` notifications without duplicate notifications for the same overdue state.
4. **Given** a user has unread notifications, **When** one notification is marked read, **Then** unread count decreases and the read timestamp is preserved on repeat read.
5. **Given** a user marks all notifications read, **When** unread count is requested, **Then** it reflects the updated state.
6. **Given** User A attempts to view or mutate User B's notification, **When** the request is submitted, **Then** access is rejected or the notification is hidden.

---

### User Story 6 - Audit Feed Integration (Priority: P2)

As an administrator, I need a real audit feed scenario proving that business actions from multiple Phase 1 modules are visible in one audit view and remain admin-only.

**Why this priority**: The audit feed merges several event sources; integration coverage is needed to catch schema, query, category, and security wiring issues.

**Independent Test**: Perform authentication, order, production, and inventory actions; request the audit feed as admin; verify expected categories, filters, and forbidden behavior for non-admin and unauthenticated sessions.

**Acceptance Scenarios**:

1. **Given** authentication, order, production, and inventory actions have occurred, **When** the administrator requests the audit feed, **Then** the feed contains events for the relevant Phase 1 categories that are implemented.
2. **Given** an admin filters audit events by category, date range, actor, or search text, **When** matching events exist, **Then** only matching real events are returned.
3. **Given** a non-admin session, **When** it requests the audit feed, **Then** access is rejected.
4. **Given** no authenticated session, **When** the audit feed is requested, **Then** unauthenticated behavior is returned.
5. **Given** an audit category is not implemented, **When** the scenario is reviewed, **Then** the test documents the gap instead of assuming the category exists.

---

### Edge Cases

- What happens when the integration database starts dirty? Scenarios must isolate their data or start from a controlled state so results are deterministic.
- What happens when a flow needs users with several roles? Scenario setup must create only the minimum users needed and document each role's purpose.
- What happens if a module already has detailed unit coverage? The integration scenario must focus on cross-layer wiring and only a few key negative cases.
- What happens if a Phase 1 category is not yet available in the audit feed? The scenario must record this as an explicit residual risk or deferred assertion.
- What happens if the full suite becomes too slow? The suite must remain small and scenario-based; adding broad permutations is out of scope.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST include an application-level integration scenario for Auth + Users security covering bootstrap, login, current-user lookup, admin users access, non-admin rejection, and unauthenticated rejection.
- **FR-002**: The system MUST include an application-level integration scenario for Production Task lifecycle covering order-to-task creation, assignment, executor visibility, supervisor/admin visibility, lifecycle transitions, history, stale-version conflict, and unauthenticated rejection.
- **FR-003**: The system MUST include an application-level integration scenario for Warehouse/BOM/Consumption covering material creation, receipt, order BOM, stock consumption, usage totals, insufficient stock, shipped-order restrictions, and role access.
- **FR-004**: The system SHOULD include an application-level integration scenario for Order lifecycle covering create, list/detail, update, status transitions, shipped-order rejection, non-writer rejection, and invalid transition rejection.
- **FR-005**: The system SHOULD include an application-level integration scenario for Notifications covering assignment, status-change, overdue notifications, unread count, mark-read, mark-all-read, and user isolation.
- **FR-006**: The system SHOULD include an application-level integration scenario for Audit feed covering multi-module audit events, admin-only access, unauthenticated rejection, and category/date/actor/search filtering on real event data.
- **FR-007**: The MVP MUST contain exactly the three highest-priority scenarios: Auth + Users Security, Production Task Lifecycle, and Warehouse/BOM/Consumption.
- **FR-008**: Each integration scenario MUST verify the real application path across authentication, request handling, business use cases, persistence, migrations, and response mapping.
- **FR-009**: Each scenario MUST include only 2-4 high-value negative cases and MUST NOT duplicate the full matrix already covered by unit tests.
- **FR-010**: The integration suite MUST be runnable through the standard backend verification workflow or through one clearly documented backend integration target.
- **FR-011**: Integration scenarios MUST obtain authenticated sessions through the normal login flow unless a specific setup shortcut is explicitly justified in the scenario documentation.
- **FR-012**: Each scenario MUST document the Phase 1 flow it covers and any residual risk it intentionally leaves to unit tests or manual smoke checks.
- **FR-013**: The suite MUST preserve existing unit tests as the main business-rule coverage and use integration tests only for wiring, security, persistence, migration, and cross-module behavior.

### Key Entities

- **Integration Scenario**: A named business-flow test that exercises a complete Phase 1 workflow through the real application path.
- **Scenario Actor**: A test user with a specific Phase 1 role, such as `ADMIN`, `ORDER_MANAGER`, `WAREHOUSE`, `PRODUCTION_SUPERVISOR`, or `PRODUCTION_EXECUTOR`.
- **Scenario Fixture**: Minimal data needed to execute a scenario, such as users, customers, orders, order items, materials, stock movements, production tasks, notifications, or audit events.
- **Verification Evidence**: The scenario result, command target, and documented coverage/residual-risk note used to show what the integration suite protects.
- **Residual Risk**: A behavior intentionally not covered by an integration scenario because it remains covered by existing unit tests or manual smoke checks.

### Constitution Alignment *(mandatory)*

- **ERP/domain fit**: Strengthens Phase 1 confidence across users, roles, orders, production tasks, inventory, notifications, and audit events by validating complete operational flows.
- **TOC readiness**: Preserves task flow history, status timing, overdue pressure, inventory consumption facts, and audit trails by verifying these facts survive real application wiring.
- **Traceability/audit**: Ensures significant business-state changes remain visible in history, notifications, and audit feed scenarios where applicable.
- **Security/API boundary**: Directly validates explicit authenticated, unauthenticated, admin, non-admin, writer, executor, supervisor, and warehouse access expectations through the same access paths used by clients.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The MVP integration suite contains 3 scenario classes or equivalent scenario groups covering Auth + Users, Production Task Lifecycle, and Warehouse/BOM/Consumption.
- **SC-002**: The full planned suite contains no more than 6 scenario classes or equivalent scenario groups for Phase 1 integration coverage.
- **SC-003**: Every MVP scenario includes at least one successful business flow and at least two negative/security/conflict checks.
- **SC-004**: The integration suite can be run from a documented backend verification command without requiring manual application setup.
- **SC-005**: A reviewer can read the scenario names and coverage notes in under 10 minutes and understand which Phase 1 flows are protected.
- **SC-006**: Existing unit tests remain unchanged in purpose: business-rule matrices stay in unit tests, while integration scenarios focus on cross-layer behavior.
- **SC-007**: Any skipped, deferred, or not-yet-implemented Phase 1 category is recorded with an explicit residual-risk note.

## Assumptions

- Existing unit tests remain the primary coverage for detailed business-rule permutations.
- The integration scenarios are backend-focused and do not replace frontend unit tests or manual browser smoke checks.
- A PostgreSQL-compatible isolated test database is available for planning and implementation.
- Test data may be created through scenario setup or through public application flows, but authenticated behavior should use the normal login path.
- Scenario classes may share test helpers for login, request creation, user setup, and common assertions, as long as each scenario remains independently understandable.
