# Tasks: Migrate Cabinet Frontend

**Input**: Design documents from `specs/002-migrate-cabinet-frontend/`  
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/frontend-runtime.md`, `contracts/login-placeholder.md`, `quickstart.md`

**Tests**: Automated test tasks are not generated as a separate TDD phase because the feature specification does not explicitly request TDD. Runtime verification is performed through the frontend runtime and login placeholder contracts.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Each task includes an exact file path

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare frontend workspace boundaries and source migration rules.

- [ ] T001 Create migrated frontend root directory structure in `frontend/cabinet/`
- [ ] T002 Create frontend-specific Docker ignore rules for Node, Vite, coverage, logs, and local env files in `frontend/cabinet/.dockerignore`
- [ ] T003 [P] Add frontend-generated artifacts and local env files to repository ignore rules in `.gitignore`
- [ ] T004 [P] Add frontend cabinet section and source reference notes to `README.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Move the existing cabinet source into the new repository and make it own its local build/runtime configuration.

**CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T005 Copy legacy `package.json`, `pnpm-lock.yaml`, and package metadata from `/home/dbykadorov/Work/projects/ctfind-production-control/frappe-bench/apps/ctfind_production_control/cabinet_app/` to `frontend/cabinet/`
- [ ] T006 Copy legacy TypeScript, Vite, Tailwind, PostCSS, ESLint, Vitest, and UI component config files from `/home/dbykadorov/Work/projects/ctfind-production-control/frappe-bench/apps/ctfind_production_control/cabinet_app/` to `frontend/cabinet/`
- [ ] T007 Copy legacy application source tree from `/home/dbykadorov/Work/projects/ctfind-production-control/frappe-bench/apps/ctfind_production_control/cabinet_app/src/` to `frontend/cabinet/src/`
- [ ] T008 Copy legacy tests from `/home/dbykadorov/Work/projects/ctfind-production-control/frappe-bench/apps/ctfind_production_control/cabinet_app/tests/` to `frontend/cabinet/tests/`
- [ ] T009 Copy legacy public assets and scripts from `/home/dbykadorov/Work/projects/ctfind-production-control/frappe-bench/apps/ctfind_production_control/cabinet_app/public/` and `/home/dbykadorov/Work/projects/ctfind-production-control/frappe-bench/apps/ctfind_production_control/cabinet_app/scripts/` to `frontend/cabinet/public/` and `frontend/cabinet/scripts/`
- [ ] T010 Remove any copied dependency/build artifacts such as `frontend/cabinet/node_modules/`, `frontend/cabinet/dist/`, and `frontend/cabinet/.vite/`
- [ ] T011 Adapt package name, description, and local scripts for the new platform in `frontend/cabinet/package.json`
- [ ] T012 Adapt Vite base path, output settings, host binding, and dev server port for standalone local serving in `frontend/cabinet/vite.config.ts`
- [ ] T013 Ensure frontend TypeScript aliases and include paths still resolve under `frontend/cabinet/tsconfig.json`

**Checkpoint**: Migrated frontend source exists in the new repository and can be configured without reading from the old Frappe directory.

---

## Phase 3: User Story 1 - Запуск перенесенного кабинета в Docker (Priority: P1) MVP

**Goal**: A developer can start the local Docker workflow and open the migrated cabinet login screen from the new project.

**Independent Test**: Run `docker compose up --build --wait`, then open `http://localhost:5173/cabinet/login` and verify that the migrated SPA loads without missing mandatory login assets or requiring the old Frappe runtime.

### Implementation for User Story 1

- [ ] T014 [US1] Create frontend container build/runtime definition in `frontend/cabinet/Dockerfile`
- [ ] T015 [US1] Add `frontend` service, container name, build context, host port `5173`, and network wiring in `docker-compose.yml`
- [ ] T016 [US1] Add frontend service healthcheck for `http://localhost:5173/cabinet/login` in `docker-compose.yml`
- [ ] T017 [US1] Ensure root startup command includes backend, postgres, and frontend readiness in `docker-compose.yml`
- [ ] T018 [P] [US1] Update frontend runtime URL, service name, and logs documentation in `README.md`
- [ ] T019 [P] [US1] Align frontend runtime expectations with implementation in `specs/002-migrate-cabinet-frontend/contracts/frontend-runtime.md`
- [ ] T020 [US1] Verify US1 Docker startup and login URL against `specs/002-migrate-cabinet-frontend/quickstart.md`

