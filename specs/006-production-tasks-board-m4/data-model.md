# Data Model: Production Tasks Board (M4)

This feature adds **no backend persistence**. There are no new tables, JPA entities, Flyway migrations, or domain entities. All data continues to come from the existing production task tables and DTOs introduced in feature 005.

This document is therefore limited to the **frontend view model** that backs the board page.

## Frontend View Model

### `ProductionTaskBoardCard`

A read-only projection over the existing `ProductionTaskListRowResponse` (already shipped in `frontend/cabinet/src/api/types/production-tasks.ts`). The board does not transform the row into a new shape — the card consumes the row directly. The «card» concept is purely a UI grouping; it does not warrant a new TS interface.

Fields read from `ProductionTaskListRowResponse` (no changes required):

- `id: string` — used as router target
- `taskNumber: string` — card title
- `purpose: string` — main subtitle
- `order.orderNumber: string` + `order.customerDisplayName: string` — context line
- `executor?: ProductionTaskExecutorSummary` — assignee chip (or «не назначен»)
- `plannedFinishDate?: string` — date display, drives overdue badge
- `status: ProductionTaskStatus` — selects the column
- `blockedReason?: string` — only used when `status === 'BLOCKED'`, truncated to 2 lines
- `updatedAt: string` — used by COMPLETED-window filter

### `ProductionTasksBoardData`

The composable's grouped result. Lives in `frontend/cabinet/src/api/composables/use-production-tasks-board.ts`.

```ts
interface ProductionTasksBoardData {
  /** Tasks per status column, already filtered/capped per FR-015. */
  byStatus: Record<ProductionTaskStatus, ProductionTaskListRowResponse[]>
  /** Total number of tasks across all four columns currently rendered. */
  totalVisible: number
  /**
   * True when the API page returned `totalItems > size` and the board is showing
   * only the first page. The page surfaces a non-blocking truncation banner.
   */
  truncated: boolean
}
```

`Record<ProductionTaskStatus, ...>` MUST contain entries for all four statuses, even when empty, so the page can render four columns deterministically.

### Status → column mapping

Strict 1:1 mapping; column order is fixed left-to-right:

| Column header (RU) | `ProductionTaskStatus` |
|---|---|
| Не начато | `NOT_STARTED` |
| В работе | `IN_PROGRESS` |
| Заблокировано | `BLOCKED` |
| Выполнено | `COMPLETED` |

Display labels reuse `productionTaskStatusLabelRu(status)` from the existing query use case via the API response's `statusLabel` field; the board does not invent its own translations.

## Filtering Rules (frontend-only)

### COMPLETED window (FR-015)

A task appears in the COMPLETED column only when **all** of the following hold:

- `status === 'COMPLETED'`
- `updatedAt` parsed as ISO timestamp is `>= now − 7 days`
- Among the surviving completed tasks, only the 30 with the largest `updatedAt` (descending) are kept; the rest are dropped.

The cap is implemented in `useProductionTasksBoard` so pages and tests share one source of truth. The list view remains unaffected by this cap.

### Overdue badge (FR-003 reuses list rule)

Same predicate as the list view's `isOverdue(row)`:

- `plannedFinishDate` parsed and `< startOfToday()`
- AND `status !== 'COMPLETED'`

### «Only overdue» toggle (FR-008)

Client-side filter applied AFTER status grouping but BEFORE the COMPLETED window cap (so toggling this empties the COMPLETED column by definition — completed tasks are never overdue per the rule).

### Truncation rule

The composable issues `GET /api/production-tasks?size=200&...filters`. If the response's `totalItems` exceeds `size`, the composable sets `data.truncated = true`. The page then renders a non-blocking banner suggesting the user refine filters; columns still render the available 200 rows.

## Backend View

No changes. For reference, the source of truth for these rows is feature 005's `ProductionTaskQueryUseCase.list(...)` projection through `ProductionTaskListRowView` and the controller mapper in `ProductionTaskDtos.kt`. The board does not depend on any field that 005 doesn't already emit.
