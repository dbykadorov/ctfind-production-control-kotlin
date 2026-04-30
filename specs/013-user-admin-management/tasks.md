# Tasks: Superadmin Seed and User Management

**Input**: Design documents from `/specs/013-user-admin-management/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Included because this feature changes authentication, authorization, persistence, startup behavior, and user-facing cabinet access.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Every task includes exact file paths

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare shared files and role catalog scaffolding.

- [X] T001 Create role catalog migration for `ADMIN`, `ORDER_MANAGER`, `WAREHOUSE`, `PRODUCTION_SUPERVISOR`, and `PRODUCTION_EXECUTOR` in `src/main/resources/db/migration/V10__ensure_auth_role_catalog.sql`
- [X] T002 [P] Add user-management TypeScript API types in `frontend/cabinet/src/api/types/user-management.ts`
- [X] T003 [P] Add user-management i18n keys in `frontend/cabinet/src/i18n/keys.ts`
- [X] T004 [P] Add Russian user-management translations in `frontend/cabinet/src/i18n/ru.ts`
- [X] T005 [P] Add English fallback user-management translations in `frontend/cabinet/src/i18n/en.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared backend contracts, ports, audit vocabulary, and frontend API surface required by all user stories.

**CRITICAL**: No user story work should begin until this phase is complete.