**Checkpoint**: User Story 1 is complete when the migrated frontend service starts with root Docker workflow and displays login.

---

## Phase 4: User Story 2 - Экран логина доступен без авторизации (Priority: P2)

**Goal**: The migrated login screen is visible and interactive, but submit never creates an authenticated session.

**Independent Test**: Open `http://localhost:5173/cabinet/login`, enter any non-empty credentials, submit, and verify that the user remains on login with an "authorization not connected yet" message.

### Implementation for User Story 2

- [ ] T021 [US2] Replace legacy Frappe login success path with deterministic unavailable-auth placeholder in `frontend/cabinet/src/api/auth-service.ts`
- [ ] T022 [US2] Ensure auth store remains unauthenticated after login submit in `frontend/cabinet/src/stores/auth.ts`
- [ ] T023 [US2] Add or adjust i18n message for authorization-not-connected state in `frontend/cabinet/src/i18n/ru.ts`
- [ ] T024 [P] [US2] Add or adjust English i18n message for authorization-not-connected state in `frontend/cabinet/src/i18n/en.ts`
- [ ] T025 [US2] Ensure login page renders the placeholder error message without redirecting to workspace in `frontend/cabinet/src/pages/auth/LoginPage.vue`
- [ ] T026 [US2] Ensure unauthenticated protected routes still redirect to login in `frontend/cabinet/src/router/index.ts`
- [ ] T027 [P] [US2] Update login placeholder behavior contract with final message key and route behavior in `specs/002-migrate-cabinet-frontend/contracts/login-placeholder.md`
- [ ] T028 [US2] Verify US2 login placeholder behavior against `specs/002-migrate-cabinet-frontend/quickstart.md`

**Checkpoint**: User Story 2 is complete when login is interactive, never authenticates, and shows the placeholder message.

---

## Phase 5: User Story 3 - Сохранение переносимого UI-контента (Priority: P3)

**Goal**: Preserve existing cabinet pages, components, styles, localization, assets, and tests for future integration slices.

**Independent Test**: Confirm that the migrated source tree contains the legacy UI content under `frontend/cabinet/`, builds or starts without Frappe-only boot failures, and documents remaining legacy integration boundaries.

### Implementation for User Story 3

- [ ] T029 [US3] Review copied `frontend/cabinet/src/components/`, `frontend/cabinet/src/pages/`, and `frontend/cabinet/src/styles/` trees for missing source files from the legacy cabinet app
- [ ] T030 [US3] Review copied `frontend/cabinet/src/assets/`, `frontend/cabinet/public/`, and font/style references for missing login assets
- [ ] T031 [US3] Isolate legacy Frappe API client startup side effects in `frontend/cabinet/src/api/frappe-client.ts`
- [ ] T032 [P] [US3] Isolate legacy Frappe socket startup side effects in `frontend/cabinet/src/api/socket.ts`
- [ ] T033 [US3] Isolate legacy boot/session assumptions so login render does not require Frappe boot payload in `frontend/cabinet/src/api/boot.ts`
- [ ] T034 [P] [US3] Update migrated frontend README or migration notes in `frontend/cabinet/README.md`
- [ ] T035 [US3] Verify preserved UI content and legacy integration boundaries against `specs/002-migrate-cabinet-frontend/data-model.md`

**Checkpoint**: User Story 3 is complete when the copied UI source is preserved and Frappe-specific integrations do not block initial login rendering.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final consistency checks across Docker, docs, and migrated source.

