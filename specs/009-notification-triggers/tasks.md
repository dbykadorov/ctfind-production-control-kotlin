# Tasks: Триггеры генерации уведомлений

**Input**: Design documents from `specs/009-notification-triggers/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Included — trigger logic has conditional behavior (self-suppression, deduplication, fire-and-forget) that must be verified through unit tests.

**Organization**: Tasks grouped by user story. US1 (TASK_ASSIGNED) and US2 (STATUS_CHANGED) are both P1 and independent of each other. US3 (TASK_OVERDUE) depends on foundational port methods.

**Constitution**: Triggers live in application layer, cross-module via `NotificationCreatePort` interface. No new API endpoints. Fire-and-forget error handling. Domain boundaries preserved.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Exact file paths relative to repository root

## Phase 1: Setup

**Purpose**: Enable @Scheduled support and verify existing infrastructure

- [X] T001 Verify @EnableScheduling is present in Spring configuration, add if missing in `src/main/kotlin/com/ctfind/productioncontrol/infrastructure/` config class

---

## Phase 2: Foundational (Port Extensions)

**Purpose**: Add new port methods required by all trigger stories — MUST complete before user stories

**⚠️ CRITICAL**: US3 depends on both new port methods. US1/US2 depend on NotificationCreatePort (already exists).

- [X] T002 Add `existsByTypeAndTargetIdAndRecipient(type: NotificationType, targetId: String, recipientUserId: UUID): Boolean` method to `NotificationPersistencePort` in `src/main/kotlin/com/ctfind/productioncontrol/notifications/application/NotificationPorts.kt`
- [X] T003 Implement `existsByTypeAndTargetIdAndRecipient()` in `NotificationPersistenceAdapter` in `src/main/kotlin/com/ctfind/productioncontrol/notifications/adapter/persistence/NotificationPersistenceAdapter.kt` (add JPA repository query method)
- [X] T004 Add `findOverdue(today: LocalDate): List<ProductionTask>` method to `ProductionTaskPort` in `src/main/kotlin/com/ctfind/productioncontrol/production/application/ProductionTaskPorts.kt`
- [X] T005 Implement `findOverdue()` in production persistence adapter in `src/main/kotlin/com/ctfind/productioncontrol/production/adapter/persistence/ProductionTaskPersistenceAdapter.kt` (JPA query: plannedFinishDate < today AND status != COMPLETED AND plannedFinishDate IS NOT NULL)

**Checkpoint**: Port extensions ready — user story implementation can begin

---

## Phase 3: User Story 1 — TASK_ASSIGNED trigger (Priority: P1) 🎯 MVP

**Goal**: When executor is assigned to a production task, the new executor receives a TASK_ASSIGNED notification

**Independent Test**: Assign executor to task → check that notification with type=TASK_ASSIGNED appears for executor

### Tests for User Story 1

- [X] T006 [P] [US1] Create unit tests for TASK_ASSIGNED trigger in `src/test/kotlin/com/ctfind/productioncontrol/production/application/AssignProductionTaskNotificationTests.kt`: (1) notification created when executor changes, (2) notification NOT created when executor unchanged, (3) notification NOT created on planning-only change, (4) notification has correct type/title/targetType/targetId, (5) exception in notification does not fail use-case

### Implementation for User Story 1

- [X] T007 [US1] Inject `NotificationCreatePort` into `AssignProductionTaskUseCase` in `src/main/kotlin/com/ctfind/productioncontrol/production/application/AssignProductionTaskUseCase.kt`, add TASK_ASSIGNED notification creation in `executorChanged` branch after audit.record(), wrap in try-catch with SLF4J warning on failure

**Checkpoint**: TASK_ASSIGNED trigger functional. Verify: assign executor → notification appears in executor's feed.

---

## Phase 4: User Story 2 — STATUS_CHANGED trigger (Priority: P1)

**Goal**: When task status changes, the task creator receives a STATUS_CHANGED notification (unless the creator is the one changing status)

**Independent Test**: Change task status from executor → check creator gets STATUS_CHANGED notification; change status by creator → no notification

### Tests for User Story 2

- [X] T008 [P] [US2] Create unit tests for STATUS_CHANGED trigger in `src/test/kotlin/com/ctfind/productioncontrol/production/application/ChangeProductionTaskStatusNotificationTests.kt`: (1) notification sent to creator on status change, (2) self-notification suppressed when actorUserId == createdByUserId, (3) notification has correct title with taskNumber and new status, (4) all status transitions generate notification (IN_PROGRESS, BLOCKED, COMPLETED), (5) exception in notification does not fail use-case

### Implementation for User Story 2

- [X] T009 [US2] Inject `NotificationCreatePort` into `ChangeProductionTaskStatusUseCase` in `src/main/kotlin/com/ctfind/productioncontrol/production/application/ChangeProductionTaskStatusUseCase.kt`, add STATUS_CHANGED notification after audit.record() with self-suppression check (cmd.actorUserId != saved.createdByUserId), wrap in try-catch

**Checkpoint**: STATUS_CHANGED trigger functional. Verify: change status → creator gets notification; self-change → no notification.

---

## Phase 5: User Story 3 — TASK_OVERDUE trigger (Priority: P2)

**Goal**: Scheduled job detects overdue tasks and sends TASK_OVERDUE notifications to executor and creator, with deduplication

**Independent Test**: Create task with past plannedFinishDate → run job → verify TASK_OVERDUE notifications created; run again → verify no duplicates

### Tests for User Story 3

- [X] T010 [P] [US3] Create unit tests for OverdueTaskNotificationJob in `src/test/kotlin/com/ctfind/productioncontrol/production/application/OverdueTaskNotificationJobTests.kt`: (1) overdue task sends notification to executor and creator, (2) completed task is skipped, (3) task without plannedFinishDate is skipped, (4) duplicate notification prevented (existsBy returns true → skip), (5) task without executor sends only to creator, (6) exception in one notification does not stop processing others, (7) executor == creator sends only one notification

### Implementation for User Story 3

- [X] T011 [US3] Create `OverdueTaskNotificationJob` in `src/main/kotlin/com/ctfind/productioncontrol/production/application/OverdueTaskNotificationJob.kt` with @Component, @Scheduled(fixedRate = 15 min), inject ProductionTaskPort + NotificationCreatePort + NotificationPersistencePort, implement checkOverdueTasks() with dedup via existsByTypeAndTargetIdAndRecipient, fire-and-forget try-catch per task

**Checkpoint**: TASK_OVERDUE trigger functional. Verify: overdue tasks get notifications; no duplicates on re-run.

---

## Phase 6: Polish & Verification

**Purpose**: Cross-cutting verification

- [X] T012 Run `make backend-test-docker` — all tests pass (existing + new)
- [X] T013 Run `make docker-up-detached && make health` — stack healthy with all changes
- [X] T014 Run quickstart.md validation steps: login, assign task, verify TASK_ASSIGNED, change status, verify STATUS_CHANGED

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — T001 standalone
- **Foundational (Phase 2)**: T002-T005 can be done in parallel (different files), BLOCKS US3
- **US1 (Phase 3)**: Depends on Phase 1 only (NotificationCreatePort already exists)
- **US2 (Phase 4)**: Depends on Phase 1 only, independent of US1
- **US3 (Phase 5)**: Depends on Phase 2 (needs findOverdue + existsBy... port methods)
- **Polish (Phase 6)**: Depends on all user stories complete

### User Story Dependencies

- **US1 (TASK_ASSIGNED)**: Independent — can start after Phase 1
- **US2 (STATUS_CHANGED)**: Independent — can start after Phase 1, parallel with US1
- **US3 (TASK_OVERDUE)**: Depends on Phase 2 (port extensions) — start after T002-T005

### Within Each User Story

- Tests written first (TDD)
- Implementation follows tests
- Fire-and-forget error handling in all triggers

### Parallel Opportunities

- T002 + T004 can run in parallel (different port files)
- T003 + T005 can run in parallel (different adapter files)
- T006 + T008 can run in parallel (different test files, independent stories)
- US1 (T006-T007) and US2 (T008-T009) can run fully in parallel

---

## Parallel Example: Foundational Phase

```bash
# Port method declarations (parallel — different files):
Task T002: "Add existsBy... to NotificationPersistencePort"
Task T004: "Add findOverdue to ProductionTaskPort"

