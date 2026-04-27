# Feature Specification: Production Tasks

**Feature Branch**: `005-production-tasks`  
**Created**: 2026-04-27  
**Status**: Ready for implementation  
**Input**: User description: "Feature 005: Production Tasks. Production tasks from customer orders and order items with statuses, assignment, planning, workflow buttons, RBAC, audit and history."

## Clarifications

### Session 2026-04-27

- Q: Should Feature 005 enforce one task per order item, allow manual multiple tasks per order item, or generate operation/work-step tasks? → A: Allow manual multiple tasks per order item with a distinct purpose.
- Q: What production task status lifecycle should Feature 005 support? → A: Linear lifecycle plus blocked/unblocked path.
- Q: Who can see production tasks in Feature 005? → A: Managers and supervisors see all tasks; executors see only tasks assigned to them.
- Q: What assignment target should Feature 005 support? → A: Assign to one executor; work area is out of scope for Feature 005.
- Q: Who can update production task statuses? → A: Supervisors update any task; executors update only tasks assigned to them.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View And Filter Production Tasks (Priority: P1)

Office users and shop supervisors need a shared production task list that shows work derived from accepted customer orders and order positions. Executors need a focused task list limited to their assigned work. Users can find visible tasks by order, customer, item, status, assignee, and due date so daily work is visible without opening each order individually.

**Why this priority**: This creates the first production-facing operational surface. Without task visibility, order data cannot drive shop-floor execution.

**Independent Test**: Sign in as a manager or supervisor and confirm all production tasks are visible; sign in as an executor and confirm only tasks assigned to that executor are visible, with order context, item context, status, assignee, due date, and filterable list behavior.

**Acceptance Scenarios**:

1. **Given** production tasks exist for customer order items, **When** a user opens the task list, **Then** each task shows the related order number, customer, item name, quantity, status, due date, and assignee if assigned.
2. **Given** tasks have different statuses and assignees, **When** a user filters by status, assignee, due date, or searches by order/customer/item, **Then** only matching tasks are shown.
3. **Given** an executor has assigned tasks, **When** they open the production task list, **Then** they see only their assigned tasks and cannot access unassigned or other executors' tasks.
4. **Given** a user has read-only Phase 1 access, **When** they open the production task list and task detail, **Then** they can view only tasks permitted by their visibility scope and cannot plan, assign, or change status.

---

### User Story 2 - Create Production Tasks From Orders (Priority: P1)

Order managers need to generate production tasks from customer orders and their order items so each ordered item can become actionable production work. The resulting tasks must preserve their link to the source order and item for traceability, while allowing multiple purposeful tasks for the same item when production needs to split work manually.

**Why this priority**: Creating tasks from orders is the core handoff from order management to production execution.

**Independent Test**: As an order manager or administrator, open an eligible order, create production tasks from one or more order items, and verify the new tasks appear in the production task list with source order and item context.

**Acceptance Scenarios**:

1. **Given** an order has one or more items requiring production work, **When** an order manager creates production tasks from the order, **Then** tasks are created for selected order items with initial status, quantity, due date, purpose, and source links.
2. **Given** production tasks already exist for an order item, **When** an order manager creates another task for that same item, **Then** the system requires a distinct purpose so the additional task is intentional and traceable.
3. **Given** a read-only user opens an eligible order, **When** they attempt to create production tasks, **Then** the action is unavailable or rejected and no tasks are created.

---

### User Story 3 - Plan And Assign Production Work (Priority: P2)

Order managers and shop supervisors need to assign production tasks to one responsible executor and set planned dates so production work can be coordinated before execution begins.

**Why this priority**: Assignment and planning turn generated tasks into executable shop-floor work and establish accountability.

**Independent Test**: As an order manager or shop supervisor, open a task, assign it to one responsible executor, set planned dates, save, and verify the task list and task detail reflect the assignment and planning history.

**Acceptance Scenarios**:

