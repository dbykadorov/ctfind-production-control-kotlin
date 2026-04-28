# Implementation Plan: Production Tasks Board (M4)

**Branch**: `006-production-tasks-board-m4` | **Date**: 2026-04-28 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/006-production-tasks-board-m4/spec.md`

## Summary

Add a kanban-style board view of production tasks at `/cabinet/production-tasks/board`, complementing (not replacing) the existing flat list page from feature 005. Tasks are rendered as cards in four status columns (NOT_STARTED / IN_PROGRESS / BLOCKED / COMPLETED). The page reuses the existing `GET /api/production-tasks` endpoint with no backend changes — visibility, executor assigned-only filtering, and role gating are inherited from feature 005. The COMPLETED column is capped at 30 most-recent completed tasks within the last 7 days; the BLOCKED column shows the `blockedReason` truncated to two lines on the card. Status mutations remain on the detail page; the board is navigation + observation only.

## Technical Context

**Language/Version**: TypeScript 5.7, Vue 3.5 (existing frontend), no backend changes
**Primary Dependencies**: Vue Router, Pinia, Axios, Vitest, vue-i18n, Tailwind, date-fns, lucide-vue-next (all already installed in `frontend/cabinet`)
**Storage**: N/A (no new persistence; reads `/api/production-tasks`)
**Testing**: Vitest + @vue/test-utils for new pages/composables/components; existing router-guard test extended for the new route
**Target Platform**: Browser (cabinet SPA) on desktop and 10–12" tablet in landscape orientation
**Project Type**: Frontend extension on top of existing Spring/Kotlin backend (no backend module changed)
**Performance Goals**: Render the first page of visible tasks within 2 s at the Phase 1 dataset size (≤ 200 visible tasks per fetch); no auto-refresh
**Constraints**:

- API-only backend remains untouched; no new endpoints, DTOs, or migrations.
- Server-side visibility (executor sees only assigned) MUST stay authoritative; no client-side trust check is added.
- No drag-and-drop, no real-time push, no per-user saved board layouts.
- Columns must reflow into a horizontal scroll strip on viewports narrower than the tablet-landscape breakpoint (1024 px).

**Scale/Scope**: Phase 1 — up to 50 users, several hundred tasks per active dataset; the board fetches up to 200 tasks per pull and shows a truncation notice when more are visible.

## Constitution Check

*GATE: Pre-Phase-0.*

- **ERP domain fit** — PASS. The board strengthens the order-to-production workflow by giving the shop supervisor a stage-centric view of work-in-progress without introducing a new operational entity. It re-presents existing production tasks.
- **Constraint-aware operations** — PASS. The board preserves and surfaces every TOC-relevant fact already produced by feature 005: status, blocked reason, executor assignment, planned dates, overdue signal, history (still on detail). It does not hardcode a single priority model — the column order follows the lifecycle, not FIFO/created-date. WIP limits and bottleneck flags are intentionally deferred to a later feature; the board UI does not preclude them.
- **Architecture boundaries** — PASS. Feature is a frontend extension only. No domain or application code changes; the existing query use case and persistence adapter remain authoritative for visibility. The new view model lives in a frontend composable + page; no business rules leak out of the application/domain layer.
- **Traceability & auditability** — PASS. No new auditable mutations. The board observes the existing `production_task_audit_event` and history flows untouched. Status / assignment / planning changes continue to write history and audit records via the feature-005 detail page.
- **API-only & explicit security** — PASS. No backend changes. The existing JWT-protected `GET /api/production-tasks` is the only data source. Frontend route is gated by the same role allowlist as the list route (`ADMIN`, `ORDER_MANAGER`, `PRODUCTION_SUPERVISOR`, `PRODUCTION_EXECUTOR`); roles outside this allowlist receive `/cabinet/403`. Executor assigned-only visibility is enforced server-side.
- **Docker-first verifiable delivery** — PASS. Root Docker workflow remains unchanged. Verification = `make backend-test-docker` (regression-only, since no backend code changed), `pnpm test`/`pnpm build` for the cabinet, `docker compose up --build --wait` for stack health, and a manual board smoke against the live cabinet.
- **Exception handling** — None. No constitution principle is violated.

## Project Structure

### Documentation (this feature)

```text
specs/006-production-tasks-board-m4/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (board view model only)
├── quickstart.md        # Phase 1 output — local verification flow
├── contracts/
│   └── frontend-production-tasks-board.md
├── checklists/
│   └── requirements.md  # already PASS
└── tasks.md             # produced later by /speckit-tasks
```

### Source Code (repository root)

```text
frontend/cabinet/src/
├── pages/production/
│   └── ProductionTasksBoardPage.vue        # NEW — board page
├── components/domain/
│   └── ProductionTaskBoardCard.vue         # NEW — task card on the board
├── api/composables/
│   └── use-production-tasks-board.ts       # NEW — board fetch + grouping
├── router/
│   └── index.ts                            # EDIT — add board route + nav entry
├── components/layout/
│   └── (existing nav components)           # EDIT — add board entry next to list
└── i18n/
    └── (existing locales)                  # EDIT only if new strings need i18n keys

