# Tasks: Edit Existing Users in Cabinet

**Input**: Design documents from `/specs/014-edit-users/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Included because this feature changes auth-sensitive backend behavior, audit traceability, and admin-only cabinet flows.

**Organization**: Tasks are grouped by user story so each story can be implemented and verified independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (`US1`, `US2`, `US3`)
- Every task includes exact file paths in the description

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare shared frontend contract and localization scaffolding for user editing.

- [ ] T001 Add edit-user request/form TypeScript contracts in `frontend/cabinet/src/api/types/user-management.ts`
- [ ] T002 [P] Add edit-flow localization keys in `frontend/cabinet/src/i18n/keys.ts`
- [ ] T003 [P] Add Russian edit-flow localization messages in `frontend/cabinet/src/i18n/ru.ts`
- [ ] T004 [P] Add English edit-flow localization messages in `frontend/cabinet/src/i18n/en.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared backend and API plumbing required before user-story implementation.

**CRITICAL**: No user story work should begin until this phase is complete.

- [ ] T005 Add `USER_UPDATED` audit event type in `src/main/kotlin/com/ctfind/productioncontrol/auth/domain/AuthenticationAuditEvent.kt`
- [ ] T006 Extend update-related application ports/models in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/AuthenticationPorts.kt`
- [ ] T007 Add JPA repository methods for target-user lookup and active-admin counting in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/persistence/AuthJpaRepositories.kt`
- [ ] T008 Implement persistence adapter support for update flow and role-set replacement in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/persistence/AuthPersistenceAdapters.kt`
- [ ] T009 Add update-user request/response DTOs and web mapping helpers in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/web/AuthDtos.kt`
- [ ] T010 Add `updateUser(userId, payload)` API client and typed error parsing in `frontend/cabinet/src/api/composables/use-users.ts`

**Checkpoint**: Foundation ready - user stories can now proceed.

---

## Phase 3: User Story 1 - Admin Updates User Access (Priority: P1) 🎯 MVP

**Goal**: Admin edits existing user `displayName` and `roleCodes` from the users list and sees updated data immediately.

**Independent Test**: Admin edits a user from `/cabinet/users`, saves successfully, and sees updated name/roles in refreshed list/search.

### Tests for User Story 1

- [ ] T011 [P] [US1] Add `UpdateUserUseCase` happy-path tests for display-name and role updates in `src/test/kotlin/com/ctfind/productioncontrol/auth/application/UpdateUserUseCaseTests.kt`
- [ ] T012 [P] [US1] Add `PUT /api/users/{userId}` success contract tests in `src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserControllerTests.kt`
- [ ] T013 [P] [US1] Add users-page edit-success UI test coverage in `frontend/cabinet/tests/unit/pages/UsersPage.test.ts`

### Implementation for User Story 1

- [ ] T014 [US1] Implement `UpdateUserUseCase` success path and role normalization in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/UpdateUserUseCase.kt`
- [ ] T015 [US1] Add `PUT /api/users/{userId}` success handler in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserController.kt`
- [ ] T016 [US1] Add edit action and prefilled edit form state in `frontend/cabinet/src/pages/admin/UsersPage.vue`
- [ ] T017 [US1] Wire edit submit to `updateUser` API and list refresh in `frontend/cabinet/src/pages/admin/UsersPage.vue`

**Checkpoint**: US1 is independently functional and demo-ready.

---

## Phase 4: User Story 2 - Security Guardrails for Editing (Priority: P1)

**Goal**: Only admins can edit users, and the system prevents removing `ADMIN` from the last active administrator.

**Independent Test**: Non-admin update attempts are denied, and last-admin role removal is rejected with conflict response.

### Tests for User Story 2

- [ ] T018 [P] [US2] Add unauthorized and non-admin `PUT /api/users/{userId}` tests in `src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserControllerSecurityTests.kt`
- [ ] T019 [P] [US2] Add `UpdateUserUseCase` guard tests for forbidden actor and last-admin protection in `src/test/kotlin/com/ctfind/productioncontrol/auth/application/UpdateUserUseCaseTests.kt`
- [ ] T020 [P] [US2] Add non-admin route/edit-visibility regression tests in `frontend/cabinet/tests/unit/router/router-users.test.ts`

### Implementation for User Story 2