1. **Given** a task is unassigned, **When** an authorized planner assigns one executor, **Then** the assignee is visible on the list and detail screens.
2. **Given** a task has planned dates, **When** an authorized planner changes those dates, **Then** the latest values are visible and the change is recorded in history.
3. **Given** an executor is not allowed to plan tasks, **When** they view a task, **Then** assignment and planning fields are read-only.

---

### User Story 4 - Move Tasks Through Production Statuses (Priority: P2)

Shop supervisors and executors need workflow actions for production task statuses so actual progress is captured consistently and users can see whether work is not started, in progress, blocked, or completed. Supervisors can coordinate status changes across all tasks, while executors can update only tasks assigned to them.

**Why this priority**: Status transitions are the first reliable production execution signal and are required for future flow, bottleneck, and buffer analysis.

**Independent Test**: Move a task through the allowed lifecycle, block and unblock it, verify each status change appears in history, verify forbidden skipped or reverse transitions are rejected, and verify completed tasks no longer allow regular execution changes.

**Acceptance Scenarios**:

1. **Given** a task is not started, **When** a supervisor or assigned executor starts work, **Then** the task moves to in progress and records who changed it and when.
2. **Given** a task is not started or in progress, **When** a supervisor or assigned executor blocks it with a reason, **Then** the task visibly shows blocked status and the reason is retained in history.
3. **Given** a task is blocked, **When** an authorized user unblocks it, **Then** the task returns to its previous active status and records who changed it and when.
4. **Given** an executor tries to change a task assigned to someone else, **When** the action is submitted, **Then** the action is rejected and the current status remains unchanged.
5. **Given** a user attempts a skipped, repeated, or reverse transition, **When** the action is submitted, **Then** the transition is rejected and the current status remains unchanged.

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
- If a task would require team or work-area assignment, Feature 005 records one responsible executor and leaves team/work-area planning for a later feature.
- If two users try to update the same task at the same time, the later stale update is rejected and the user is asked to reload before saving.
- If no production tasks exist yet, task list and dashboard areas show an empty state with a clear path for authorized users to create tasks from orders.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST represent production tasks as operational work items linked to a source customer order and, when applicable, a specific order item.
- **FR-002**: Users MUST be able to view a production task list with order number, customer, item, quantity, task status, due date, and assignee.
- **FR-003**: Users MUST be able to search and filter production tasks by order, customer, item, status, assignee, due date, and blocked/active state.
- **FR-004**: Authorized order managers and administrators MUST be able to create one or more production tasks from eligible customer order items.
- **FR-005**: The system MUST allow multiple production tasks for the same order item only when each additional task has a distinct user-visible purpose.
- **FR-006**: The system MUST assign each newly created task an initial production status and retain its source order and item references.
- **FR-007**: Authorized order managers and shop supervisors MUST be able to assign or reassign a production task to exactly one executor.
- **FR-008**: Authorized order managers and shop supervisors MUST be able to set and update planned production dates for a task.
- **FR-009**: Shop supervisors MUST be able to move any production task through the allowed production lifecycle: not-started, in-progress, completed, plus blocked/unblocked from active work.
- **FR-010**: Executors MUST be able to move only tasks assigned to them through the allowed production lifecycle.
- **FR-011**: The system MUST reject skipped, repeated, reverse, unauthorized, or otherwise invalid task status transitions, except unblocking returns a blocked task to its previous active status.
- **FR-012**: The system MUST support a blocked task state with a required user-visible reason when a task is marked blocked.
- **FR-013**: Completed tasks MUST be protected from regular edits that would change execution facts, while remaining readable for review.
- **FR-014**: Managers and shop supervisors MUST be able to view all production tasks; executors MUST be able to view only tasks assigned to them.
- **FR-015**: Read-only Phase 1 users MUST be able to view only production tasks permitted by their visibility scope and MUST NOT be able to create, assign, plan, or change task status.
- **FR-016**: The system MUST record audit/history events for task creation, assignment changes, planning changes, status changes, block/unblock events, and completion.
- **FR-017**: Task history MUST show actor, timestamp, event type, and business-relevant details in a readable timeline.
- **FR-018**: Production task detail MUST display source order and order item context so users can navigate from production work back to the customer commitment.
- **FR-019**: The system MUST protect task updates from silent overwrites when another user has changed the same task.
- **FR-020**: Task list, detail, assignment, and workflow controls MUST reflect the current user's permissions and task visibility scope.
- **FR-021**: Production task data MUST support future reporting of waiting time, blocked time, throughput, and work-in-progress by status or assignee.
- **FR-022**: Existing order and dashboard workflows MUST continue to function when production tasks are introduced.

