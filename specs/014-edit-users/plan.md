# Implementation Plan: Edit Existing Users in Cabinet

**Branch**: `014-edit-users` | **Date**: 2026-04-30 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/014-edit-users/spec.md`

## Summary

Add admin-only editing of existing users in Phase 01 without expanding into full user lifecycle management. The feature introduces a focused update flow for `displayName` and `roleCodes`, with strict authorization checks, protection from removing the last active administrator, immediate list refresh in cabinet UI, and auditable user-update events without sensitive data.

## Technical Context

**Language/Version**: Kotlin + Spring Boot on Java 21; Vue 3 + Vite + TypeScript in `frontend/cabinet`  
**Primary Dependencies**: Spring Web/Security/OAuth2 resource server, Spring Data JPA, PostgreSQL/Flyway, existing auth audit services, Vue Router, Pinia, axios composables, vue-i18n  
**Storage**: PostgreSQL tables `app_user`, `app_role`, `app_user_role`, `auth_audit_event` (no new table expected)  
**Testing**: `make backend-test`, `make frontend-test`, `make frontend-build`, plus targeted auth/controller and users-page tests  
**Target Platform**: Linux Docker Compose local runtime; production-like API + SPA deployment  
**Project Type**: Web application with API-only backend and SPA frontend  
**Performance Goals**: Admin can complete one user edit interaction in under 1 minute; successful save is reflected in user list on next refresh cycle (interactive UX expectation)  
**Constraints**: API must keep explicit `401/403`; edit flow must not change login or password; system must reject removing `ADMIN` from the last active admin; audit/details/responses must not expose password data  
**Scale/Scope**: Minimal Phase 01 edit scope for existing users (single-record update), no bulk actions, no delete/disable/reset/login-change

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **ERP domain fit**: Strengthens the personnel/role administration workflow so access corrections do not require user recreation or manual database intervention.
- **Constraint-aware operations**: Preserves accurate role assignment facts that govern operational visibility to orders, production, warehouse, and audit flows; does not alter production prioritization semantics.
- **Architecture boundaries**: Business rules (validation, permission checks, last-admin guard) stay in auth application use cases; controllers/adapters remain transport and mapping layers.
- **Traceability/audit**: User profile and role updates are privileged business-state changes and must produce password-free audit events.
- **API-only/security**: Backend remains API-only with JWT-based explicit authorization behavior and negative scenarios for unauthenticated/non-admin actors.
- **Docker/verifiability**: Root Docker startup remains authoritative; completion evidence requires backend tests, frontend tests/build, health check, and quickstart smoke scenarios for update success/failure.
- **Exception handling**: No constitution exceptions anticipated.

## Project Structure

### Documentation (this feature)

```text
specs/014-edit-users/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── rest-users-edit.md
│   └── frontend-users-edit.md
└── tasks.md              # Generated later by /speckit-tasks
```

### Source Code (repository root)

```text
src/main/kotlin/com/ctfind/productioncontrol/auth/
├── application/
│   ├── AuthenticationPorts.kt
│   ├── UpdateUserUseCase.kt
│   └── UserQueryUseCase.kt
├── adapter/web/
│   ├── AuthDtos.kt
│   └── UserController.kt
└── adapter/persistence/
    ├── AuthJpaRepositories.kt
    └── AuthPersistenceAdapters.kt

src/test/kotlin/com/ctfind/productioncontrol/auth/
├── application/
│   └── UpdateUserUseCaseTests.kt
└── adapter/web/
    ├── UserControllerTests.kt
    └── UserControllerSecurityTests.kt

frontend/cabinet/src/
├── api/composables/use-users.ts
├── api/types/user-management.ts
├── pages/admin/UsersPage.vue
├── i18n/
└── components/layout/Sidebar.vue

frontend/cabinet/tests/unit/
├── pages/UsersPage.test.ts
└── router/router-users.test.ts
```

**Structure Decision**: Reuse existing auth module and cabinet users page introduced in `013-user-admin-management`; extend current contracts/use-cases instead of adding a new module or separate admin application area.

## Phase 0: Research Summary

See [research.md](./research.md). Research resolves update semantics, conflict handling, last-admin guard behavior, and audit payload boundaries for user edits.

## Phase 1: Design Summary

See [data-model.md](./data-model.md), [contracts/rest-users-edit.md](./contracts/rest-users-edit.md), [contracts/frontend-users-edit.md](./contracts/frontend-users-edit.md), and [quickstart.md](./quickstart.md).

## Post-Design Constitution Check

- **ERP domain fit**: PASS — design directly improves corrective access management for ERP roles.
- **Constraint-aware operations**: PASS — role corrections keep operational access aligned without changing TOC-related production flow data.
- **Architecture boundaries**: PASS — update policy and guardrails remain in use cases; adapters only map input/output.
- **Traceability/audit**: PASS — design requires `USER_UPDATED` audit events with explicit changed fields and no secrets.
- **API-only/security**: PASS — admin-only edit API with explicit `401/403/404/409` outcomes and no backend-rendered login behavior.
- **Docker/verifiability**: PASS — quickstart includes docker/health and edit-flow smoke checks; plan includes backend/frontend verification commands.
- **Exception handling**: No exceptions.

## Complexity Tracking

No constitution violations or exceptional complexity.
