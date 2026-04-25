# Tasks: Local Container Startup

**Input**: Design documents from `specs/001-local-docker-startup/`  
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/local-runtime.md`, `quickstart.md`

**Tests**: Automated test tasks are not included because the feature specification did not request TDD or explicit automated tests. Verification is performed through the local runtime contract and quickstart commands.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Each task includes an exact file path

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare repository-level files needed by all local runtime work.

- [X] T001 Create Docker build ignore rules for Gradle outputs, VCS metadata, IDE files, and local runtime artifacts in `.dockerignore`
- [X] T002 Add local development Spring configuration placeholders for datasource, profile, actuator health, and safe defaults in `src/main/resources/application-local.properties`
- [X] T003 [P] Document local runtime scope and prerequisite summary in `README.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Define the shared container runtime objects that all user stories depend on.

**CRITICAL**: No user story work can begin until this phase is complete.

- [X] T004 Create a multi-stage JVM application image build that packages the Spring Boot app via Gradle wrapper in `Dockerfile`
- [X] T005 Define base Compose project name, shared network, and named PostgreSQL volume in `docker-compose.yml`
- [X] T006 Define the `postgres` service with local-only database name, username, password, port mapping, volume, and healthcheck in `docker-compose.yml`
- [X] T007 Define the `app` service build context, environment variables, dependency on `postgres`, and HTTP port mapping in `docker-compose.yml`
- [X] T008 Configure the application service to use the local Spring profile and local PostgreSQL datasource values in `docker-compose.yml`
- [X] T009 Ensure local database state and host override files remain untracked by updating `.gitignore`

**Checkpoint**: Shared local runtime foundation exists and user story implementation can proceed.

---

## Phase 3: User Story 1 - Запуск проекта одной командой (Priority: P1) MVP

**Goal**: A developer can start the local environment from the repository root and reach a healthy backend application.

**Independent Test**: From the repository root, run `docker compose up --build`, then verify `curl http://localhost:8080/actuator/health` returns an `UP` response.

### Implementation for User Story 1

- [X] T010 [US1] Expose a host-reachable application port and service name consistent with the local runtime contract in `docker-compose.yml`
- [X] T011 [US1] Make the application wait for the local database readiness through Compose dependency health conditions in `docker-compose.yml`
- [X] T012 [P] [US1] Allow unauthenticated local readiness checks for `/actuator/health` while keeping other routes protected in `src/main/kotlin/com/ctfind/productioncontrol/config/SecurityConfig.kt`
- [X] T013 [US1] Configure Actuator health exposure and local readiness details for the local profile in `src/main/resources/application-local.properties`
- [X] T014 [US1] Add the one-command startup instructions and expected application URL to `README.md`
- [X] T015 [US1] Add the readiness check command and expected healthy response to `README.md`
- [X] T016 [US1] Verify the US1 happy path against `specs/001-local-docker-startup/contracts/local-runtime.md`

**Checkpoint**: User Story 1 is complete when the backend and PostgreSQL start with one command and the health endpoint reports `UP`.

---

## Phase 4: User Story 2 - Предсказуемая остановка и повторный запуск (Priority: P2)

**Goal**: A developer can stop and restart the local environment without manual cleanup.

**Independent Test**: Run `docker compose down`, then `docker compose up --build --wait`, and verify the application returns to healthy state without deleting containers or volumes manually.

### Implementation for User Story 2

- [X] T017 [US2] Confirm the Compose volume preserves PostgreSQL state across ordinary `docker compose down` and restart cycles in `docker-compose.yml`
- [X] T018 [US2] Add stop and detached restart commands to `README.md`
- [X] T019 [US2] Add explicit local data reset instructions using `docker compose down -v` to `README.md`
- [X] T020 [US2] Document the stop, restart, and reset expectations in `specs/001-local-docker-startup/quickstart.md`
- [X] T021 [US2] Verify the US2 stop-and-restart flow against `specs/001-local-docker-startup/contracts/local-runtime.md`

**Checkpoint**: User Story 2 is complete when normal stop/start preserves local state and reset is explicit.

---

## Phase 5: User Story 3 - Понятная диагностика локального запуска (Priority: P3)

**Goal**: A developer can inspect logs and identify common local startup failures.