### Key Entities *(include if feature involves data)*

- **ProductionTask**: A production work item created from a customer order or order item. Key business attributes include task number, source order, source order item, purpose, item description, quantity, status, executor assignee, planned dates, blocked reason, created actor, and timestamps.
- **ProductionTaskStatus**: The lifecycle state of a production task. It distinguishes not-started, in-progress, blocked, and completed states.
- **ProductionTaskAssignment**: The current responsibility for a task, expressed as exactly one executor.
- **ProductionTaskHistoryEvent**: A traceable business event for creation, assignment, planning, status movement, blocking, unblocking, or completion.
- **CustomerOrder / CustomerOrderItem Reference**: Existing order context that remains the commercial source of each production task.

### Constitution Alignment *(mandatory)*

- **ERP/domain fit**: Strengthens the order-to-production workflow by turning customer order demand into executable production work.
- **TOC readiness**: Preserves status history, blocked reasons, executor context, planned dates, and timestamps needed for future waiting-time, work-in-progress, throughput, bottleneck, and buffer analysis.
- **Traceability/audit**: Creation, assignment, planning, status changes, blocked/unblocked events, and completion must be traceable to actor and time.
- **Security/API boundary**: Introduces role-specific production permissions: order managers plan/create, shop supervisors plan, coordinate, and update any task status, executors view and update only assigned tasks, read-only roles observe only within their visibility scope.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An authorized order manager can create production tasks for a multi-item order in under 2 minutes.
- **SC-002**: 95% of task list searches or filters over the Phase 1 expected dataset show results or an empty state in under 2 seconds.
- **SC-003**: Managers and supervisors can identify any task's source order, customer, item, status, assignee, and due date from the task list without opening detail; executors can do the same for their assigned tasks.
- **SC-004**: 100% of task creation, assignment, planning, status, block, and completion events appear in task history with actor and timestamp.
- **SC-005**: Invalid or unauthorized task changes, including executor attempts to update another executor's task, leave task state unchanged and show a user-understandable failure message.
- **SC-006**: A shop supervisor can move a task from not-started to completed through the allowed workflow without leaving the task detail screen.
- **SC-007**: Production task data is sufficient to calculate basic work-in-progress counts by status and assignee after tasks are used for one workday.

## Assumptions

- Production tasks are created from existing active customer orders and order items; creating standalone tasks unrelated to orders is out of scope for this feature.
- Multiple tasks may be linked to the same order item when the user provides a distinct purpose; automatic operation routing or work-step generation is out of scope for this feature.
- Customer and order CRUD remain governed by existing order features; this feature only consumes their context.
- The first version supports one current executor assignee per task, with assignment changes retained in history.
- Work-area, team, and multi-executor assignment are out of scope for Feature 005.
- Initial production statuses are limited to not-started, in-progress, blocked, and completed, while preserving history for later TOC-specific buffer and bottleneck concepts.
- Inventory reservation, material availability, capacity scheduling, and detailed routing are out of scope for this feature unless explicitly added in a later feature.
- Existing authentication and Phase 1 roles are reused; this feature defines production-specific actions and visibility rules within those roles rather than introducing a full user administration feature.
