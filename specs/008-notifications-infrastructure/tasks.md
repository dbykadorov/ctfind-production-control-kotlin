# Tasks: Инфраструктура и модель внутренних уведомлений

**Input**: Design documents from `specs/008-notifications-infrastructure/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Included — this is a foundational infrastructure module where port contracts and security isolation must be verified.

**Organization**: Tasks grouped by user story. US3 (Create via port, P1) before US1 (List, P1) because listing depends on having notifications in the DB for meaningful tests.

**Constitution**: Domain-centered architecture (pure Kotlin domain, ports in application, JPA in adapter), API-only backend with JWT auth, Flyway migration, readAt for TOC reaction-time analysis.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Exact file paths relative to repository root

## Phase 1: Setup

**Purpose**: Create package structure for the new `notifications` module

- [X] T001 Create notifications module directory structure under `src/main/kotlin/com/ctfind/productioncontrol/notifications/{domain,application,adapter/{persistence,web}}` and corresponding test directories under `src/test/kotlin/com/ctfind/productioncontrol/notifications/{domain,application,adapter/{persistence,web}}`

---

## Phase 2: Foundational (Domain + Persistence)

**Purpose**: Domain entity, enums, Flyway migration, JPA entities, ports, persistence adapter — MUST complete before any user story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T002 [P] Create domain enums `NotificationType` (TASK_ASSIGNED, STATUS_CHANGED, TASK_OVERDUE) and `NotificationTargetType` (ORDER, PRODUCTION_TASK) in `src/main/kotlin/com/ctfind/productioncontrol/notifications/domain/NotificationType.kt` and `src/main/kotlin/com/ctfind/productioncontrol/notifications/domain/NotificationTargetType.kt`
- [X] T003 [P] Create domain entity `Notification` data class with validation (title 1–200 chars, body max 1000, targetType/targetId both-null-or-both-non-null) in `src/main/kotlin/com/ctfind/productioncontrol/notifications/domain/Notification.kt`
- [X] T004 [P] Create Flyway migration `V6__create_notification_table.sql` with table, FK to app_user, composite index (recipient_user_id, read, created_at DESC), partial index for unread count in `src/main/resources/db/migration/V6__create_notification_table.sql`
- [X] T005 Create application-layer ports: `NotificationPersistencePort` (findById, findByRecipientUserId paginated, countUnread, save, markAllRead) and `NotificationCreatePort` (create — exposed to other modules) in `src/main/kotlin/com/ctfind/productioncontrol/notifications/application/NotificationPorts.kt`
- [X] T006 [P] Create application-layer models: `NotificationListQuery` (page, size, unreadOnly), `NotificationPageResult<T>`, `CreateNotificationCommand` in `src/main/kotlin/com/ctfind/productioncontrol/notifications/application/NotificationModels.kt`
- [X] T007 Create JPA entity `NotificationEntity` mapping to `notification` table with enum-as-string columns in `src/main/kotlin/com/ctfind/productioncontrol/notifications/adapter/persistence/NotificationJpaEntities.kt`
- [X] T008 Create JPA repository `NotificationJpaRepository` with Spring Data queries: findByRecipientUserId (paginated, sorted), countByRecipientUserIdAndReadFalse, markAllRead bulk update (@Modifying @Query) in `src/main/kotlin/com/ctfind/productioncontrol/notifications/adapter/persistence/NotificationJpaRepositories.kt`
- [X] T009 Implement `NotificationPersistenceAdapter` implementing `NotificationPersistencePort` with domain↔JPA mapping in `src/main/kotlin/com/ctfind/productioncontrol/notifications/adapter/persistence/NotificationPersistenceAdapter.kt`

**Checkpoint**: Domain + persistence layer ready. Use cases can now be built.

---

## Phase 3: User Story 3 — Create Notification via Port (Priority: P1)

**Goal**: Other modules (production, orders) can create notifications for users via `NotificationCreatePort`

**Independent Test**: Call CreateNotificationUseCase programmatically with valid and invalid data, verify notification is persisted with read=false and createdAt set

### Implementation for User Story 3

- [X] T010 [US3] Implement `CreateNotificationUseCase` (implements `NotificationCreatePort`): validate domain rules, set createdAt=now(), read=false, readAt=null, delegate to persistence port in `src/main/kotlin/com/ctfind/productioncontrol/notifications/application/CreateNotificationUseCase.kt`
- [X] T011 [P] [US3] Write domain validation tests for `Notification` (blank title, title > 200, body > 1000, targetType without targetId, happy path) in `src/test/kotlin/com/ctfind/productioncontrol/notifications/domain/NotificationTests.kt`
- [X] T012 [US3] Write unit tests for `CreateNotificationUseCase` (successful creation, validation failures, nullable body/target) with mocked persistence port in `src/test/kotlin/com/ctfind/productioncontrol/notifications/application/CreateNotificationUseCaseTests.kt`

**Checkpoint**: Internal creation port works and is tested. Other modules can call `NotificationCreatePort.create(...)`.

---

## Phase 4: User Story 1 — List Notifications + Unread Count (Priority: P1)

**Goal**: Authenticated user lists their own notifications (paginated, sorted, filterable by unread) and gets unread count

**Independent Test**: Authenticate as any user, call GET /api/notifications and GET /api/notifications/unread-count, verify data isolation and pagination

### Implementation for User Story 1

- [X] T013 [US1] Implement `ListNotificationsUseCase`: list (paginated, recipient=currentUser, optional unreadOnly filter) and countUnread in `src/main/kotlin/com/ctfind/productioncontrol/notifications/application/ListNotificationsUseCase.kt`
- [X] T014 [P] [US1] Create response DTOs (`NotificationResponse`, `NotificationPageResponse`, `UnreadCountResponse`) and JWT→userId extraction helper in `src/main/kotlin/com/ctfind/productioncontrol/notifications/adapter/web/NotificationDtos.kt`
- [X] T015 [US1] Implement `NotificationController` with `GET /api/notifications` (list, paginated, size clamped 1–100) and `GET /api/notifications/unread-count` in `src/main/kotlin/com/ctfind/productioncontrol/notifications/adapter/web/NotificationController.kt`
- [X] T016 [US1] Write unit tests for `ListNotificationsUseCase` (user isolation, pagination, unreadOnly filter) with mocked persistence port in `src/test/kotlin/com/ctfind/productioncontrol/notifications/application/ListNotificationsUseCaseTests.kt`
- [X] T017 [US1] Write MockMvc controller tests for `GET /api/notifications`: authenticated list, empty list, pagination, unreadOnly, size clamping; and `GET /api/notifications/unread-count`: correct count, zero count in `src/test/kotlin/com/ctfind/productioncontrol/notifications/adapter/web/NotificationControllerListTests.kt`
- [X] T018 [US1] Write persistence adapter integration test (@DataJpaTest): save notifications, query by recipient, verify isolation between users, verify sorting, verify unread count in `src/test/kotlin/com/ctfind/productioncontrol/notifications/adapter/persistence/NotificationPersistenceAdapterTests.kt`

**Checkpoint**: Users can list their notifications and see unread badge count via REST API.

---

## Phase 5: User Story 2 — Manage Read State (Priority: P2)

**Goal**: User marks one notification as read (idempotent, readAt preserved) or marks all unread as read (returns count)

**Independent Test**: Create notifications, mark one read (verify readAt), mark again (readAt unchanged), mark-all-read (verify count and readAt set)

### Implementation for User Story 2

- [X] T019 [US2] Implement `MarkNotificationReadUseCase`: markRead(notificationId, currentUserId) — set read=true, readAt only on first read; markAllRead(currentUserId) — bulk update, return affected count in `src/main/kotlin/com/ctfind/productioncontrol/notifications/application/MarkNotificationReadUseCase.kt`
- [X] T020 [US2] Add `PATCH /api/notifications/{id}/read` and `POST /api/notifications/mark-all-read` endpoints to `NotificationController` in `src/main/kotlin/com/ctfind/productioncontrol/notifications/adapter/web/NotificationController.kt`
- [X] T021 [P] [US2] Add `MarkReadResponse` and `MarkAllReadResponse` DTOs to `src/main/kotlin/com/ctfind/productioncontrol/notifications/adapter/web/NotificationDtos.kt`
- [X] T022 [US2] Write unit tests for `MarkNotificationReadUseCase` (first read sets readAt, second read preserves readAt, foreign notification returns not-found, mark-all returns count) in `src/test/kotlin/com/ctfind/productioncontrol/notifications/application/MarkNotificationReadUseCaseTests.kt`
- [X] T023 [US2] Write MockMvc controller tests for `PATCH .../read` (success, idempotent, 404 foreign, 404 missing) and `POST .../mark-all-read` (count returned, zero count) in `src/test/kotlin/com/ctfind/productioncontrol/notifications/adapter/web/NotificationControllerReadTests.kt`

**Checkpoint**: Full CRUD-minus-delete for notifications is functional and tested.

---

## Phase 6: Polish & Verification

**Purpose**: Security, cross-cutting concerns, build verification

- [X] T024 Write security-focused MockMvc tests: 401 without JWT for all 4 endpoints, data isolation (user A cannot see/mark user B's notifications → 404) in `src/test/kotlin/com/ctfind/productioncontrol/notifications/adapter/web/NotificationControllerSecurityTests.kt`
- [X] T025 Verify `make backend-test` passes (all existing + new tests green)
- [X] T026 Verify `make backend-build` succeeds (clean compile, no warnings)
- [X] T027 Verify `make docker-up-detached && make health` returns UP with V6 migration applied
- [ ] T028 Run quickstart.md validation steps against running Docker stack — pending explicit 008 quickstart evidence; implementation files, tests, and Docker health evidence exist, but this artifact does not record a full notification quickstart pass.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 — BLOCKS all user stories
- **US3 Create (Phase 3)**: Depends on Phase 2 — internal port, no controller
- **US1 List (Phase 4)**: Depends on Phase 2 — can run in parallel with Phase 3 but tests benefit from having create port
- **US2 Mark Read (Phase 5)**: Depends on Phase 2 — can run in parallel with Phase 3/4
- **Polish (Phase 6)**: Depends on all user stories complete

### User Story Dependencies

- **US3 (P1 — Create)**: Depends only on Foundational. No REST endpoints.
- **US1 (P1 — List)**: Depends on Foundational. Independent of US3 at code level, but test data creation uses the create port.
- **US2 (P2 — Mark Read)**: Depends on Foundational. Adds endpoints to the same controller as US1.

### Within Each User Story

- Domain/application code before adapters
- Use cases before controllers
- Implementation before tests (tests verify the implementation)
- Core implementation before integration

### Parallel Opportunities

- T002, T003, T004 can run in parallel (domain enums, entity, migration — different files)
- T005, T006 can run in parallel (ports and models — different files)
- T011, T012 tests can run in parallel (domain tests vs use case tests — different files)
- T014 (DTOs) can run in parallel with T013 (use case)
- T021 (DTOs) can run in parallel with T019 (use case)

---

## Parallel Example: Foundational Phase

```bash
# These 3 can run in parallel (different files):
Task T002: "Create NotificationType + NotificationTargetType enums"
Task T003: "Create Notification domain entity"
Task T004: "Create Flyway V6 migration"
```

---

## Implementation Strategy

### MVP First (US3 + US1)

1. Complete Phase 1: Setup (1 task)
2. Complete Phase 2: Foundational — domain, migration, JPA, ports (8 tasks)
3. Complete Phase 3: US3 — Create port (3 tasks)
4. Complete Phase 4: US1 — List + unread count (6 tasks)
5. **STOP and VALIDATE**: Test list and create independently via `make backend-test`

### Full Delivery

1. Complete Phase 5: US2 — Mark read (5 tasks)
2. Complete Phase 6: Polish + verification (5 tasks)
3. Total: 28 tasks

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- Domain validation in Notification.kt init block (matches existing ProductionTask pattern)
- NotificationType stored as VARCHAR (EnumType.STRING) per research R1
- JWT userId extraction follows existing pattern from ProductionTaskDtos.kt (research R2)
- mark-all-read via @Modifying @Query per research R4
- No SecurityConfig changes needed — existing `.anyRequest().authenticated()` covers all endpoints (research R5)
- readAt idempotency per research R6: set only when read transitions from false to true
