# Contract: Frontend Production Tasks Wiring

This contract describes how the cabinet SPA adds production task list/detail/workflow behavior through `src/api/api-client.ts`.

## Shared Rules

- All requests use the existing `httpClient`, so the Bearer JWT is attached automatically.
- `401` responses use the existing session-expired redirect behavior.
- `403` responses show a permission or visibility error and leave current UI state unchanged.
- `409` stale version responses ask the user to reload before retrying.
- No frontend code may call legacy Frappe endpoints, Frappe realtime, `frappeCall`, or `socket.io`.

## Types

```ts
type ProductionTaskStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'BLOCKED' | 'COMPLETED'
type ProductionTaskAction = 'ASSIGN' | 'PLAN' | 'START' | 'BLOCK' | 'UNBLOCK' | 'COMPLETE'

interface ProductionTaskListRow {
  id: string
  taskNumber: string
  purpose: string
  orderNumber: string
  customerDisplayName: string
  itemName: string
  quantity: number
  uom: string
  status: ProductionTaskStatus
  statusLabel: string
  executorDisplayName?: string
  plannedStartDate?: string
  plannedFinishDate?: string
  blockedReason?: string
  updatedAt: string
  version: number
}
```

## use-production-tasks.ts

### List Tasks

Behavior:

- Calls `GET /api/production-tasks` with search, status, order, item, executor, assigned-to-me, blocked, active, due-date range, page, and size filters.
- Maps API status codes to existing UI labels and filter options.
- Empty result is a valid state, not an error.
- Executor users receive only assigned tasks from the backend and the UI labels the page as "my tasks".

### Create From Order

Behavior:

- Calls `POST /api/production-tasks/from-order`.
- Requires an order, at least one selected order item, a non-blank task purpose, positive quantity, and optional executor/planned dates.
- Supports creating more than one task for the same order item when each task has a distinct purpose.
- On success, navigates to the created task detail or task list according to existing page flow.
- On validation failure, maps backend field errors to form feedback.

## use-production-task-detail.ts

### Load Task Detail

Behavior:

- Calls `GET /api/production-tasks/{id}`.
- Shows source order, order item, task fields, executor, planned dates, status, allowed actions, and history timeline.
- Keeps `version` for stale update protection.
- Shows a visibility/permission state when the backend returns `403`.

### Assign Or Plan Task

Behavior:

- Calls `PUT /api/production-tasks/{id}/assignment` with `expectedVersion`.
- Supports exactly one executor and planned start/finish dates.
- On success, refreshes detail and list caches.
- Completed tasks show assignment/planning fields as read-only.

## use-production-task-workflow.ts

### Load Allowed Actions

Behavior:

- Uses `allowedActions` from task detail to decide which buttons to show.
- Shows no workflow actions for completed tasks.
- Shows `START`, `BLOCK`, `UNBLOCK`, and `COMPLETE` only when allowed by role, assignment, current status, and stale state.

### Apply Status

Behavior:

- Calls `POST /api/production-tasks/{id}/status` with `expectedVersion`, `toStatus`, optional `note`, and required `reason` for `BLOCKED`.
- On success, refreshes task detail/list and any production counters.
- On invalid transition, forbidden assignment, or stale version, shows a clear error and leaves local task data unchanged.

## Routes And Screens

### ProductionTasksListPage

Behavior:

- Displays a table/card list of visible production tasks.
- Supports filters for search, status, executor, assigned-to-me, blocked-only, active-only, and date range.
- Shows task number, source order, customer, item, purpose, status, assignee, planned finish date, and blocked reason when present.
- Provides an empty state with a clear path for authorized order managers to create tasks from orders.

### ProductionTaskDetailPage

Behavior:

- Displays source order and order item context with navigation back to the order.
- Shows assignment/planning controls only for users allowed to plan/assign.
- Shows workflow buttons according to allowed actions.
- Shows history timeline with creation, assignment, planning, status, block, unblock, and completion events.

## Permission Behavior

- `ADMIN` users see create, assign/plan, and all status actions.
- `ORDER_MANAGER` users see create and assign/plan actions, but not executor-only status actions unless also holding production status permissions.
- `PRODUCTION_SUPERVISOR` users see all tasks, assign/plan actions, and all status actions.
- `PRODUCTION_EXECUTOR` users see only assigned tasks and status actions only for those assigned tasks.
- If the backend returns `403`, the UI must show an access message even if the action was visible due to stale role state.

## Regression Expectations

- Unit tests cover API parameter mapping, response mapping, validation error mapping, stale update handling, executor assigned-only behavior, and permission-driven action visibility.
- Existing `no-frappe-runtime` regression must remain green.
- Frontend build must pass with no placeholder data used for production task behavior.
