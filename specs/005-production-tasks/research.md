# Research: Production Tasks

## Decision: Add A Dedicated `production` Module

**Rationale**: Production tasks are a core ERP domain concept distinct from customer order management. Keeping task lifecycle, assignment, visibility, and history rules in a `production` module preserves clean/hexagonal boundaries and prevents order controllers or persistence adapters from becoming production workflow owners.

**Alternatives considered**:

- Extend the `orders` module with production task behavior: rejected because task execution rules, assignee visibility, and future TOC concepts would overload order management.
- Put task behavior directly in frontend workflow state: rejected because production task status and audit are business facts that must be server-owned and traceable.

## Decision: Reuse Orders As Source Context Through Ports

**Rationale**: Production tasks must link to existing `CustomerOrder` and `CustomerOrderItem` records, but task creation should validate order/item existence through application-facing ports rather than embedding production rules in order persistence. This keeps task creation testable without the web layer and limits cross-module coupling.

**Alternatives considered**:

- Directly query order JPA repositories from production use cases: rejected because it couples modules through persistence details.
- Copy order/customer/item display data only: rejected because source traceability back to customer commitments would weaken.

## Decision: Allow Multiple Tasks Per Order Item With Distinct Purpose

**Rationale**: The source requirements say orders are broken into tasks, which can mean more than one task per order item. Feature 005 avoids full routing/work-step generation but keeps manual task splitting by requiring a distinct purpose for each additional task.

**Alternatives considered**:

- Enforce one task per order item: rejected because it conflicts with "разбиение заказа на задачи" and limits common production scenarios.
- Generate operation/work-step tasks automatically: deferred because routing, operations, and work centers are not yet in Phase 1 scope.

## Decision: Use A Simple Lifecycle With Blocked Interrupt State

**Rationale**: The task lifecycle remains simple enough for MVP execution: `NOT_STARTED -> IN_PROGRESS -> COMPLETED`. `BLOCKED` is modeled as an interrupt state that requires a reason and returns to the previous active status when unblocked. This supports user workflows and future waiting/blocked-time analysis without a custom workflow engine.

**Alternatives considered**:

- Strict linear status only: rejected because blocked work and overdue signals are explicit product needs.
- Custom transitions or full workflow configuration: deferred because it adds complexity before the basic execution model is proven.
- Cancelled/closed statuses in Feature 005: deferred; order cancellation and task closure policies should be designed with broader order/inventory implications.

## Decision: Assign Exactly One Executor

**Rationale**: The source requirements state task assignment to employees and that each employee sees only their own tasks. Assigning exactly one executor gives a clear visibility rule, simple accountability, and focused executor UI.

**Alternatives considered**:

- Assign to work area: deferred because work-area/resource modeling belongs with later capacity and TOC work.
- Assign to multiple executors: deferred because it complicates visibility, audit, status responsibility, and "my tasks" semantics.

## Decision: Production Roles And Permissions

**Rationale**: Use explicit role codes:

- `ADMIN`: full access to production task actions.
- `ORDER_MANAGER`: create tasks from orders and plan/assign tasks.
- `PRODUCTION_SUPERVISOR`: view all tasks, plan/assign tasks, and update any task status.
- `PRODUCTION_EXECUTOR`: view and update status only for assigned tasks.

This maps to the product roles "Администратор", "Менеджер заказов", "Мастер / начальник цеха", and "Сотрудник (исполнитель)".

**Alternatives considered**:

- Reuse only `ORDER_MANAGER`: rejected because production execution responsibilities differ from order planning.
- Let all authenticated users view all tasks: rejected because the source requirements say each employee sees only their own tasks.

## Decision: Store Task History As Business Events

**Rationale**: Task history must serve both product audit and future TOC analysis. Store creation, assignment, planning changes, status transitions, block/unblock, and completion as structured events with actor, timestamp, before/after business values, and optional note/reason.

**Alternatives considered**:

- Store only current task fields: rejected because it loses traceability and blocked-time facts.
- Store only generic audit text: rejected because later analysis needs structured status and timestamp facts.

## Decision: Frontend Uses Dedicated Production Composables

**Rationale**: Existing order composables should remain focused on orders. Dedicated production task composables keep list/detail/workflow behavior explicit while continuing to use the existing Spring API client, JWT handling, and session redirect behavior.

**Alternatives considered**:

- Extend `use-orders.ts` with production task behavior: rejected because task filtering, assignment, and workflow semantics are independent enough to warrant their own API surface.
- Reintroduce Frappe runtime calls or socket assumptions: rejected by project constraints and existing regression guards.
