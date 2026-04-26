# Tasks: Login API Authentication

**Input**: Design documents from `specs/003-login-api-auth/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`

**Tests**: Included because the feature specification and plan require backend, frontend, and Docker verification for authentication, security, audit, and local runtime behavior.

**Organization**: Tasks are grouped by user story so each story can be implemented and tested as an independently valuable increment.

**Constitution**: Preserve ERP/domain traceability, future TOC accountability through actor identity, clean/hexagonal backend boundaries, API-only security behavior, and Docker-first verification.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel with other tasks in the same phase because it touches different files or has no dependency on incomplete tasks.
- **[Story]**: User story label for story-specific tasks.
- Every task includes exact file paths.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add shared dependencies and configuration required before auth implementation.

- [X] T001 Add Spring Security OAuth2 Resource Server and JOSE dependencies in `build.gradle.kts`
- [X] T002 Add local JWT issuer, signing secret, and 8-hour expiration properties in `src/main/resources/application-local.properties`
- [X] T003 [P] Add auth API base URL and localStorage key constants in `frontend/cabinet/src/api/auth-service.ts`
- [X] T004 [P] Add authentication quickstart references to local runtime docs in `README.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish schema, module boundaries, and security infrastructure that all user stories depend on.

**Critical**: No user story work should begin until this phase is complete.

- [X] T005 Create Flyway migration for `app_user`, `app_role`, `app_user_role`, and `auth_audit_event` tables in `src/main/resources/db/migration/V2__create_auth_tables.sql`
- [X] T006 [P] Create `UserAccount` domain model and login normalization in `src/main/kotlin/com/ctfind/productioncontrol/auth/domain/UserAccount.kt`
- [X] T007 [P] Create `Role` domain model and `ADMIN` role code in `src/main/kotlin/com/ctfind/productioncontrol/auth/domain/Role.kt`
- [X] T008 [P] Create `AuthenticationAuditEvent` domain model and event/outcome enums in `src/main/kotlin/com/ctfind/productioncontrol/auth/domain/AuthenticationAuditEvent.kt`
- [X] T009 [P] Create `LoginThrottlePolicy` domain model for runtime failure buckets in `src/main/kotlin/com/ctfind/productioncontrol/auth/domain/LoginThrottlePolicy.kt`
- [X] T010 [P] Define auth application ports in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/AuthenticationPorts.kt`
- [X] T011 [P] Define auth application result and command types in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/AuthenticationModels.kt`
- [X] T012 [P] Create JPA auth entities and mappings in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/persistence/AuthJpaEntities.kt`
- [X] T013 [P] Create JPA auth repositories in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/persistence/AuthJpaRepositories.kt`
- [X] T014 Create persistence adapters for user, role, and audit ports in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/persistence/AuthPersistenceAdapters.kt`
- [X] T015 Create JWT issuer and decoder configuration in `src/main/kotlin/com/ctfind/productioncontrol/infrastructure/security/JwtSecurityConfig.kt`
- [X] T016 Update API-only stateless Bearer security rules in `src/main/kotlin/com/ctfind/productioncontrol/config/SecurityConfig.kt`
- [X] T017 [P] Create shared JSON error response DTO in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/web/AuthErrorResponse.kt`
- [X] T018 [P] Add backend security regression tests for no form login or Basic Auth challenge in `src/test/kotlin/com/ctfind/productioncontrol/config/SecurityConfigTests.kt`
- [X] T019 [P] Add migration smoke test for auth tables in `src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/persistence/AuthMigrationTests.kt`

**Checkpoint**: Auth schema, domain/application boundaries, persistence adapters, JWT infrastructure, and API-only security baseline are ready.

---

## Phase 3: User Story 1 - Sign in with seeded local administrator (Priority: P1) - MVP

**Goal**: A fresh local installation seeds `admin` / `admin`, login succeeds through the new platform, and the cabinet reaches a protected page.

**Independent Test**: Start from clean local data, open `/cabinet/login`, submit `admin` / `admin`, and see an authenticated cabinet page instead of the placeholder message.

### Tests for User Story 1

- [X] T020 [P] [US1] Add seed idempotency tests for `admin` and `ADMIN` role in `src/test/kotlin/com/ctfind/productioncontrol/auth/application/LocalAdminSeedUseCaseTests.kt`
- [X] T021 [P] [US1] Add successful login API test for `POST /api/auth/login` in `src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/AuthControllerLoginTests.kt`
- [X] T022 [P] [US1] Add frontend auth service success test for `/api/auth/login` in `frontend/cabinet/tests/unit/api/auth-service.test.ts`
- [X] T023 [P] [US1] Add frontend login page success routing test in `frontend/cabinet/tests/unit/pages/login-page.test.ts`

### Implementation for User Story 1

- [X] T024 [US1] Implement password hashing configuration in `src/main/kotlin/com/ctfind/productioncontrol/infrastructure/security/PasswordSecurityConfig.kt`
- [X] T025 [US1] Implement local admin seed use case in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/LocalAdminSeedUseCase.kt`
- [X] T026 [US1] Run local admin seed on local profile startup in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/persistence/LocalAdminSeedRunner.kt`
- [X] T027 [US1] Implement authenticate user use case for valid credentials and JWT response in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/AuthenticateUserUseCase.kt`
- [X] T028 [US1] Implement auth request and response DTOs in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/web/AuthDtos.kt`
- [X] T029 [US1] Implement `POST /api/auth/login` endpoint in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/web/AuthController.kt`
- [X] T030 [US1] Replace placeholder login call with `/api/auth/login` in `frontend/cabinet/src/api/auth-service.ts`
- [X] T031 [US1] Persist successful login token and user state in `frontend/cabinet/src/stores/auth.ts`
- [X] T032 [US1] Redirect after successful login to sanitized target or `/cabinet` in `frontend/cabinet/src/stores/auth.ts`
- [X] T033 [US1] Update login i18n copy to remove placeholder-only behavior in `frontend/cabinet/src/i18n/ru.ts` and `frontend/cabinet/src/i18n/en.ts`
- [X] T034 [US1] Verify seeded login API smoke scenario from `specs/003-login-api-auth/quickstart.md`

