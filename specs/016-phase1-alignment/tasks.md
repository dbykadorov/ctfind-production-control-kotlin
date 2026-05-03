# Tasks: Phase 1 Alignment

**Input**: Design documents from `/specs/016-phase1-alignment/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/, quickstart.md

**Tests**: Runtime tests are not required for this documentation-only feature unless an open verification item is closed with fresh runtime evidence. Validation is performed through repository searches and documentation/sign-off review from `quickstart.md`.

**Organization**: Tasks are grouped by user story to enable independent implementation and review.

**Constitution**: Tasks preserve ERP/domain traceability, future TOC analysis facts, domain-centered architecture boundaries, API-only backend behavior, and Docker-first verification evidence expectations. This feature must not change runtime behavior or permission semantics.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Inventory)

**Purpose**: Capture current Phase 1 alignment state before editing existing artifacts.

- [X] T001 Create an alignment inventory section in `specs/016-phase1-alignment/quickstart.md` listing baseline commands for role vocabulary, completion criteria, open verification items, spec statuses, and 008 task format.
- [X] T002 [P] Record the current Phase 1 completion-criteria gap in `specs/016-phase1-alignment/quickstart.md` using `docs/PHASE_01.md` as source.
- [X] T003 [P] Record the current role-vocabulary mismatch findings in `specs/016-phase1-alignment/quickstart.md` using `specs/011-warehouse-materials/spec.md`, `specs/012-order-bom-consumption/spec.md`, and `docs/PHASE_01_MANUAL.md` as sources.
- [X] T004 [P] Record current open verification items from `specs/007-audit-log-viewer/tasks.md` and `specs/015-pam-dark-theme-sync/tasks.md` in `specs/016-phase1-alignment/quickstart.md`.
- [X] T005 [P] Record current `Status` values from `specs/*/spec.md` and current non-checkbox task lines from `specs/008-notifications-infrastructure/tasks.md` in `specs/016-phase1-alignment/quickstart.md`.

---

## Phase 2: Foundational (Status and Disposition Rules)

**Purpose**: Define shared wording that all user stories will use.

**CRITICAL**: No user story cleanup should begin until these rules are recorded.

- [X] T006 Add a "Status Vocabulary" section to `specs/016-phase1-alignment/quickstart.md` defining `Accepted`, `Pending verification`, `Deferred`, `Blocked`, and `Superseded` for Phase 1 specs.
- [X] T007 Add a "Verification Disposition Record" template to `specs/016-phase1-alignment/quickstart.md` matching `specs/016-phase1-alignment/contracts/verification-disposition.contract.md`.
- [X] T008 Add a "Role Vocabulary Mapping" note to `docs/PHASE_01_MANUAL.md` that maps human role labels to canonical backend role codes from `specs/016-phase1-alignment/contracts/role-vocabulary.contract.md`.
- [X] T009 Update `specs/016-phase1-alignment/plan.md` if the implementation chooses a status/disposition wording that differs from `specs/016-phase1-alignment/research.md`.

**Checkpoint**: Shared status, verification, and role vocabulary rules are explicit and reusable.

---

## Phase 3: User Story 1 - Align Phase 1 Acceptance Scope (Priority: P1) MVP

**Goal**: Make Phase 1 completion criteria match the delivered Phase 1 scope.

**Independent Test**: Review `docs/PHASE_01.md` completion criteria and confirm every delivered Phase 1 business capability has an explicit acceptance outcome or explicit deferral.

### Implementation for User Story 1

- [X] T010 [US1] Update `docs/PHASE_01.md` completion criteria to cover administrator bootstrap, users and roles, orders, production tasks, task board/executor workflow, overdue/status visibility, audit log, internal notifications, warehouse materials, and stock consumption under orders.
- [X] T011 [US1] Add explicit deferral wording to `docs/PHASE_01.md` for any Phase 1 capability that is not accepted at closeout, including owner, reason, and sign-off impact.
- [X] T012 [US1] Cross-check `docs/PHASE_01.md` against `specs/016-phase1-alignment/contracts/phase1-acceptance-baseline.contract.md` and record the result in `specs/016-phase1-alignment/quickstart.md`.
- [X] T013 [US1] Update `specs/016-phase1-alignment/quickstart.md` with the final command/output summary for the Phase 1 completion criteria validation.

**Checkpoint**: Phase 1 acceptance scope is reviewable from `docs/PHASE_01.md`.

---

## Phase 4: User Story 2 - Normalize Role Vocabulary (Priority: P1)

**Goal**: Ensure Phase 1 requirements and contracts use the canonical role codes for access behavior.

**Independent Test**: Search Phase 1 docs/specs for role references and confirm access behavior uses `ADMIN`, `ORDER_MANAGER`, `WAREHOUSE`, `PRODUCTION_SUPERVISOR`, or `PRODUCTION_EXECUTOR`.

### Implementation for User Story 2

- [X] T014 [US2] Replace current-access references to `WAREHOUSE_MANAGER` with `WAREHOUSE` in `specs/011-warehouse-materials/spec.md`.
- [X] T015 [P] [US2] Replace or qualify current-access references to `WAREHOUSE_MANAGER` in `specs/011-warehouse-materials/checklists/requirements.md`.
- [X] T016 [P] [US2] Verify `WAREHOUSE` role wording remains consistent in `specs/011-warehouse-materials/contracts/materials-api.md`; update only if it conflicts with `specs/016-phase1-alignment/contracts/role-vocabulary.contract.md`.
- [X] T017 [US2] Replace current-access references to `WAREHOUSE_MANAGER` with `WAREHOUSE` in `specs/012-order-bom-consumption/spec.md`.
- [X] T018 [P] [US2] Verify role wording remains consistent in `specs/012-order-bom-consumption/plan.md` and `specs/012-order-bom-consumption/contracts/rest-consumption.md`; update only if it conflicts with canonical role codes.
- [X] T019 [P] [US2] Qualify human-facing labels in `docs/PHASE_01.md` where role responsibilities imply access behavior, using mappings from `docs/PHASE_01_MANUAL.md`.
- [X] T020 [US2] Run the role vocabulary validation from `specs/016-phase1-alignment/quickstart.md` and record the final result in `specs/016-phase1-alignment/quickstart.md`.

**Checkpoint**: Current access behavior in Phase 1 artifacts uses canonical role codes or clearly mapped human labels.

---

## Phase 5: User Story 3 - Resolve Open Verification Evidence (Priority: P1)

**Goal**: Ensure every known open verification item is either closed with evidence or explicitly deferred/blocked with owner, reason, and sign-off impact.

**Independent Test**: Review `specs/007-audit-log-viewer/tasks.md`, `specs/007-audit-log-viewer/quickstart.md`, `specs/015-pam-dark-theme-sync/tasks.md`, and `specs/015-pam-dark-theme-sync/contracts/qa-signoff-routes.contract.md`; no open verification item remains without disposition.

### Implementation for User Story 3

- [X] T021 [US3] Decide the disposition for `specs/007-audit-log-viewer/tasks.md` T028 backend Docker test: passed with evidence, blocked, or deferred with owner/reason/sign-off impact.
- [X] T022 [US3] Decide the disposition for `specs/007-audit-log-viewer/tasks.md` T030 Docker startup and health check: passed with evidence, blocked, or deferred with owner/reason/sign-off impact.
- [X] T023 [US3] Decide the disposition for `specs/007-audit-log-viewer/tasks.md` T031 admin smoke, T032 non-admin smoke, and T033 tablet smoke in `specs/007-audit-log-viewer/tasks.md`.
- [X] T024 [US3] Update `specs/007-audit-log-viewer/quickstart.md` verification record so backend tests, Docker startup, API smoke, admin smoke, non-admin smoke, and tablet smoke have current disposition, owner when needed, sign-off impact, and date.
- [X] T025 [US3] Decide the disposition for `specs/015-pam-dark-theme-sync/tasks.md` T014 dark-theme manual sign-off.
- [X] T026 [US3] Update `specs/015-pam-dark-theme-sync/contracts/qa-signoff-routes.contract.md` Sign-off table so Product/Design and QA rows reflect current disposition, owner when needed, sign-off impact, and date.
- [X] T027 [US3] Update checkbox states in `specs/007-audit-log-viewer/tasks.md` and `specs/015-pam-dark-theme-sync/tasks.md` only for verification items that have a valid passed/deferred/blocked disposition.
- [X] T028 [US3] Run the verification-disposition validation from `specs/016-phase1-alignment/quickstart.md` and record the final result in `specs/016-phase1-alignment/quickstart.md`.

**Checkpoint**: 007 and 015 no longer hide open verification state.

---

## Phase 6: User Story 4 - Finalize Spec Status and Task Format (Priority: P2)

**Goal**: Make Phase 1 spec statuses and task tracking readable for next-phase planning.

**Independent Test**: Review all `specs/*/spec.md` status lines and `specs/008-notifications-infrastructure/tasks.md`; implemented specs do not remain generic `Draft`, incomplete specs expose their state, and 008 actionable tasks use checkbox states.

### Implementation for User Story 4

- [X] T029 [US4] Update `Status` values in `specs/001-local-docker-startup/spec.md`, `specs/002-migrate-cabinet-frontend/spec.md`, `specs/003-login-api-auth/spec.md`, `specs/004-orders-api-wiring/spec.md`, and `specs/005-production-tasks/spec.md` according to the status vocabulary in `specs/016-phase1-alignment/quickstart.md`.
- [X] T030 [US4] Update `Status` values in `specs/006-production-tasks-board-m4/spec.md`, `specs/007-audit-log-viewer/spec.md`, `specs/008-notifications-infrastructure/spec.md`, `specs/009-notification-triggers/spec.md`, and `specs/010-notification-frontend/spec.md` according to verification state.
- [X] T031 [US4] Update `Status` values in `specs/011-warehouse-materials/spec.md`, `specs/012-order-bom-consumption/spec.md`, `specs/013-user-admin-management/spec.md`, `specs/014-edit-users/spec.md`, `specs/015-pam-dark-theme-sync/spec.md`, and `specs/016-phase1-alignment/spec.md` according to verification state.
- [X] T032 [US4] Convert actionable tasks T001-T028 in `specs/008-notifications-infrastructure/tasks.md` to strict checkbox format without changing task wording beyond the checkbox prefix.
- [X] T033 [US4] Set checkbox states in `specs/008-notifications-infrastructure/tasks.md` only where existing evidence supports completion; leave uncertain tasks open and note the reason in `specs/016-phase1-alignment/quickstart.md`.
- [X] T034 [US4] Run the spec-status validation from `specs/016-phase1-alignment/quickstart.md` and record the final result in `specs/016-phase1-alignment/quickstart.md`.
- [X] T035 [US4] Run the 008 task-format validation from `specs/016-phase1-alignment/quickstart.md` and record the final result in `specs/016-phase1-alignment/quickstart.md`.

**Checkpoint**: Phase 1 specs and notification tasks are trackable at a glance.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and consistency review across all stories.

- [X] T036 [P] Review `docs/PHASE_01_MANUAL.md` against `docs/PHASE_01.md` and update wording only where role mapping or Phase 1 acceptance wording would otherwise conflict.
- [X] T037 [P] Review `specs/016-phase1-alignment/contracts/role-vocabulary.contract.md`, `specs/016-phase1-alignment/contracts/phase1-acceptance-baseline.contract.md`, and `specs/016-phase1-alignment/contracts/verification-disposition.contract.md` against final edits; update contracts if implementation decisions changed.
- [X] T038 Run all commands in `specs/016-phase1-alignment/quickstart.md` and record final PASS/DEFERRED/BLOCKED outcomes in `specs/016-phase1-alignment/quickstart.md`.
- [X] T039 Update `specs/016-phase1-alignment/tasks.md` checkbox states for completed alignment tasks after verification evidence has been recorded.
- [X] T040 Perform final repository hygiene check with `git status --short --branch` and record unrelated untracked items, including `micro` if still present, in the implementation summary.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - blocks all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational
- **User Story 2 (Phase 4)**: Depends on Foundational; can run in parallel with US1 after role mapping rules are set
- **User Story 3 (Phase 5)**: Depends on Foundational; can run in parallel with US1/US2 if verification owners are available
- **User Story 4 (Phase 6)**: Depends on US3 for accurate statuses of 007 and 015; can start partial status review earlier
- **Polish (Phase 7)**: Depends on desired user stories being complete

### User Story Dependencies

- **US1 Align Phase 1 Acceptance Scope (P1)**: No dependency on other user stories after Foundational
- **US2 Normalize Role Vocabulary (P1)**: No dependency on other user stories after Foundational
- **US3 Resolve Open Verification Evidence (P1)**: No dependency on US1/US2 after Foundational
- **US4 Finalize Spec Status and Task Format (P2)**: Depends on US3 for final statuses where verification state matters

### Within Each User Story

- Inventory before edits
- Contract/rule wording before artifact updates
- Artifact updates before quickstart validation
- Validation results recorded before marking tasks complete

### Parallel Opportunities

- T002-T005 can run in parallel after T001.
- T015, T016, T018, and T019 can run in parallel after T014/T017 owners agree on canonical wording.
- T021-T023 and T025 can be investigated in parallel by different reviewers.
- T029-T031 can be split by spec ranges once status vocabulary and verification disposition are known.
- T036 and T037 can run in parallel during final polish.

---

## Parallel Example: User Story 2

```text
Task: "Replace or qualify current-access references to WAREHOUSE_MANAGER in specs/011-warehouse-materials/checklists/requirements.md"
Task: "Verify WAREHOUSE role wording remains consistent in specs/011-warehouse-materials/contracts/materials-api.md"
Task: "Verify role wording remains consistent in specs/012-order-bom-consumption/plan.md and specs/012-order-bom-consumption/contracts/rest-consumption.md"
Task: "Qualify human-facing labels in docs/PHASE_01.md where role responsibilities imply access behavior"
```

---

## Parallel Example: User Story 3

```text
Task: "Decide disposition for specs/007-audit-log-viewer/tasks.md T028 backend Docker test"
Task: "Decide disposition for specs/007-audit-log-viewer/tasks.md T030 Docker startup and health check"
Task: "Decide disposition for specs/007-audit-log-viewer/tasks.md T031-T033 manual smoke checks"
Task: "Decide disposition for specs/015-pam-dark-theme-sync/tasks.md T014 dark-theme manual sign-off"
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1: Setup inventory.
2. Complete Phase 2: Foundational status/disposition/role rules.
3. Complete Phase 3: US1 completion criteria alignment.
4. Stop and validate `docs/PHASE_01.md` against `phase1-acceptance-baseline.contract.md`.

### Incremental Delivery

1. Complete Setup + Foundational.
2. Complete US1 to align acceptance scope.
3. Complete US2 to eliminate role-vocabulary ambiguity.
4. Complete US3 to make verification state explicit.
5. Complete US4 to finalize spec statuses and 008 task tracking.
6. Complete Polish quickstart validation.

### Parallel Team Strategy

With multiple reviewers:

1. One reviewer owns `docs/PHASE_01.md` completion criteria.
2. One reviewer owns role vocabulary across 011/012/manual docs.
3. One reviewer owns verification dispositions for 007/015.
4. One reviewer owns spec statuses and 008 checkbox conversion after verification state is known.

---

## Notes

- [P] tasks = different files, no dependency on incomplete tasks
- [US1] aligns Phase 1 acceptance criteria
- [US2] normalizes role vocabulary
- [US3] resolves open verification evidence
- [US4] finalizes spec statuses and task checkbox format
- Runtime commands are not required unless a verification item is closed with fresh runtime evidence
- This feature must not change authorization behavior, source code, database schema, or Docker runtime configuration