- [ ] T036 [P] Align `specs/002-migrate-cabinet-frontend/quickstart.md` with implemented frontend URL, service names, and verification commands
- [ ] T037 [P] Align `specs/002-migrate-cabinet-frontend/contracts/frontend-runtime.md` with implemented Compose service, healthcheck, and browser URL
- [ ] T038 Run frontend package install/build or equivalent container build verification and document any intentional skipped legacy tests in `frontend/cabinet/package.json`
- [ ] T039 Run `docker compose config` and record any required documentation adjustments in `README.md`
- [ ] T040 Run full local quickstart flow and update `README.md` if observed frontend behavior differs from documented behavior
- [ ] T041 Run `git status --short` and ensure `frontend/cabinet/node_modules/`, build output, coverage, and logs are not tracked in `.gitignore`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies; can start immediately.
- **Foundational (Phase 2)**: Depends on Setup; blocks all user stories because the source tree must exist first.
- **US1 (Phase 3)**: Depends on Foundational; delivers Docker-visible frontend MVP.
- **US2 (Phase 4)**: Depends on Foundational and benefits from US1 runtime to validate login behavior.
- **US3 (Phase 5)**: Depends on Foundational and can proceed after copied source structure is stable.
- **Polish (Phase 6)**: Depends on selected user stories being implemented.

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational; no dependency on US2 or US3.
- **US2 (P2)**: Can start after Foundational; final validation needs a runnable frontend from US1.
- **US3 (P3)**: Can start after Foundational; final validation should use US1 startup and US2 placeholder behavior.

### Within Each User Story

- Source copy before Vite/Docker configuration.
- Docker service before frontend runtime verification.
- Auth placeholder service changes before login page verification.
- Documentation alignment after observed runtime behavior is known.

### Parallel Opportunities

- T003 and T004 can run in parallel with T001/T002.
- T018 and T019 can run in parallel with Docker service implementation after service names are chosen.
- T024 can run in parallel with T023 because localization files are separate.
- T027 can run in parallel with T021-T026 because it updates the contract after behavior is selected.
- T032 and T034 can run in parallel with other US3 source review tasks.
- T036 and T037 can run in parallel during polish because they update separate files.

---

## Parallel Example: User Story 1

```bash
Task: "T015 [US1] Add frontend service, container name, build context, host port 5173, and network wiring in docker-compose.yml"
Task: "T018 [P] [US1] Update frontend runtime URL, service name, and logs documentation in README.md"
Task: "T019 [P] [US1] Align frontend runtime expectations with implementation in specs/002-migrate-cabinet-frontend/contracts/frontend-runtime.md"
```

## Parallel Example: User Story 2

```bash
Task: "T023 [US2] Add or adjust i18n message for authorization-not-connected state in frontend/cabinet/src/i18n/ru.ts"
Task: "T024 [P] [US2] Add or adjust English i18n message for authorization-not-connected state in frontend/cabinet/src/i18n/en.ts"
Task: "T027 [P] [US2] Update login placeholder behavior contract with final message key and route behavior in specs/002-migrate-cabinet-frontend/contracts/login-placeholder.md"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational source migration.
3. Complete Phase 3: User Story 1.
4. Stop and validate: `docker compose up --build --wait` and open `http://localhost:5173/cabinet/login`.
5. Demo migrated login screen loading from the new project.

### Incremental Delivery

1. Copy and configure frontend source under `frontend/cabinet/`.
2. Add Docker frontend service and verify login render.
3. Add placeholder login behavior that cannot authenticate.
4. Preserve/review remaining UI content and isolate legacy integration boundaries.
5. Finish with docs/contracts/quickstart consistency checks.

### Parallel Team Strategy

With multiple developers:

1. One developer migrates/corrects the source tree under `frontend/cabinet/`.
2. One developer wires Docker Compose and frontend container runtime.
3. One developer adapts login placeholder and route behavior.
4. One developer reviews preserved assets/components and documentation consistency.

## Summary

- Total tasks: 41
- Setup tasks: 4
- Foundational tasks: 9
- US1 tasks: 7
- US2 tasks: 8
- US3 tasks: 7
- Polish tasks: 6
- MVP scope: Phase 1 + Phase 2 + User Story 1