- [ ] T021 [US2] Enforce admin authorization and last-active-admin guard in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/UpdateUserUseCase.kt`
- [ ] T022 [US2] Map forbidden and last-admin conflict outcomes in `src/main/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserController.kt`
- [ ] T023 [US2] Hide edit controls for non-admin sessions in `frontend/cabinet/src/pages/admin/UsersPage.vue`

**Checkpoint**: US2 is independently verifiable through backend security checks and non-admin UI access behavior.

---

## Phase 5: User Story 3 - Transparent Edit Outcome (Priority: P2)

**Goal**: Admin receives clear success/error feedback for edits, and successful changes are auditable without sensitive data exposure.

**Independent Test**: Successful edits show confirmation and create audit events; validation/not-found/guard failures show clear UI errors and do not alter data.

### Tests for User Story 3

- [ ] T024 [P] [US3] Add update-audit and validation/not-found outcome tests in `src/test/kotlin/com/ctfind/productioncontrol/auth/application/UpdateUserUseCaseTests.kt`
- [ ] T025 [P] [US3] Add `PUT /api/users/{userId}` error mapping tests (`400/404/409`) in `src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserControllerTests.kt`
- [ ] T026 [P] [US3] Add users-page edit error/success feedback tests in `frontend/cabinet/tests/unit/pages/UsersPage.test.ts`

### Implementation for User Story 3

- [ ] T027 [US3] Record `USER_UPDATED` audit details with role delta and actor/target metadata in `src/main/kotlin/com/ctfind/productioncontrol/auth/application/UpdateUserUseCase.kt`
- [ ] T028 [US3] Implement update error-code parsing for form feedback in `frontend/cabinet/src/api/composables/use-users.ts`
- [ ] T029 [US3] Add localized success/error banners for edit outcomes in `frontend/cabinet/src/pages/admin/UsersPage.vue`
- [ ] T030 [US3] Handle `user_not_found` refresh-and-retry UX in `frontend/cabinet/src/pages/admin/UsersPage.vue`

**Checkpoint**: US3 is independently testable with both positive and negative edit outcomes.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final documentation and verification across all user stories.

- [ ] T031 [P] Update administrator edit-user guidance in `docs/PHASE_01_MANUAL.md`
- [ ] T032 [P] Align Phase 01 scope notes with edit capability in `docs/PHASE_01.md`
- [ ] T033 Run backend verification for auth changes in `src/test/kotlin/com/ctfind/productioncontrol/auth` with `make backend-test`
- [ ] T034 Run frontend verification for users-page changes in `frontend/cabinet/tests/unit` with `make frontend-test && make frontend-build`
- [ ] T035 Run full integration verification for repository roots `src` and `frontend/cabinet` with `make test && make build`
- [ ] T036 Execute smoke scenarios from `specs/014-edit-users/quickstart.md` against `/cabinet/users` and `PUT /api/users/{userId}`
- [ ] T037 Review changed update flow files in `src/main/kotlin/com/ctfind/productioncontrol/auth` and `frontend/cabinet/src/pages/admin/UsersPage.vue` for sensitive-data leakage

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup and blocks all story work.
- **US1 (Phase 3)**: Depends on Foundational.
- **US2 (Phase 4)**: Depends on Foundational; can run after or alongside US1 with coordination on shared backend files.
- **US3 (Phase 5)**: Depends on US1 + US2 outcomes and shared update endpoint behavior.
- **Polish (Phase 6)**: Depends on all intended user stories being complete.

### User Story Dependencies

- **US1 (P1)**: No dependency on other user stories after foundation.
- **US2 (P1)**: Uses same update flow and should be integrated with US1 endpoint/use-case changes.
- **US3 (P2)**: Depends on completed update flow and guardrail outcomes from US1/US2.

### Parallel Opportunities

- Setup tasks `T002`-`T004` run in parallel.
- Foundation tasks `T007` and `T010` can proceed in parallel once ports are agreed.
- US1 test tasks `T011`-`T013` run in parallel.
- US2 test tasks `T018`-`T020` run in parallel.
- US3 test tasks `T024`-`T026` run in parallel.
- Polish docs tasks `T031`-`T032` can run in parallel with verification tasks `T033`-`T036`.

## Parallel Example: User Story 1

```text
Task: "Add UpdateUserUseCase happy-path tests in src/test/kotlin/com/ctfind/productioncontrol/auth/application/UpdateUserUseCaseTests.kt"
Task: "Add PUT /api/users/{userId} success contract tests in src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserControllerTests.kt"
Task: "Add users-page edit-success UI tests in frontend/cabinet/tests/unit/pages/UsersPage.test.ts"
```

## Parallel Example: User Story 2

```text
Task: "Add unauthorized and non-admin PUT security tests in src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserControllerSecurityTests.kt"
Task: "Add last-admin guard tests in src/test/kotlin/com/ctfind/productioncontrol/auth/application/UpdateUserUseCaseTests.kt"
Task: "Add non-admin route/edit visibility regression tests in frontend/cabinet/tests/unit/router/router-users.test.ts"
```

## Parallel Example: User Story 3

```text
Task: "Add update-audit and validation/not-found outcome tests in src/test/kotlin/com/ctfind/productioncontrol/auth/application/UpdateUserUseCaseTests.kt"
Task: "Add PUT /api/users/{userId} error mapping tests in src/test/kotlin/com/ctfind/productioncontrol/auth/adapter/web/UserControllerTests.kt"
Task: "Add users-page edit error/success feedback tests in frontend/cabinet/tests/unit/pages/UsersPage.test.ts"
```

## Implementation Strategy

### MVP First (US1)

1. Complete Setup and Foundational phases.
2. Deliver US1 happy-path edit flow end-to-end.
3. Validate US1 independently from `/cabinet/users` and API success contract.
4. Proceed to security and audit hardening.

### Incremental Delivery

1. **US1**: Basic admin edit capability (name + roles + refresh).
2. **US2**: Security guardrails (admin-only + last-admin protection).
3. **US3**: Transparent feedback and auditable update outcomes.
4. **Polish**: Docs and full verification.

### Parallel Team Strategy

- **Backend track**: `T005`-`T009`, `T011`-`T015`, `T018`-`T022`, `T024`-`T027`.
- **Frontend track**: `T001`-`T004`, `T010`, `T013`, `T016`-`T017`, `T020`, `T023`, `T026`, `T028`-`T030`.
- **QA/docs track**: `T031`-`T037` once US1-US3 are merged.

## Notes

- Preserve hexagonal boundaries: business rules stay in application layer, not controllers or Vue components.
- Keep backend API-only behavior with explicit `401/403` semantics.
- Ensure `PUT /api/users/{userId}` never returns or logs sensitive credentials.
- Keep out-of-scope items excluded: login/password change, disable/delete, bulk edits, external IdP integration.
