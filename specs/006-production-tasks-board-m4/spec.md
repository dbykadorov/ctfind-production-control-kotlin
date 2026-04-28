# Feature Specification: Production Tasks Board (M4)

**Feature Branch**: `006-production-tasks-board-m4`
**Created**: 2026-04-28
**Status**: Draft
**Input**: User brief covering M4 «доска задач мастера» from `docs/PHASE_01.md` — a board view of production tasks grouped by status, complementing the existing flat list (`ProductionTasksListPage.vue` from feature 005).

## Clarifications

### Session 2026-04-28

- Q: Должен ли PRODUCTION_EXECUTOR иметь доступ к доске и в каком виде? → A: Полная доска со всеми четырьмя колонками; видимость по-прежнему фильтруется на сервере, поэтому исполнитель видит в столбцах только назначенные ему задачи (Option A).
- Q: Как должна вести себя колонка «Выполнено»? → A: Всегда видна с лимитом не более 30 задач, завершённых за последние 7 дней; более старые завершённые задачи на доске не показываются (Option A).
- Q: Где показывать `blockedReason` на BLOCKED-карточке? → A: Прямо под номером задачи на самой карточке, с усечением длинного текста до 2 строк (полный текст доступен на детальной странице) (Option A).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Shop supervisor sees task distribution at a glance (Priority: P1)

A shop supervisor (role «Мастер / начальник цеха») opens the production tasks board and sees current production work split into status columns: «Не начато» / «В работе» / «Заблокировано» / «Выполнено». Each column lists the visible tasks as cards with their key context (task number, purpose, source order + customer, executor, planned finish date, overdue and blocked indicators). Clicking any card opens the existing production task detail page where the supervisor can apply the workflow actions delivered in feature 005.

**Why this priority**: This is the primary workflow that closes Phase 1 §M4 «доска задач мастера». Without it the supervisor has only a flat list and cannot quickly judge how much work is in each stage of production. All later stories (filters, executor view) extend this MVP behaviour but the board itself must work first.

**Independent Test**: Sign in as a shop supervisor (or admin / order manager) with at least one production task in each of the four statuses, open the new board route, and verify each task appears in the column matching its status, with the same metadata shown on the existing list. Click any card and confirm navigation to the task detail page.

**Acceptance Scenarios**:

1. **Given** a supervisor session with tasks in NOT_STARTED, IN_PROGRESS, BLOCKED, and COMPLETED states, **When** the supervisor opens the production tasks board, **Then** four status columns are rendered and each task appears in the column corresponding to its current status.
2. **Given** a task whose planned finish date is in the past and status is not COMPLETED, **When** that task is rendered as a card, **Then** the card shows an overdue indicator using the same rule as the list view.
3. **Given** a card on any column, **When** the supervisor clicks it, **Then** the application navigates to the existing production task detail page for that task.
4. **Given** a blocked task with a non-empty `blockedReason`, **When** the card is rendered, **Then** the reason text is visible directly on the card under the task number, truncated to two lines on overflow (the full text remains available on the detail page).
5. **Given** an authenticated user without permission to see production tasks, **When** the user attempts to open the board route, **Then** access is refused per the same role policy as the list route (redirect to the existing forbidden page).

---

### User Story 2 - Filter the board to focus on a slice of work (Priority: P2)

The supervisor narrows the board down to a particular slice — by free-text search (task number / customer / purpose), by executor, by planned-finish date range, and by an «only overdue» toggle. The board re-renders with the same column layout but only matching cards appear. The filter set is the same one already exposed on the list page where it makes sense for a board.

**Why this priority**: Even at Phase 1 scale (~50 users, hundreds of tasks) the board can become noisy. Filters keep the board usable without forcing the supervisor back to the list view. The board still works without filters (P1), so this is an improvement, not a blocker.

**Independent Test**: With ten or more tasks across columns, apply each filter individually (search by partial task number, pick an executor, set a date range, enable «only overdue») and verify the columns shrink to exactly the matching subset while preserving column structure.

**Acceptance Scenarios**:

1. **Given** the board displays many tasks, **When** the supervisor types a partial task number into the search field, **Then** only cards matching that text remain.
2. **Given** a multi-executor team, **When** the supervisor picks one executor in the filter, **Then** every visible card has that executor.
3. **Given** the «only overdue» toggle is enabled, **When** the board re-renders, **Then** only cards meeting the overdue rule remain (and the COMPLETED column is empty by definition).
4. **Given** date-range filters that yield no matches, **When** the filter is applied, **Then** every column shows an empty-state message and no card.

---

### User Story 3 - Executor view of the board (Priority: P3)