- [X] T006 Extend `UserSummary` to include assigned roles and add `RoleSummary` in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/AuthenticationPorts.kt`
- [X] T007 Extend `UserSummaryResponse` and add create-user/role DTOs in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/web/AuthDtos.kt`
- [X] T008 Add role catalog and admin-existence port methods in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/AuthenticationPorts.kt`
- [X] T009 Add `USER_CREATED` bootstrap/user-management audit event vocabulary in `src/main/kotlin/com/ctfind/productioncontrol/auth/domain/AuthenticationAuditEvent.kt`
- [X] T010 Implement role-aware user search and role catalog persistence mapping in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/persistence/AuthPersistenceAdapters.kt`
- [X] T011 Add repository queries needed for admin-existence and role catalog checks in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/persistence/AuthJpaRepositories.kt`
- [X] T012 Create shared user-management composable shell for list, roles, and create calls in `frontend/cabinet/src/api/composables/use-users.ts`

**Checkpoint**: Foundation ready - user story implementation can now begin.

---

## Phase 3: User Story 1 - Initial Superadmin Access (Priority: P1) MVP

**Goal**: A fresh environment has a safe, idempotent path to the first administrator; production-like startup fails clearly without secure credentials.

**Independent Test**: Start from no administrator account and verify local bootstrap, production-like secure bootstrap, missing-credentials failure, and repeated-start idempotency.

### Tests for User Story 1

- [X] T013 [P] [US1] Add local/default bootstrap and idempotency tests in `src/test/kotlin/com/ctfind/productioncontrol/auth/application/EnsureSuperadminUseCaseTests.kt`
- [X] T014 [P] [US1] Add production-like missing-credentials failure tests in `src/test/kotlin/com/ctfind/productioncontrol/auth/application/EnsureSuperadminUseCaseTests.kt`
- [X] T015 [P] [US1] Add superadmin bootstrap runner configuration tests in `src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/persistence/SuperadminSeedRunnerTests.kt`

### Implementation for User Story 1

- [X] T016 [US1] Replace local-only bootstrap policy with `EnsureSuperadminUseCase` in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/EnsureSuperadminUseCase.kt`
- [X] T017 [US1] Add superadmin bootstrap properties and validation in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/persistence/SuperadminSeedRunner.kt`
- [X] T018 [US1] Update existing local admin seed integration to delegate or coexist safely in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/LocalAdminSeedUseCase.kt`
- [X] T019 [US1] Add secure bootstrap configuration keys and local defaults in `src/main/resources/application-local.properties`
- [X] T020 [US1] Add production-like bootstrap credential documentation comments/default handling in `src/main/resources/application.properties`
- [X] T021 [US1] Record password-free bootstrap audit outcomes in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/EnsureSuperadminUseCase.kt`

**Checkpoint**: US1 is testable independently with bootstrap tests and local startup smoke check.

---

## Phase 4: User Story 2 - Admin Views Users (Priority: P2)

**Goal**: Admin users can open a cabinet Users section, search users, and see assigned roles for each result.

**Independent Test**: Sign in as admin and non-admin; only admin sees `/cabinet/users`, and search results show role labels/codes.

### Tests for User Story 2

- [X] T022 [P] [US2] Add role-aware user search use-case tests in `src/test/kotlin/com/ctfind/productioncontrol/auth/application/UserQueryUseCaseTests.kt`
- [X] T023 [P] [US2] Add `GET /api/users` response contract tests in `src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserControllerTests.kt`
- [X] T024 [P] [US2] Add Users route/sidebar visibility tests in `frontend/cabinet/tests/unit/router-users.test.ts`
- [X] T025 [P] [US2] Add Users page list/search rendering tests in `frontend/cabinet/tests/unit/pages/UsersPage.test.ts`

### Implementation for User Story 2

- [X] T026 [US2] Update `UserQueryUseCase` to return role-aware user summaries in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/UserQueryUseCase.kt`
- [X] T027 [US2] Update `GET /api/users` mapping to include assigned roles in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserController.kt`
- [X] T028 [US2] Implement `fetchUsers` replacement using role-aware `UserSummaryResponse` in `frontend/cabinet/src/api/composables/use-users.ts`
- [X] T029 [US2] Add `/cabinet/users` admin route in `frontend/cabinet/src/router/index.ts`
- [X] T030 [US2] Add admin-only Users sidebar item in `frontend/cabinet/src/components/layout/Sidebar.vue`
- [X] T031 [US2] Create searchable Users page with role display states in `frontend/cabinet/src/pages/admin/UsersPage.vue`

**Checkpoint**: US2 is testable independently by opening `/cabinet/users` as admin and verifying role-aware search.

---

## Phase 5: User Story 3 - Admin Creates Users with Roles (Priority: P3)

**Goal**: Admin users can create enabled users with login, display name, non-empty initial password, and any supported role set including `ADMIN`.

**Independent Test**: Create `warehouse.demo` with `WAREHOUSE`, verify it appears in search, sign in as that user, and verify role-scoped cabinet access.

### Tests for User Story 3

- [X] T032 [P] [US3] Add create-user success, duplicate login, no roles, invalid roles, empty password, and ADMIN-role tests in `src/test/kotlin/com/ctfind/productioncontrol/auth/application/CreateUserUseCaseTests.kt`
- [X] T033 [P] [US3] Add role catalog use-case tests in `src/test/kotlin/com/ctfind/productioncontrol/auth/application/RoleCatalogUseCaseTests.kt`
- [X] T034 [P] [US3] Add `POST /api/users` and `GET /api/users/roles` contract tests in `src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserControllerTests.kt`
- [X] T035 [P] [US3] Add create-user form success/validation tests in `frontend/cabinet/tests/unit/pages/UsersPage.test.ts`

### Implementation for User Story 3

- [X] T036 [US3] Implement create-user command/result validation in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/CreateUserUseCase.kt`
- [X] T037 [US3] Implement supported role catalog query in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/RoleCatalogUseCase.kt`
- [X] T038 [US3] Add `POST /api/users` and `GET /api/users/roles` handlers in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserController.kt`
- [X] T039 [US3] Add create-user API and role catalog API calls in `frontend/cabinet/src/api/composables/use-users.ts`
- [X] T040 [US3] Add create-user form/dialog with role multi-select in `frontend/cabinet/src/pages/admin/UsersPage.vue`
- [X] T041 [US3] Add success refresh and error handling for duplicate login, validation, invalid roles, 401, and 403 in `frontend/cabinet/src/pages/admin/UsersPage.vue`
- [X] T042 [US3] Ensure user-created audit event excludes password values in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/CreateUserUseCase.kt`

**Checkpoint**: US3 is testable independently by creating a user and signing in with the assigned initial password.

---

## Phase 6: User Story 4 - Admin-Only User Creation (Priority: P4)

**Goal**: Direct API and UI attempts by unauthenticated or non-admin users are denied consistently.

**Independent Test**: Attempt list, role catalog, and creation as unauthenticated, non-admin, and admin; only admin succeeds.

### Tests for User Story 4

- [X] T043 [P] [US4] Add unauthenticated and non-admin negative tests for `GET /api/users`, `GET /api/users/roles`, and `POST /api/users` in `src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserControllerSecurityTests.kt`
- [X] T044 [P] [US4] Add non-admin sidebar/route denial tests in `frontend/cabinet/tests/unit/router-users.test.ts`

### Implementation for User Story 4

- [X] T045 [US4] Enforce ADMIN-only checks for list, role catalog, and create use cases in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/UserQueryUseCase.kt`
- [X] T046 [US4] Map unauthenticated and forbidden responses consistently in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserController.kt`
- [X] T047 [US4] Ensure `/cabinet/users` route metadata and sidebar visibility deny non-admin users in `frontend/cabinet/src/router/index.ts`
- [X] T048 [US4] Ensure Users page never exposes password or password hash in `frontend/cabinet/src/pages/admin/UsersPage.vue`

**Checkpoint**: US4 is testable independently with direct API negative cases and non-admin UI navigation checks.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, verification, and cleanup across all stories.

- [X] T049 [P] Update administrator manual user-management section in `docs/PHASE_01_MANUAL.md`
- [X] T050 [P] Update Phase 01 overview if user-management scope is described in `docs/PHASE_01.md`
- [X] T051 [P] Reconcile generated/admin API documentation with `specs/013-user-admin-management/contracts/rest-users.md`
- [X] T052 Run backend tests with `make backend-test`
- [X] T053 Run frontend tests and build with `make frontend-test && make frontend-build`
- [X] T054 Run full verification with `make test && make build`
- [X] T055 Validate runtime smoke scenarios from `specs/013-user-admin-management/quickstart.md`
- [X] T056 Review changed files for password leakage in logs, audit details, API responses, and frontend state in `src/main/kotlin/com/ctfind/productioncontrol/auth` and `frontend/cabinet/src/pages/admin/UsersPage.vue`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup and blocks all user stories.
- **US1 (Phase 3)**: Depends on Foundation; recommended MVP because it enables the first administrator.
- **US2 (Phase 4)**: Depends on Foundation; can proceed after US1 for realistic admin login, but backend/UI list work is otherwise isolated.
- **US3 (Phase 5)**: Depends on US2 route/page shell and Foundation.
- **US4 (Phase 6)**: Depends on US2 and US3 endpoints/UI existing.
- **Polish (Phase 7)**: Depends on implemented story set.

### User Story Dependencies

- **US1 (P1)**: No dependency on other user stories.
- **US2 (P2)**: Depends on foundational role-aware summaries; no dependency on create-user behavior.
- **US3 (P3)**: Depends on role catalog and Users page shell from US2.
- **US4 (P4)**: Depends on endpoints/routes from US2 and US3 for negative coverage.

### Parallel Opportunities

- Setup tasks T002-T005 can run in parallel.
- Foundation tasks T006-T012 touch separate files and can be split after agreeing on DTO/port names.
- US1 tests T013-T015 can be written in parallel before T016-T021.
- US2 tests T022-T025 can be written in parallel before T026-T031.
- US3 tests T032-T035 can be written in parallel before T036-T042.
- US4 tests T043-T044 can be written in parallel before T045-T048.
- Documentation tasks T049-T051 can run after contracts stabilize while verification tasks T052-T055 run separately.

## Parallel Example: User Story 2

```text
Task: "Add role-aware user search use-case tests in src/test/kotlin/com/ctfind/productioncontrol/auth/application/UserQueryUseCaseTests.kt"
Task: "Add GET /api/users response contract tests in src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserControllerTests.kt"
Task: "Add Users route/sidebar visibility tests in frontend/cabinet/tests/unit/router-users.test.ts"
Task: "Add Users page list/search rendering tests in frontend/cabinet/tests/unit/pages/UsersPage.test.ts"
```

## Parallel Example: User Story 3

```text
Task: "Add create-user success, duplicate login, no roles, invalid roles, empty password, and ADMIN-role tests in src/test/kotlin/com/ctfind/productioncontrol/auth/application/CreateUserUseCaseTests.kt"
Task: "Add role catalog use-case tests in src/test/kotlin/com/ctfind/productioncontrol/auth/application/RoleCatalogUseCaseTests.kt"
Task: "Add POST /api/users and GET /api/users/roles contract tests in src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserControllerTests.kt"
Task: "Add create-user form success/validation tests in frontend/cabinet/tests/unit/pages/UsersPage.test.ts"
```

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Setup and Foundation.
2. Complete US1 bootstrap behavior.
3. Verify local bootstrap, production-like missing-credentials failure, and idempotency.
4. Stop and validate before adding UI.

### Incremental Delivery

1. US1: safe superadmin bootstrap.
2. US2: admin-only Users page with role-aware list/search.
3. US3: create users with roles and audit.
4. US4: harden negative authorization coverage.
5. Polish: docs, quickstart, full build/test/runtime verification.

### Team Parallelization

- Backend developer: T006-T011, T013-T023, T026-T038, T042-T046.
- Frontend developer: T002-T005, T012, T024-T025, T028-T031, T035, T039-T041, T047-T048.
- Documentation/QA: T049-T055 after contracts and UI stabilize.

## Notes

- Preserve auth module boundaries: permission checks and validation live in application use cases, not controllers or Vue pages.
- Keep API responses password-free.
- Keep production-like bootstrap safe: no hardcoded production superadmin password.
- Do not implement edit, disable, delete, password reset, first-login rotation, self-registration, or external identity providers in this increment.
