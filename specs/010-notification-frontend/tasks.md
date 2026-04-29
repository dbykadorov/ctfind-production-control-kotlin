# Tasks: Фронтенд уведомлений

**Input**: Design documents from `specs/010-notification-frontend/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Included — source-text TDD tests following project conventions (readFileSync + string assertions).

**Organization**: US1 (Badge) and US2 (Dropdown) are both P1 and tightly coupled (bell component hosts both). US3 (Page) and US4 (Sidebar) are P2 and independent. Backend targetEntityId change is foundational — blocks navigation in US2/US3.

**Constitution**: Frontend components handle display only. Backend remains API-only with JWT auth. No business rules in Vue components. All notification data from backend API via existing axios interceptor.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Paths: `frontend/cabinet/src/` for frontend, `src/main/kotlin/...` for backend

## Phase 1: Setup

**Purpose**: Types, i18n keys, utility functions — shared foundation

- [X] T001 [P] Create notification TypeScript types in `frontend/cabinet/src/api/types/notifications.ts`: NotificationResponse, NotificationType, NotificationTargetType, NotificationsPageResponse, UnreadCountResponse, MarkReadResponse, MarkAllReadResponse
- [X] T002 [P] Add notifications i18n namespace to `frontend/cabinet/src/i18n/keys.ts`: nav.notifications, meta.title.notifications, notifications.{title, empty, markAllRead, allNotifications, unreadOnly, badge99plus, timeJustNow, timeMinAgo, timeHoursAgo, timeYesterday, timeDaysAgo, types.TASK_ASSIGNED, types.STATUS_CHANGED, types.TASK_OVERDUE}
- [X] T003 [P] Add Russian translations to `frontend/cabinet/src/i18n/ru.ts` for all keys added in T002
- [X] T004 [P] Create relative time formatter in `frontend/cabinet/src/utils/relative-time.ts`: relativeTime(iso: string) → Russian strings (только что, X мин назад, X ч назад, вчера, X дн назад, dd.MM.yyyy fallback)
- [X] T005 [P] Create navigation helper in `frontend/cabinet/src/utils/notification-route.ts`: notificationRoute(n: NotificationResponse) → RouteLocationRaw | null, mapping PRODUCTION_TASK → production-tasks.detail, ORDER → orders.detail, null → null

---

## Phase 2: Foundational (Backend targetEntityId)

**Purpose**: Add targetEntityId to backend Notification entity and API response — BLOCKS frontend navigation

**⚠️ CRITICAL**: Without targetEntityId, click-to-navigate in dropdown/page is impossible.

- [X] T006 Create Flyway migration `src/main/resources/db/migration/V10__add_notification_target_entity_id.sql`: ALTER TABLE notification ADD COLUMN target_entity_id UUID
- [X] T007 Add `targetEntityId: UUID?` field to Notification domain entity in `src/main/kotlin/com/ctfind/productioncontrol/notifications/domain/Notification.kt`
- [X] T008 Add `targetEntityId: UUID?` to CreateNotificationCommand in `src/main/kotlin/com/ctfind/productioncontrol/notifications/application/NotificationPorts.kt`
- [X] T009 [P] Update JPA entity mapping in `src/main/kotlin/com/ctfind/productioncontrol/notifications/adapter/persistence/NotificationJpaEntities.kt` to include targetEntityId column
- [X] T010 [P] Add targetEntityId to NotificationResponse DTO and toResponse() mapper in `src/main/kotlin/com/ctfind/productioncontrol/notifications/adapter/web/NotificationDtos.kt`
- [X] T011 Update all three trigger call sites to pass entity UUID as targetEntityId: `AssignProductionTaskUseCase.kt` (saved.id), `ChangeProductionTaskStatusUseCase.kt` (saved.id), `OverdueTaskNotificationJob.kt` (task.id) in `src/main/kotlin/com/ctfind/productioncontrol/production/application/`
- [X] T012 Update all existing test files that create CreateNotificationCommand or mock NotificationCreatePort to include targetEntityId parameter (scan all test files in notifications/ and production/ test packages)
- [X] T013 Run `make backend-test-docker` — verify all backend tests pass with targetEntityId changes

**Checkpoint**: Backend API now returns targetEntityId. Frontend can use it for navigation.

---

## Phase 3: User Story 1+2 — Bell + Badge + Dropdown (Priority: P1) 🎯 MVP

**Goal**: Bell icon in TopBar with unread badge (polling 30s) + dropdown with notification list, mark-read, mark-all-read, navigation

**Independent Test**: Login → see badge → click bell → see dropdown → click notification → navigate to object

### Tests for US1+US2

- [X] T014 [P] [US1] Create source-text test for notification store in `frontend/cabinet/tests/unit/stores/notifications.test.ts`: verify store exports unreadCount, startPolling, stopPolling, fetchDropdown, markRead, markAllRead
- [X] T015 [P] [US2] Create source-text test for NotificationBell component in `frontend/cabinet/tests/unit/components/NotificationBell.test.ts`: verify uses useNotificationStore, renders Bell icon, shows badge when unreadCount > 0, renders NotificationDropdown, handles click
- [X] T016 [P] [US1] Create source-text test for use-notifications composable in `frontend/cabinet/tests/unit/composables/use-notifications.test.ts`: verify composable exports data/loading/error/refetch, uses httpClient, supports page/unreadOnly params

### Implementation for US1+US2

- [X] T017 [US1] Create Pinia notification store in `frontend/cabinet/src/stores/notifications.ts`: unreadCount ref, dropdownItems ref, dropdownLoading ref, fetchUnreadCount(), fetchDropdown() (size=10), markRead(id), markAllRead(), startPolling()/stopPolling() with 30s interval + document.visibilitychange pause/resume
- [X] T018 [US1] Create notifications API composable in `frontend/cabinet/src/api/composables/use-notifications.ts`: useNotifications() following useAuditLog pattern → { data, loading, error, refetch } with page/size/unreadOnly params, AbortController
- [X] T019 [P] [US2] Create NotificationItem component in `frontend/cabinet/src/components/domain/notifications/NotificationItem.vue`: renders single notification row — type icon (UserPlus/ArrowRightLeft/AlertTriangle), title, relativeTime, unread highlight (bg color or dot), click handler
- [X] T020 [US2] Create NotificationDropdown component in `frontend/cabinet/src/components/domain/notifications/NotificationDropdown.vue`: header with "Уведомления" title + "Отметить все прочитанными" button, list of NotificationItem (max 10 from store.dropdownItems), empty state, footer with "Все уведомления" RouterLink to /cabinet/notifications
- [X] T021 [US1] Create NotificationBell component in `frontend/cabinet/src/components/domain/notifications/NotificationBell.vue`: Bell icon button, badge with unreadCount (hidden if 0, "99+" if >99), Popover wrapping NotificationDropdown, on-open calls store.fetchDropdown(), click-outside closes
- [X] T022 [US1] Integrate NotificationBell into TopBar in `frontend/cabinet/src/components/layout/TopBar.vue`: import and place NotificationBell before user dropdown button
- [X] T023 [US1] Start/stop polling lifecycle: call store.startPolling() in AppShell or router afterEach, call store.stopPolling() on logout in `frontend/cabinet/src/stores/notifications.ts` integration with auth store

**Checkpoint**: Bell + badge + dropdown functional. Badge polls every 30s. Dropdown shows notifications with mark-read and navigation.

---

## Phase 4: User Story 3 — Notifications Page (Priority: P2)

**Goal**: Full notifications page at /cabinet/notifications with pagination, unreadOnly filter, mark-all-read

**Independent Test**: Navigate to /cabinet/notifications → see list with pagination → filter unread → click notification → navigate

### Tests for US3

- [X] T024 [P] [US3] Create source-text test for NotificationsPage in `frontend/cabinet/tests/unit/pages/NotificationsPage.test.ts`: verify uses useNotifications composable, renders table/list, has pagination controls, has unreadOnly toggle, has mark-all-read button, renders NotificationItem components

### Implementation for US3

- [X] T025 [US3] Create NotificationsPage in `frontend/cabinet/src/pages/notifications/NotificationsPage.vue`: full page following AuditLogPage pattern — header with title + "Отметить все прочитанными" button, unreadOnly toggle, loading skeleton, error banner, empty state, list of NotificationItem, pagination (prev/next, page X of Y, 20 per page)
- [X] T026 [US3] Add route for /cabinet/notifications in `frontend/cabinet/src/router/index.ts`: path 'notifications', name 'notifications.list', lazy import NotificationsPage, meta: { title: 'meta.title.notifications' }

**Checkpoint**: Notifications page functional with pagination and filters.

---

## Phase 5: User Story 4 — Sidebar Item (Priority: P2)

**Goal**: "Уведомления" navigation item in Sidebar for all authenticated roles

**Independent Test**: Login with any role → see "Уведомления" in sidebar → click → navigate to /cabinet/notifications

- [X] T027 [US4] Add "Уведомления" NavItem to Sidebar in `frontend/cabinet/src/components/layout/Sidebar.vue`: import Bell from lucide-vue-next, add item { to: '/cabinet/notifications', icon: Bell, key: 'nav.notifications', visible: true }

**Checkpoint**: Sidebar shows "Уведомления" for all roles, navigates correctly.

---

## Phase 6: Polish & Verification

**Purpose**: Cross-cutting verification

- [X] T028 Run `pnpm typecheck` in `frontend/cabinet/` — no TypeScript errors
- [X] T029 Run `pnpm test` in `frontend/cabinet/` — all tests pass (existing + new)
- [X] T030 Run `pnpm build` in `frontend/cabinet/` — production build succeeds
- [X] T031 Run `make docker-up-detached && make health` — stack healthy
- [X] T032 Run quickstart.md manual verification: login, check badge, open dropdown, click notification, mark-all-read, check page, check sidebar

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — T001-T005 all parallel
- **Foundational (Phase 2)**: No dependency on Phase 1 (backend-only), BLOCKS navigation in US2/US3
- **US1+US2 (Phase 3)**: Depends on Phase 1 (types, i18n) + Phase 2 (targetEntityId for navigation)
- **US3 (Phase 4)**: Depends on Phase 1 + Phase 2 + T018 (composable from US1+US2)
- **US4 (Phase 5)**: Depends on Phase 1 (i18n) + T026 (route from US3)
- **Polish (Phase 6)**: Depends on all phases complete

### Within Each Phase

- Tests written first (TDD)
- Shared components before page-level components
- Store before components that consume it
- Route definition after page component exists

### Parallel Opportunities

- T001 + T002 + T003 + T004 + T005 — all Phase 1 tasks in parallel (different files)
- T009 + T010 — parallel (different backend files)
- T014 + T015 + T016 — all test tasks in parallel (different test files)
- T019 can run in parallel with T017 (different files)
- Phase 1 and Phase 2 can run in parallel (frontend vs backend)

---

## Implementation Strategy

### MVP First (US1+US2 Only)

1. Phase 1: Setup (types, i18n, utils) — all parallel
2. Phase 2: Backend targetEntityId change
3. Phase 3: Bell + Badge + Dropdown
4. **STOP and VALIDATE**: Login → badge visible → dropdown works → navigate by click

### Incremental Delivery

1. Setup + Backend change → foundation ready
2. US1+US2 (Bell + Dropdown) → test → verify ✅ (MVP!)
3. US3 (Page) → test → verify ✅
4. US4 (Sidebar) → test → verify ✅
5. Polish → full verification

---

## Notes

- US1 (Badge) and US2 (Dropdown) are merged into one phase because they share NotificationBell component
- No new npm dependencies — reusing existing Popover, lucide-vue-next icons, axios, Pinia
- Backend targetEntityId migration is V10 — verify no V10 exists already before running
- Frontend tests use source-text TDD pattern (readFileSync + string assertions), NOT component mounting
- Polling pauses on hidden tab (document.visibilitychange) to save resources
- Badge shows "99+" for counts > 99