# Port implementations (parallel — different adapter files):
Task T003: "Implement existsBy... in NotificationPersistenceAdapter"
Task T005: "Implement findOverdue in ProductionTaskPersistenceAdapter"
```

## Parallel Example: US1 + US2

```bash
# Tests (parallel — different files):
Task T006: "AssignProductionTaskNotificationTests"
Task T008: "ChangeProductionTaskStatusNotificationTests"

# Implementation (parallel — different use case files):
Task T007: "TASK_ASSIGNED trigger in AssignProductionTaskUseCase"
Task T009: "STATUS_CHANGED trigger in ChangeProductionTaskStatusUseCase"
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Phase 1: Setup (@EnableScheduling)
2. Phase 3: US1 — TASK_ASSIGNED trigger
3. **STOP and VALIDATE**: Assign executor → notification in feed

### Incremental Delivery

1. Setup → Phase 1 done
2. US1 (TASK_ASSIGNED) → test → verify ✅
3. US2 (STATUS_CHANGED) → test → verify ✅ (parallel with US1)
4. Phase 2 (port extensions) → US3 (TASK_OVERDUE) → test → verify ✅
5. Polish → full verification

---

## Notes

- No new Flyway migrations — all changes are application-level code
- NotificationType values (TASK_ASSIGNED, STATUS_CHANGED, TASK_OVERDUE) already exist in enum from spec 008
- targetId in notifications = taskNumber (human-readable), not UUID
- Fire-and-forget: try-catch in all trigger points, SLF4J logger.warn on failure
- Self-notification suppression only for STATUS_CHANGED (not TASK_ASSIGNED)
