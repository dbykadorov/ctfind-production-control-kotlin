# Research: Фронтенд уведомлений

## Decision 1: Polling vs Reactive updates

**Decision**: Polling GET /api/notifications/unread-count every 30 seconds via `setInterval` in a Pinia store.

**Rationale**: WebSocket/SSE explicitly out of scope (M7 Phase 1). Polling is the simplest approach, consistent with the spec. The unread-count endpoint is lightweight (single integer). 30-second interval balances freshness vs server load.

**Alternatives considered**:
- WebSocket/SSE — rejected, out of scope for Phase 1
- Polling from composable — rejected; store is better because badge state is global (TopBar) and must persist across page navigations
- Polling only when tab visible — adopted as optimization; use `document.visibilitychange` to pause/resume

## Decision 2: Notification state management — Pinia store vs composable

**Decision**: Dedicated Pinia store `useNotificationStore` for unread count + dropdown data. Separate composable `useNotifications` for the full-page list with pagination.

**Rationale**: The unread count badge lives in TopBar and must survive page navigation. A Pinia store is the right primitive for global reactive state. The dropdown loads the latest 10 on open — the store caches them. The full notifications page is a standard list page — a composable following `useAuditLog` pattern fits perfectly.

**Alternatives considered**:
- Single composable for everything — rejected; composable lifecycle is tied to component, badge needs global persistence
- Single store for everything — rejected; full page list with pagination is a local concern, not global

## Decision 3: targetEntityId resolution for navigation

**Decision**: Add `targetEntityId: UUID?` field to backend `NotificationResponse` DTO and `Notification` domain entity. Populate it at notification creation time by storing the entity UUID alongside the human-readable targetId.

**Rationale**: The frontend routes require UUID (`/cabinet/production-tasks/:id`, `/cabinet/orders/:id`), but `targetId` contains human-readable numbers (PT-000001, ORD-000005). Without UUID, the frontend would need a lookup-by-number API that doesn't exist. Storing UUID at creation time is cheapest — the creator (trigger code) already has the entity in hand.

**Alternatives considered**:
- Frontend searches by number — rejected; no search-by-number endpoint exists, would need new backend work for each entity type
- Store UUID in targetId instead of number — rejected; breaks dropdown display (users see UUID instead of task number) and all existing notifications
- Resolve at query time via JOIN — rejected; requires JOIN across modules (notification → production_task/order), violates hexagonal boundaries

**Backend changes required**:
1. Add `targetEntityId: UUID?` to `Notification` domain entity
2. Add `target_entity_id` column to `notification` table (Flyway migration)
3. Add `targetEntityId` to `CreateNotificationCommand`
4. Update all trigger call sites (AssignProductionTaskUseCase, ChangeProductionTaskStatusUseCase, OverdueTaskNotificationJob) to pass entity UUID
5. Add `targetEntityId` to `NotificationResponse` DTO

## Decision 4: Dropdown component — Popover vs custom

**Decision**: Use existing `Popover` component from `src/components/ui/popover/`.

**Rationale**: The project already has a Popover UI primitive (shadcn-style). The user dropdown in TopBar uses a custom menu implementation, but Popover provides the right UX: click to open, click outside to close, positioned relative to trigger. Reusing existing components avoids new dependencies.

**Alternatives considered**:
- Custom div with v-if — rejected; would need to implement click-outside detection and positioning manually
- Dialog/Modal — rejected; dropdown UX is better for quick glance at notifications

## Decision 5: Relative time formatting

**Decision**: Use a lightweight helper function (no library dependency) that formats ISO timestamps into Russian relative time strings: "только что", "5 мин назад", "2 ч назад", "вчера", "3 дня назад", then falls back to "dd.MM.yyyy".

**Rationale**: The project has no date library (no dayjs/date-fns). Adding a full library for one use case is overkill. A ~30-line helper covers all needed cases. The audit log already uses a manual `formatTime()` function.

**Alternatives considered**:
- `Intl.RelativeTimeFormat` — partial fit; browser API but requires manual unit selection logic, limited Russian support in older browsers
- date-fns — rejected; unnecessary dependency for one utility
- dayjs — rejected; same reason

## Decision 6: Test approach

**Decision**: Source-text TDD tests following the established pattern in the project (readFileSync + string assertions).

**Rationale**: The project consistently uses source-text tests (AuditLogPage.test.ts, ProductionTasksListPage.test.ts). These tests verify that components use the right composables, render expected UI elements, and handle states correctly. This approach is fast, doesn't require JSDOM/component mounting, and matches the team's testing conventions.

**Alternatives considered**:
- Component mounting with @vue/test-utils — rejected; project doesn't use this pattern
- E2E with Playwright — rejected; too heavy for this scope, no existing E2E infrastructure
