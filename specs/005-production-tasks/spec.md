# Feature Specification: Production Tasks

**Feature Branch**: `005-production-tasks`  
**Created**: 2026-04-27  
**Status**: Draft  
**Input**: User description: "Feature 005: Production Tasks. Production tasks from customer orders and order items with statuses, assignment, planning, workflow buttons, RBAC, audit and history."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View And Filter Production Tasks (Priority: P1)

Office and shop-floor users need a shared production task list that shows work derived from accepted customer orders and order positions. Users can find tasks by order, customer, item, status, assignee, and due date so daily work is visible without opening each order individually.

**Why this priority**: This creates the first production-facing operational surface. Without task visibility, order data cannot drive shop-floor execution.

**Independent Test**: Sign in as any authenticated Phase 1 work-area role, open the production tasks area, and confirm seeded or created tasks appear with order context, item context, status, assignee, due date, and filterable list behavior.

**Acceptance Scenarios**:

1. **Given** production tasks exist for customer order items, **When** a user opens the task list, **Then** each task shows the related order number, customer, item name, quantity, status, due date, and assignee if assigned.
2. **Given** tasks have different statuses and assignees, **When** a user filters by status, assignee, due date, or searches by order/customer/item, **Then** only matching tasks are shown.
3. **Given** a user has read-only Phase 1 access, **When** they open the production task list and task detail, **Then** they can view task information but cannot plan, assign, or change status.

---

### User Story 2 - Create Production Tasks From Orders (Priority: P1)

Order managers need to generate production tasks from customer orders and their order items so each ordered item can become actionable production work. The resulting tasks must preserve their link to the source order and item for traceability.

**Why this priority**: Creating tasks from orders is the core handoff from order management to production execution.

**Independent Test**: As an order manager or administrator, open an eligible order, create production tasks from one or more order items, and verify the new tasks appear in the production task list with source order and item context.

**Acceptance Scenarios**:

1. **Given** an order has one or more items without production tasks, **When** an order manager creates production tasks from the order, **Then** one task is created per selected order item with initial status, quantity, due date, and source links.
2. **Given** production tasks already exist for an order item, **When** an order manager attempts to create duplicate tasks for that same item, **Then** the system prevents accidental duplication or clearly requires an intentional additional task.
3. **Given** a read-only user opens an eligible order, **When** they attempt to create production tasks, **Then** the action is unavailable or rejected and no tasks are created.

---

### User Story 3 - Plan And Assign Production Work (Priority: P2)

Order managers and shop supervisors need to assign production tasks to responsible executors or work areas and set planned dates so production work can be coordinated before execution begins.

**Why this priority**: Assignment and planning turn generated tasks into executable shop-floor work and establish accountability.

**Independent Test**: As an order manager or shop supervisor, open a task, assign it to a responsible executor or work area, set planned dates, save, and verify the task list and task detail reflect the assignment and planning history.

**Acceptance Scenarios**:

1. **Given** a task is unassigned, **When** an authorized planner assigns an executor or work area, **Then** the assignee is visible on the list and detail screens.
2. **Given** a task has planned dates, **When** an authorized planner changes those dates, **Then** the latest values are visible and the change is recorded in history.
3. **Given** an executor is not allowed to plan tasks, **When** they view a task, **Then** assignment and planning fields are read-only.

---

### User Story 4 - Move Tasks Through Production Statuses (Priority: P2)

Shop supervisors and executors need workflow actions for production task statuses so actual progress is captured consistently and users can see whether work is waiting, in progress, completed, or blocked.

**Why this priority**: Status transitions are the first reliable production execution signal and are required for future flow, bottleneck, and buffer analysis.

**Independent Test**: Move a task through the allowed lifecycle, verify each status change appears in history, verify forbidden skipped or reverse transitions are rejected, and verify completed tasks no longer allow regular execution changes.

**Acceptance Scenarios**:

1. **Given** a task is ready to start, **When** an authorized user applies the next workflow action, **Then** the task moves to the next allowed status and records who changed it and when.
2. **Given** a task is blocked, **When** a user records a block reason, **Then** the task visibly shows blocked status and the reason is retained in history.
3. **Given** a user attempts a skipped, repeated, or reverse transition, **When** the action is submitted, **Then** the transition is rejected and the current status remains unchanged.

---

### User Story 5 - Review Production Task History (Priority: P3)

Managers and supervisors need a task history timeline showing creation, assignment, planning changes, status changes, and block reasons so production decisions can be audited and operational delays can be investigated.

**Why this priority**: History completes the traceability requirement and prepares the feature for later Theory of Constraints analysis.

**Independent Test**: Create, assign, update, and move a task through at least two statuses, then open its detail screen and verify the timeline shows each business event in chronological order with actor and relevant details.

**Acceptance Scenarios**:

1. **Given** a task has multiple changes, **When** a user opens the task detail, **Then** the history shows creation, assignment, planning changes, and status changes with actor and timestamp.
2. **Given** a task is blocked and later unblocked, **When** a user reviews history, **Then** both events and the block reason are visible.

---

### Edge Cases

