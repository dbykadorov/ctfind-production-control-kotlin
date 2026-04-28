# Research: Production Tasks Board (M4)

This document resolves the small set of design decisions that bridged spec.md to plan.md. None of the spec's `[NEEDS CLARIFICATION]` markers carried into this phase — all three were closed by the 2026-04-28 clarification session.

## R-001 Data fetch and client-side grouping

- **Decision**: Use the existing `GET /api/production-tasks` with `size=200` and group by `status` on the client. No new backend params, no new endpoint.
- **Rationale**: The list endpoint already supports the visibility filter executors require (server-side `assignedToMe` resolution from JWT roles), already returns every field the card needs (`taskNumber`, `purpose`, `order`, `executor`, `plannedFinishDate`, `status`, `blockedReason`, `updatedAt`). Phase 1 dataset (≤ several hundred tasks) fits comfortably in a single page; pagination on the board would add complexity without user value at this scale.
- **Alternatives considered**:
  - **Per-column fetch (4 requests)**: rejected. Adds latency, defeats single-truncation banner, complicates filter handling, and produces inconsistent counts when statuses change between requests.
  - **New `/api/production-tasks/board` endpoint with column groups built server-side**: rejected. Duplicates server logic, locks the column-shape policy into backend, and forces a Flyway migration if completion-window cap moves there later. Spec explicitly states no backend changes.
  - **Cursor pagination**: deferred. Out of Phase 1 scope.

## R-002 COMPLETED column filtering

- **Decision**: Apply the COMPLETED window filter on the client. Take only tasks where `status === 'COMPLETED'` AND `updatedAt >= now − 7d`, sort by `updatedAt desc`, and trim to first 30. Other status columns are not capped.
- **Rationale**: Completed tasks are read-only after completion (feature 005 enforces it: assignment, planning, and status changes reject completed tasks). For completed tasks, `updatedAt` therefore equals the completion timestamp in practice, making it a reliable proxy. Client-side filtering avoids a backend change and keeps the column policy mutable from the frontend if the cap needs tuning. The list view remains the authoritative archive for older completed tasks.
- **Alternatives considered**:
  - **Server-side `completedSince` / `statusChangedSince` filter**: rejected for Phase 1 (out of scope per spec; would require a Flyway migration to index `updatedAt` if performance ever became an issue).
  - **Hide COMPLETED column behind a toggle**: rejected during clarification (Q2 = A).
  - **No cap, full COMPLETED list**: rejected; column would dominate the screen on long-running projects.

## R-003 Tablet breakpoint and column reflow

- **Decision**: Show all four columns side-by-side at viewports ≥ 1024 px (Tailwind `lg:`). Below that, columns become a horizontally scrollable strip (`overflow-x-auto` on the row container, each column at `min-w-[18rem]` so two columns are visible at typical 768 px tablets in portrait).
- **Rationale**: 10–12" tablets in landscape land at 1024–1366 px CSS width — the most common landing size shows all four columns without scrolling. Below that, supervisors get a single horizontal-scroll gesture instead of vertical reflow that would break the kanban metaphor. Tailwind already exposes `lg:` so no new tokens or media queries are needed.
- **Alternatives considered**:
  - **Vertical stack on mobile**: rejected for Phase 1; the board UX is supervisor-/master-on-tablet, not phone-first. Spec says mobile portrait is not a target.
  - **Custom breakpoint at 900 px**: rejected; deviating from Tailwind defaults adds maintenance burden without payoff.

## R-004 Refresh model and staleness

- **Decision**: Refresh the board on mount (page navigation) and on manual click of the «Обновить» button. No `setInterval` polling. No `visibilitychange` listener for Phase 1.
- **Rationale**: Real-time push is explicitly deferred until M7 internal notifications (or later). Polling adds load on the backend and battery drain on tablets without product evidence the supervisor needs sub-minute freshness. Manual refresh keeps user agency clear: the supervisor knows when they're seeing fresh data because they pressed the button.
- **Alternatives considered**:
  - **Auto-refresh every 30 s on visible tab**: rejected for Phase 1; revisit when M7 notifications land and we have a real-time channel anyway.
  - **Refetch on `route.afterEach` to re-pull on every navigation**: rejected; the on-mount refetch already covers the supervisor returning from a detail-page status mutation.

## R-005 Composable shape

- **Decision**: New composable `useProductionTasksBoard(filters: Ref<ProductionTaskListFilters>): UseProductionTasksBoardResult`, returning `{ data: Ref<ProductionTasksBoardData | null>, loading, error, refetch }`. The `data` payload is grouped already (`byStatus: Record<ProductionTaskStatus, ProductionTaskListRowResponse[]>`), reports `totalVisible: number` and `truncated: boolean`, and applies the COMPLETED-window cap from R-002 inside the composable so the page renders directly without re-grouping.
- **Rationale**: Mirrors the shape of `useProductionTasksList` (familiar to existing tests) while encapsulating board-specific transformation. Page tests can mock the composable's return value; composable tests can mock `httpClient` and verify grouping + cap logic in isolation.
- **Alternatives considered**:
  - **Reuse `useProductionTasksList` and group on the page**: rejected; the page would need to know about the COMPLETED-window rule and the truncation banner, complicating page tests and bleeding logic into the view layer.
  - **Pinia store**: rejected; the board has no shared cross-page state, so a composable is sufficient and simpler.

## R-006 Filters parity with list view

- **Decision**: Reuse the same `ProductionTaskListFilters` shape. The board exposes only filters that make sense at a kanban view: `search`, `executorUserId`, `dueDateFrom/dueDateTo`, and a UI-only «only overdue» toggle. `status` is intentionally omitted (the columns ARE the status grouping). `assignedToMe`, `blockedOnly`, and `activeOnly` are not exposed on the board UI but the underlying request retains its server-side defaults.
- **Rationale**: Filters that conflict with the column structure (e.g., `status=NOT_STARTED`) would empty three of four columns, defeating the at-a-glance value. «Only overdue» is implemented client-side with the same overdue rule as the list-row badge, since it's a derived predicate over data already on the card.
- **Alternatives considered**:
  - **Expose every list-view filter on the board**: rejected; redundant with list view and confusing in column layout.
  - **Push «only overdue» to server**: rejected; the API has no `overdueOnly` param and the rule is purely derivable from `plannedFinishDate < today AND status !== COMPLETED` already known on the card.