frontend/cabinet/tests/unit/
├── pages/ProductionTasksBoardPage.test.ts  # NEW
├── composables/use-production-tasks-board.test.ts  # NEW
├── components/ProductionTaskBoardCard.test.ts      # NEW
└── router/guard.test.ts                            # EDIT — add board-route cases
```

**Backend**: no source changes. `make backend-test-docker` runs only as a regression check.

**Structure Decision**: Frontend-only extension under the existing `frontend/cabinet` cabinet SPA. No new module on the backend. Tests follow the same layout as feature 005 (`tests/unit/pages`, `tests/unit/composables`, `tests/unit/components`, `tests/unit/router`).

## Phase 0: Outline & Research

See [research.md](./research.md). Key decisions:

- **Data fetch shape**: a single `GET /api/production-tasks?size=200` per board mount; client groups by `status`. The COMPLETED column is filtered client-side to the last 7 days × max 30 most-recent items by `updatedAt`. No new backend params.
- **Tablet breakpoint**: Tailwind `lg:` (≥ 1024 px) shows all four columns side-by-side; below that the column strip becomes `overflow-x-auto` with each column at fixed `min-w-[18rem]`.
- **Truncation notice**: when `totalItems > 200`, render a non-blocking banner above the columns with the suggestion to refine filters. The list view remains the authoritative full list.
- **Refresh model**: on mount + via a manual «Обновить» button. No `setInterval`. No router-level data refetch on focus for Phase 1.
- **Composable shape**: `useProductionTasksBoard(filters)` returns `{ data, loading, error, refetch }` similar to `useProductionTasksList`, with `data` shaped as `{ byStatus: Record<ProductionTaskStatus, ProductionTaskListRowResponse[]>, totalVisible: number, truncated: boolean }`.

## Phase 1: Design & Contracts

Design artifacts:

- [data-model.md](./data-model.md) — frontend view model (`ProductionTaskBoardCard`, `ProductionTasksBoardData`) plus the COMPLETED-window filter rule. No new persistence.
- [contracts/frontend-production-tasks-board.md](./contracts/frontend-production-tasks-board.md) — route, composable signature, page behavior, card props, navigation entry, role guard expectations.
- [quickstart.md](./quickstart.md) — local verification sequence: backend regression, frontend test/build, docker startup, board smoke.

### Agent context update

The `<!-- SPECKIT START -->` block in `CLAUDE.md` will be updated by `/speckit-plan` to point at this plan: `specs/006-production-tasks-board-m4/plan.md`.

## Post-Design Constitution Check

- **ERP domain fit** — PASS. Re-confirmed after design: board view-model and frontend contract do not introduce new domain concepts; production task remains the single source of truth.
- **Constraint-aware operations** — PASS. The COMPLETED-window filter (last 7 days × max 30) is documented as a UI rule, not a backend constraint, so it does not bias future TOC analysis. All TOC-relevant fields stay on the API and remain unfiltered for the list view.
- **Architecture boundaries** — PASS. The view model lives in `api/composables/use-production-tasks-board.ts` and pages/components; no domain/application code in adapters or page logic.
- **Traceability/audit** — PASS. No new business mutations; existing audit/history flows are unchanged.
- **API-only/security** — PASS. Confirmed in `contracts/frontend-production-tasks-board.md`: backend untouched; role guard reuses the existing list-route allowlist.
- **Docker/verifiability** — PASS. `quickstart.md` lists fresh checks (`pnpm test`, `pnpm build`, docker stack up, manual board smoke).
- **Exception handling** — None.

## Complexity Tracking

No constitution violations or exceptional complexity introduced. Empty.