- If an order item is edited after production tasks have been created, existing task links remain traceable and users can see that task data may need review.
- If an order is shipped or cancelled after tasks exist, task actions are restricted according to current task state and user role rather than silently deleting tasks.
- If a task assignee becomes inactive, the task remains visible and can be reassigned by an authorized user.
- If two users try to update the same task at the same time, the later stale update is rejected and the user is asked to reload before saving.
- If no production tasks exist yet, task list and dashboard areas show an empty state with a clear path for authorized users to create tasks from orders.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST represent production tasks as operational work items linked to a source customer order and, when applicable, a specific order item.
- **FR-002**: Users MUST be able to view a production task list with order number, customer, item, quantity, task status, due date, and assignee.
- **FR-003**: Users MUST be able to search and filter production tasks by order, customer, item, status, assignee, due date, and blocked/active state.
- **FR-004**: Authorized order managers and administrators MUST be able to create production tasks from eligible customer order items.
- **FR-005**: The system MUST prevent accidental duplicate production tasks for the same order item unless the user intentionally creates additional work with a distinct purpose.
- **FR-006**: The system MUST assign each newly created task an initial production status and retain its source order and item references.
- **FR-007**: Authorized order managers and shop supervisors MUST be able to assign or reassign a production task to an executor or work area.
- **FR-008**: Authorized order managers and shop supervisors MUST be able to set and update planned production dates for a task.
- **FR-009**: Shop supervisors and executors MUST be able to move tasks through allowed production statuses according to their role permissions.
- **FR-010**: The system MUST reject skipped, repeated, reverse, or otherwise invalid task status transitions.
- **FR-011**: The system MUST support a blocked task state with a required user-visible reason when a task is marked blocked.
- **FR-012**: Completed tasks MUST be protected from regular edits that would change execution facts, while remaining readable for review.
- **FR-013**: Read-only Phase 1 users MUST be able to view production tasks but MUST NOT be able to create, assign, plan, or change task status.
- **FR-014**: The system MUST record audit/history events for task creation, assignment changes, planning changes, status changes, block/unblock events, and completion.
- **FR-015**: Task history MUST show actor, timestamp, event type, and business-relevant details in a readable timeline.
- **FR-016**: Production task detail MUST display source order and order item context so users can navigate from production work back to the customer commitment.
- **FR-017**: The system MUST protect task updates from silent overwrites when another user has changed the same task.
- **FR-018**: Task list, detail, assignment, and workflow controls MUST reflect the current user's permissions.
- **FR-019**: Production task data MUST support future reporting of waiting time, blocked time, throughput, and work-in-progress by status or assignee.
- **FR-020**: Existing order and dashboard workflows MUST continue to function when production tasks are introduced.

### Key Entities *(include if feature involves data)*

- **ProductionTask**: A production work item created from a customer order or order item. Key business attributes include task number, source order, source order item, item description, quantity, status, assignee, planned dates, blocked reason, created actor, and timestamps.
- **ProductionTaskStatus**: The lifecycle state of a production task. It distinguishes at least not-started, in-progress, blocked, completed, and cancelled/closed states.
- **ProductionTaskAssignment**: The current responsibility for a task, expressed as an executor, supervisor, or work area depending on planning needs.
- **ProductionTaskHistoryEvent**: A traceable business event for creation, assignment, planning, status movement, blocking, unblocking, or completion.
- **CustomerOrder / CustomerOrderItem Reference**: Existing order context that remains the commercial source of each production task.

### Constitution Alignment *(mandatory)*

- **ERP/domain fit**: Strengthens the order-to-production workflow by turning customer order demand into executable production work.
- **TOC readiness**: Preserves status history, blocked reasons, assignee/work-area context, planned dates, and timestamps needed for future waiting-time, work-in-progress, throughput, bottleneck, and buffer analysis.
- **Traceability/audit**: Creation, assignment, planning, status changes, blocked/unblocked events, and completion must be traceable to actor and time.
- **Security/API boundary**: Introduces role-specific production permissions: order managers plan/create, shop supervisors plan and coordinate, executors update permitted execution statuses, read-only roles observe only.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An authorized order manager can create production tasks for a multi-item order in under 2 minutes.
- **SC-002**: 95% of task list searches or filters over the Phase 1 expected dataset show results or an empty state in under 2 seconds.
- **SC-003**: Users can identify a task's source order, customer, item, status, assignee, and due date from the task list without opening detail.
- **SC-004**: 100% of task creation, assignment, planning, status, block, and completion events appear in task history with actor and timestamp.
- **SC-005**: Invalid or unauthorized task changes leave task state unchanged and show a user-understandable failure message.
- **SC-006**: A shop supervisor can move a task from not-started to completed through the allowed workflow without leaving the task detail screen.
- **SC-007**: Production task data is sufficient to calculate basic work-in-progress counts by status and assignee after tasks are used for one workday.

## Assumptions

- Production tasks are created from existing active customer orders and order items; creating standalone tasks unrelated to orders is out of scope for this feature.
- Customer and order CRUD remain governed by existing order features; this feature only consumes their context.
- The first version supports one current assignee or work area per task, with assignment changes retained in history.
- Initial production statuses will be simple enough for Phase 1 execution while preserving history for later TOC-specific buffer and bottleneck concepts.
- Inventory reservation, material availability, capacity scheduling, and detailed routing are out of scope for this feature unless explicitly added in a later feature.
- Existing authentication and Phase 1 roles are reused; this feature defines production-specific actions within those roles rather than introducing a full user administration feature.