A production executor («Исполнитель») opens the same board route as a supervisor and sees the full four-column layout with only their own assigned tasks distributed across the status columns. The executor uses the board to understand which of their tasks are in each stage and to navigate to the task detail page.

**Why this priority**: Executors already have a fully filtered list view from feature 005, so this view is convenience rather than a new capability. It can be deferred without blocking the supervisor's main workflow.

**Independent Test**: Sign in as a production executor with two tasks in different statuses, open the board, and verify all four columns are rendered, the executor's two tasks appear in the columns matching their statuses, and other executors' tasks are absent. Verify a direct GET to a task that is not assigned to the executor still produces a 403 from the existing detail endpoint.

**Acceptance Scenarios**:

1. **Given** an executor with assigned tasks in IN_PROGRESS and BLOCKED, **When** the executor opens the board, **Then** the IN_PROGRESS column shows their IN_PROGRESS task, the BLOCKED column shows their BLOCKED task, and the NOT_STARTED and COMPLETED columns are empty (or show only assigned tasks if any exist there).
2. **Given** an executor session, **When** the board issues its data fetch, **Then** the response set comes from the same server-side visibility rule already used by the list endpoint (no additional client-side trust check needed).
3. **Given** an executor with no assigned tasks, **When** they open the board, **Then** all four columns render with their empty states; no error and no «доска» entry needs to be hidden in navigation.

---

### Edge Cases