**Checkpoint**: User Story 1 works independently: clean local data seeds one admin account, backend login returns a Bearer token, and frontend login reaches a protected cabinet page.

---

## Phase 4: User Story 2 - Protect cabinet routes with app-owned authentication (Priority: P2)

**Goal**: Protected cabinet and backend routes honor Bearer authentication, preserve safe return routes, and survive refresh while the token remains valid.

**Independent Test**: Open `/cabinet/orders` unauthenticated, confirm redirect to login with a safe `from` path, sign in, return to `/cabinet/orders`, refresh, and remain authenticated.

### Tests for User Story 2

- [X] T035 [P] [US2] Add backend authenticated `/api/auth/me` and unauthorized tests in `src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/AuthControllerMeTests.kt`
- [X] T036 [P] [US2] Add Bearer token security filter tests for protected API access in `src/test/kotlin/com/ctfind/productioncontrol/config/SecurityConfigBearerTests.kt`
- [X] T037 [P] [US2] Add frontend token bootstrap and refresh tests in `frontend/cabinet/tests/unit/stores/auth.test.ts`
- [X] T038 [P] [US2] Add router guard return-path tests in `frontend/cabinet/tests/unit/router/auth-guard.test.ts`

### Implementation for User Story 2

- [X] T039 [US2] Implement authenticated principal mapping from JWT claims in `src/main/kotlin/com/ctfind/productioncontrol/infrastructure/security/JwtAuthenticationMapper.kt`
- [X] T040 [US2] Implement `GET /api/auth/me` endpoint in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/web/AuthController.kt`
- [X] T041 [US2] Attach Bearer token to shared frontend HTTP client requests in `frontend/cabinet/src/api/frappe-client.ts`
- [X] T042 [US2] Bootstrap frontend auth state from `localStorage` and `/api/auth/me` in `frontend/cabinet/src/stores/auth.ts`
- [X] T043 [US2] Clear invalid or expired stored token on `401` in `frontend/cabinet/src/stores/auth.ts`
- [X] T044 [US2] Align cabinet route guard role checks with backend `ADMIN` role code in `frontend/cabinet/src/router/index.ts`
- [X] T045 [US2] Verify protected-route return and refresh behavior from `specs/003-login-api-auth/quickstart.md`

**Checkpoint**: User Story 2 works independently: protected routes redirect when unauthenticated, return safely after login, and accept valid Bearer tokens without browser login prompts.

---

## Phase 5: User Story 3 - Handle failed sign-in safely and traceably (Priority: P3)

**Goal**: Invalid, blank, disabled, throttled, logout, and seed outcomes are safe, generic to users, and auditable without sensitive credential data.

**Independent Test**: Submit invalid credentials repeatedly, observe no authenticated state, generic failure or retry-later messages, and audit records for login success/failure/logout/seed activity without passwords.

### Tests for User Story 3

- [X] T046 [P] [US3] Add invalid and blank login API tests in `src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/AuthControllerFailureTests.kt`
- [X] T047 [P] [US3] Add throttle policy tests for repeated login/IP failures in `src/test/kotlin/com/ctfind/productioncontrol/auth/application/LoginThrottlePolicyTests.kt`
- [X] T048 [P] [US3] Add authentication audit tests for success, failure, logout, and seed events in `src/test/kotlin/com/ctfind/productioncontrol/auth/application/AuthenticationAuditTests.kt`
- [X] T049 [P] [US3] Add frontend invalid and throttled login tests in `frontend/cabinet/tests/unit/api/auth-service.test.ts`
- [X] T050 [P] [US3] Add frontend logout token-removal test in `frontend/cabinet/tests/unit/stores/auth.test.ts`

### Implementation for User Story 3

- [X] T051 [US3] Implement failure, disabled-account, and validation branches in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/AuthenticateUserUseCase.kt`
- [X] T052 [US3] Implement in-memory login/IP throttle service in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/LoginThrottleService.kt`
- [X] T053 [US3] Return `400`, `401`, and `429` auth errors from `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/web/AuthController.kt`
- [X] T054 [US3] Implement audit recording for login success, login failure, logout, and seed in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/AuthenticationAuditService.kt`
- [X] T055 [US3] Implement `POST /api/auth/logout` endpoint in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/web/AuthController.kt`
- [X] T056 [US3] Map backend auth errors to `invalid`, `rateLimit`, and `network` outcomes in `frontend/cabinet/src/api/auth-service.ts`
- [X] T057 [US3] Remove stored token and clear auth state on logout in `frontend/cabinet/src/stores/auth.ts`
- [X] T058 [US3] Update login error i18n for retry-later and generic failure in `frontend/cabinet/src/i18n/ru.ts` and `frontend/cabinet/src/i18n/en.ts`
- [X] T059 [US3] Verify failed login, throttling, and logout smoke scenarios from `specs/003-login-api-auth/quickstart.md`

**Checkpoint**: User Story 3 works independently: bad credentials and throttled attempts never authenticate, logout clears local state, and auth audit records are present without sensitive data.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final verification, documentation, and cleanup across the full feature.

- [X] T060 [P] Update frontend README authentication notes in `frontend/cabinet/README.md`
- [X] T061 [P] Update root local runtime authentication instructions in `README.md`
- [X] T062 Run backend test suite and record result for this feature in `specs/003-login-api-auth/quickstart.md`
- [X] T063 Run frontend test and build commands and record result for this feature in `specs/003-login-api-auth/quickstart.md`
- [X] T064 Run Docker Compose fresh-start smoke check and record result for this feature in `specs/003-login-api-auth/quickstart.md`
- [X] T065 Review API-only behavior, audit coverage, and local-MVP security caveats against `specs/003-login-api-auth/plan.md`
- [X] T066 Ensure no legacy Frappe login endpoint is used in `frontend/cabinet/src/api/auth-service.ts`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup completion and blocks all user stories.
- **User Story 1 (Phase 3)**: Depends on Foundational completion and is the MVP.
- **User Story 2 (Phase 4)**: Depends on Foundational completion and can be implemented after or alongside US1 once login contract types are stable.
- **User Story 3 (Phase 5)**: Depends on Foundational completion and uses login/logout endpoints from US1/US2.
- **Polish (Phase 6)**: Depends on all desired user stories being complete.

### User Story Dependencies

- **US1 (P1)**: No dependency on other stories after Foundation.
- **US2 (P2)**: Requires the login response contract and token storage shape from US1, but route guard tests can be prepared earlier.
- **US3 (P3)**: Requires the authentication use case and controller from US1 and logout/authenticated principal behavior from US2.

### Within Each User Story

- Tests are written before implementation tasks for the story.
- Domain/application rules precede web, persistence, and frontend adapter wiring.
- Backend API behavior is verified before Docker smoke checks.
- Frontend store/router behavior is verified before browser smoke checks.

## Parallel Opportunities

- T003 and T004 can run in parallel after T001/T002 decisions are clear.
- T006 through T013 and T017 through T019 can run in parallel after T005 is drafted because they touch separate files.
- US1 test tasks T020 through T023 can run in parallel.
- US2 test tasks T035 through T038 can run in parallel.
- US3 test tasks T046 through T050 can run in parallel.
- Documentation polish tasks T060 and T061 can run in parallel.

## Parallel Example: User Story 1

```bash
Task: "Add seed idempotency tests for admin and ADMIN role in src/test/kotlin/com/ctfind/productioncontrol/auth/application/LocalAdminSeedUseCaseTests.kt"
Task: "Add successful login API test for POST /api/auth/login in src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/AuthControllerLoginTests.kt"
Task: "Add frontend auth service success test for /api/auth/login in frontend/cabinet/tests/unit/api/auth-service.test.ts"
Task: "Add frontend login page success routing test in frontend/cabinet/tests/unit/pages/login-page.test.ts"
```

## Parallel Example: User Story 2

```bash
Task: "Add backend authenticated /api/auth/me and unauthorized tests in src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/AuthControllerMeTests.kt"
Task: "Add Bearer token security filter tests for protected API access in src/test/kotlin/com/ctfind/productioncontrol/config/SecurityConfigBearerTests.kt"
Task: "Add frontend token bootstrap and refresh tests in frontend/cabinet/tests/unit/stores/auth.test.ts"
Task: "Add router guard return-path tests in frontend/cabinet/tests/unit/router/auth-guard.test.ts"
```

## Parallel Example: User Story 3

```bash
Task: "Add invalid and blank login API tests in src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/AuthControllerFailureTests.kt"
Task: "Add throttle policy tests for repeated login/IP failures in src/test/kotlin/com/ctfind/productioncontrol/auth/application/LoginThrottlePolicyTests.kt"
Task: "Add authentication audit tests for success, failure, logout, and seed events in src/test/kotlin/com/ctfind/productioncontrol/auth/application/AuthenticationAuditTests.kt"
Task: "Add frontend invalid and throttled login tests in frontend/cabinet/tests/unit/api/auth-service.test.ts"
```

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 for US1.
3. Validate clean local seed, `admin` / `admin` login API, frontend login success, and no placeholder auth message.
4. Stop and demo before adding route refresh, logout, audit hardening, and throttling.

### Incremental Delivery

1. Foundation: schema, auth domain/application boundaries, persistence adapters, JWT security.
2. US1: seeded local admin login and frontend success path.
3. US2: protected route return, token bootstrap, `/api/auth/me`, and refresh survival.
4. US3: negative paths, throttle, logout, and durable audit.
5. Polish: docs, full verification, Docker smoke, and security caveat review.

### Quality Gates

- All changed backend behavior must preserve API-only security and explicit `401`/`403` responses.
- Audit records must not contain submitted passwords or full JWT values.
- Root `docker compose up --build --wait` must remain the primary local runtime path.
- Legacy Frappe login endpoints must not be used for this feature.