**Independent Test**: Use documented commands to inspect `app` and `postgres` logs, check container status, and distinguish container runtime, port, database, and application readiness failures.

### Implementation for User Story 3

- [X] T022 [US3] Add application and database log commands to `README.md`
- [X] T023 [US3] Add troubleshooting steps for missing container runtime, occupied ports, database startup, and failed health checks to `README.md`
- [X] T024 [P] [US3] Add service health status inspection commands to `specs/001-local-docker-startup/quickstart.md`
- [X] T025 [US3] Ensure PostgreSQL and application service names in documentation match `docker-compose.yml`
- [X] T026 [US3] Verify the US3 diagnostic flow against `specs/001-local-docker-startup/contracts/local-runtime.md`

**Checkpoint**: User Story 3 is complete when failed local startup has documented diagnostic paths and log commands.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and consistency across implementation and documentation.

- [X] T027 [P] Align `specs/001-local-docker-startup/quickstart.md` with the implemented commands, service names, and health response
- [X] T028 [P] Align `specs/001-local-docker-startup/contracts/local-runtime.md` with the implemented commands, ports, service names, and environment variables
- [X] T029 Run `docker compose config` and record any required documentation adjustments in `README.md`
- [X] T030 Run the full local quickstart flow and update `README.md` if observed behavior differs from documented behavior
- [X] T031 Run `git status --short` and ensure generated build outputs, local database files, and logs are not tracked in `.gitignore`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies; can start immediately.
- **Foundational (Phase 2)**: Depends on Setup completion; blocks all user stories.
- **User Story 1 (Phase 3)**: Depends on Foundational; delivers MVP local startup.
- **User Story 2 (Phase 4)**: Depends on Foundational and benefits from US1 health verification.
- **User Story 3 (Phase 5)**: Depends on Foundational and can proceed after service names and commands stabilize.
- **Polish (Phase 6)**: Depends on all selected user stories being implemented.

### User Story Dependencies

- **US1 (P1)**: No dependency on other user stories after Foundational.
- **US2 (P2)**: Can be implemented after Foundational, but final verification uses the US1 readiness check.
- **US3 (P3)**: Can be implemented after Foundational, but documentation must match final service names from US1/US2.

### Within Each User Story

- Container runtime configuration before documentation that references commands.
- Local profile and security configuration before health readiness verification.
- Documentation updates before quickstart validation.
- Verification tasks after implementation tasks in the same story.

### Parallel Opportunities

- T003 can run in parallel with T001 and T002.
- T012 can run in parallel with T010/T011 because it touches a different file.
- T024 can run in parallel with T022/T023 because it updates quickstart while README diagnostics are updated.
- T027 and T028 can run in parallel during polish because they update different documentation files.

---

## Parallel Example: User Story 1

```bash
Task: "T010 [US1] Expose a host-reachable application port and service name consistent with the local runtime contract in docker-compose.yml"
Task: "T012 [P] [US1] Allow unauthenticated local readiness checks for /actuator/health while keeping other routes protected in src/main/kotlin/com/ctfind/productioncontrol/config/SecurityConfig.kt"
```

## Parallel Example: User Story 3

```bash
Task: "T022 [US3] Add application and database log commands to README.md"
Task: "T024 [P] [US3] Add service health status inspection commands to specs/001-local-docker-startup/quickstart.md"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational.
3. Complete Phase 3: User Story 1.
4. Stop and validate: run the one-command startup and health check from `README.md`.
5. Demo the backend health endpoint and local PostgreSQL startup.

### Incremental Delivery

1. Add Setup + Foundational container runtime files.
2. Add US1 one-command startup and readiness check.
3. Add US2 stop/restart/reset behavior.
4. Add US3 diagnostics and troubleshooting.
5. Finish with quickstart and contract consistency checks.

### Parallel Team Strategy

With multiple developers:

1. One developer creates the Compose and Dockerfile foundation.
2. One developer prepares local profile/security readiness configuration.
3. One developer updates README and quickstart documentation after commands stabilize.
4. All developers validate the same local runtime contract before completion.

## Summary

- Total tasks: 31
- Setup tasks: 3
- Foundational tasks: 6
- US1 tasks: 7
- US2 tasks: 5
- US3 tasks: 5
- Polish tasks: 5
- MVP scope: Phase 1 + Phase 2 + User Story 1