- The current user has zero visible tasks → every column shows an empty-state message; no errors or layout collapse.
- Total visible tasks exceed the page size limit (200): the board renders the first page only and shows a non-blocking notice that the result was truncated, prompting the user to refine filters.
- A task changes status between fetches: the board reflects whatever status was returned by the latest fetch; the user can press refresh to re-pull. Real-time push is out of scope.
- More than 30 tasks were completed in the last 7 days: the board shows only the 30 most recent on the COMPLETED column; older entries are not surfaced via the board (the list view remains the authoritative full archive).
- All completed tasks are older than 7 days: the COMPLETED column is rendered with its empty-state message; this is not an error.
- Network error while fetching: the board shows an error banner and a retry action; columns are empty during the error state.
- 403 from the API while fetching (e.g., role changed mid-session): the board shows the standard forbidden empty state and offers a link back to the cabinet.
- Tablet (10–12") landscape: columns fit horizontally; on narrower viewports columns reflow into a horizontally scrollable strip.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose a new in-cabinet route at `/cabinet/production-tasks/board` that renders the production tasks board.
- **FR-002**: The board MUST present production tasks grouped into four columns mapped to statuses NOT_STARTED, IN_PROGRESS, BLOCKED, COMPLETED, in that left-to-right order, with the same Russian status labels used elsewhere («Не начато», «В работе», «Заблокировано», «Выполнено»).
- **FR-003**: Each task card MUST display task number, purpose, source order number + customer, executor display name (or an explicit «не назначен» state), planned finish date, and an overdue indicator (using the same rule as the list view: planned finish date in the past AND status not COMPLETED). For tasks with status BLOCKED, the card MUST show the `blockedReason` text directly on the card under the task number, truncated to at most two lines on overflow.
- **FR-004**: Clicking a card MUST navigate to the existing production task detail route for that task.
- **FR-005**: The board MUST consume task data from the existing production tasks list endpoint and MUST NOT introduce new backend endpoints, persistence changes, or schema migrations.
- **FR-006**: The board MUST honour the same visibility rules as the list view: production executors MUST receive only tasks assigned to them; admins, order managers, and shop supervisors MUST receive all tasks they can already see in the list.
- **FR-007**: The board route MUST require the same role allowlist as the list route (admin, order manager, shop supervisor, production executor); other authenticated users MUST receive the forbidden page.
- **FR-008**: The board MUST provide filter controls for free-text search (task number / order / customer / purpose), executor, planned finish date range, and an «only overdue» toggle. Filters MUST update the board content without leaving the page.
- **FR-009**: The board MUST provide a manual refresh action; data refreshes on initial mount and on each refresh action. Real-time push is explicitly out of scope.
- **FR-010**: When the visible task count exceeds 200, the board MUST render the first 200 results in column order and display a non-blocking notice that the list was truncated, asking the user to refine filters. No infinite scroll for Phase 1.
- **FR-011**: The board layout MUST remain usable on a 10–12 inch tablet in landscape orientation. Columns MUST scroll horizontally as a group when the viewport is too narrow to show all four columns at full width.
- **FR-012**: Switching between «Список» (list) and «Доска» (board) MUST be available from the existing in-cabinet navigation; the board does not replace or hide the list view.
- **FR-013**: Status changes MUST NOT be initiated from the board itself for this feature: the user opens the detail page for any workflow action. Drag-and-drop is out of scope.
- **FR-014**: PRODUCTION_EXECUTOR MUST be able to open the same board route as supervisors and see the full four-column layout. The board MUST rely on the existing server-side visibility rule of the production tasks list endpoint so that executors see only tasks assigned to them in each column; the client MUST NOT add an additional role-aware filter on top.
- **FR-015**: The COMPLETED column MUST always be visible alongside the three active columns. It MUST display at most 30 most recently completed tasks AND only those completed within the past 7 days; tasks completed earlier MUST NOT appear on the board. The same role-based visibility rule from FR-006 applies to this column.
- **FR-016**: Long `blockedReason` text on BLOCKED cards MUST be truncated to at most two lines on the card (with an ellipsis or equivalent visual cue at the cut). The full reason text MUST remain accessible without further interaction by navigating to the existing task detail page; the board MUST NOT reproduce the full reason via expanding panels or modal popups.

### Key Entities

- **Production Task Card**: A read-only projection of an existing production task tailored to board rendering — task number, purpose, source order summary, executor display name, planned finish, overdue flag, blocked flag and reason, current status. No new persistence; this is a frontend view model on top of the existing list response.
- **Board View**: The composite of four status columns plus the active filter set and the truncation notice. Stateless on the server, ephemeral on the client (no saved per-user board configurations in this feature).

### Constitution Alignment *(mandatory)*

- **ERP/domain fit**: Strengthens the order-to-production workflow by giving the shop supervisor an executable, at-a-glance view of where production work currently sits in the lifecycle. Does not introduce a new operational entity; it re-presents existing tasks.
- **TOC readiness**: Reuses the same status, planned dates, executor assignment, and overdue facts produced by feature 005. The board makes those facts visible to the role responsible for the bottleneck without introducing new buffer or throughput primitives. WIP limits and TOC signals are intentionally deferred to a later feature; the board does not preclude them.
- **Traceability/audit**: No new auditable mutations. The board reads the existing visible task list; status / assignment / planning changes continue to flow through the feature 005 detail-page actions which already write history and audit events.
- **Security/API boundary**: No new backend endpoints; the existing JWT-protected production tasks list is the only data source. Frontend route is gated by the same role allowlist as the list route. Executor assigned-only visibility is enforced server-side, not on the client.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A shop supervisor can identify how many production tasks are in each lifecycle stage in under 5 seconds after opening the board (no scrolling needed at Phase 1 scale).
- **SC-002**: From the board, a supervisor can reach the detail page for any visible task in 1 click.
- **SC-003**: At least 80% of overdue tasks visible on the board are spotted within 10 seconds in a guided test, thanks to the overdue indicator.
- **SC-004**: 100% of role-restricted access attempts (a user without one of the allowed roles) result in the forbidden page rather than partial board content.
- **SC-005**: On a 10–12" tablet in landscape, all four columns are reachable (either fully visible or via single horizontal scroll gesture) without the supervisor needing to switch device orientation.
- **SC-006**: The board renders the first page of visible tasks within 2 seconds at the Phase 1 dataset size (up to several hundred tasks).
- **SC-007**: No regression on the existing list view: list-page tests and the route-guard tests for the list route continue to pass.

## Assumptions

- Feature 005 (production tasks list / detail / workflow) is shipped and remains the source of truth for status, history, and per-task workflow actions. The board does not duplicate any of those actions.
- The existing production tasks list endpoint already returns enough fields to render board cards (task number, purpose, order summary with customer, executor summary, planned dates, status, blocked reason, version, updated timestamp). No new fields are required from the backend.
- Phase 1 scale (≤ 50 users, hundreds of tasks) means a single fetch with `size=200` is acceptable; pagination on the board is not required for Phase 1.
- Real-time updates and drag-and-drop status changes are explicitly deferred to a later phase (Phase 2 or M7 internal notifications), per the brief.
- The «Список» ↔ «Доска» switch lives in the existing layout/navigation rather than as in-page tabs, so existing route-guard and in-cabinet navigation patterns apply unchanged.
- Tablet support targets a 10–12" landscape viewport per the cross-cutting UI/UX requirement in `docs/CTfind Production Control.md`. Mobile portrait is not a target for this feature.
- All three clarification questions (executor access, COMPLETED column policy, BLOCKED reason placement) were resolved in the 2026-04-28 clarification session and are now reflected in functional requirements FR-003, FR-014, FR-015, and FR-016.
