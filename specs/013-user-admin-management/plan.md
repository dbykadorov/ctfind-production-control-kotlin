# Implementation Plan: Superadmin Seed and User Management

**Branch**: `013-user-admin-management` | **Date**: 2026-04-30 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/013-user-admin-management/spec.md`

## Summary

Add a minimal administrator-only user-management capability for Phase 01. The implementation extends the existing auth module with secure/idempotent superadmin bootstrap, user listing/search that includes assigned roles, role catalog exposure, and user creation with role assignment. The cabinet gains an admin-only Users section that uses the new contracts and keeps first-login password rotation out of scope.

## Technical Context

**Language/Version**: Kotlin + Spring Boot on Java 21; Vue 3 + Vite + TypeScript in `frontend/cabinet`  
**Primary Dependencies**: Spring Web/Security/OAuth2 resource server, Spring Data JPA, PostgreSQL/Flyway, PasswordEncoder, Vue Router, Pinia, axios client, vue-i18n  
**Storage**: PostgreSQL tables `app_user`, `app_role`, `app_user_role`, `auth_audit_event`; Flyway migration if schema/seed catalog changes are needed  
**Testing**: `make backend-test`, `make frontend-test`, `make frontend-build`, cross-cutting `make test && make build`  
**Target Platform**: Linux/Docker Compose local runtime; production-like Spring Boot API + Vue SPA deployment  
**Project Type**: Web application with API-only backend and SPA frontend  
**Performance Goals**: User search/list for the admin page returns up to 100 users per request within normal cabinet interaction expectations; create-user interaction completes in under 2 minutes manually  
**Constraints**: API must return explicit 401/403; no browser form login or Basic auth; passwords are never returned; production-like startup must fail when no ADMIN exists and secure bootstrap credentials are missing; local may keep documented `admin` convenience  
**Scale/Scope**: Minimal Phase 01 user administration: list/search/create only, no edit/delete/disable/reset/bulk import/IdP integration

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **ERP domain fit**: Strengthens employees/roles/access administration so operational staff can be onboarded into order, warehouse, and production workflows without manual SQL.
- **Constraint-aware operations**: No new TOC facts are introduced, but the feature preserves access to the existing operational facts by assigning correct roles to staff; it does not hardcode priority/flow behavior.
- **Architecture boundaries**: User creation, bootstrap policy, role validation, and permission checks belong in auth application use cases; controllers only adapt HTTP and persistence adapters only map/query data.
- **Traceability/audit**: User creation and bootstrap are privileged state changes and must record authentication/audit events without passwords in details.
- **API-only/security**: Backend remains API-only; user list/search/create and role catalog are JWT-protected with ADMIN-only mutating access and explicit 401/403 negative scenarios.
- **Docker/verifiability**: Root Docker startup must still work. Completion evidence: backend tests, frontend tests/build, and quickstart smoke check for superadmin login, user creation, and role-scoped login.
- **Exception handling**: No constitution violations identified.

## Project Structure

### Documentation (this feature)

```text
specs/013-user-admin-management/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── rest-users.md
│   └── frontend-users.md
└── tasks.md              # Generated later by /speckit-tasks
```

### Source Code (repository root)

```text
src/main/kotlin/com/ctfind/productioncontrol/auth/
├── domain/
│   ├── Role.kt
│   ├── UserAccount.kt
│   └── AuthenticationAuditEvent.kt
├── application/
│   ├── AuthenticationPorts.kt
│   ├── CreateUserUseCase.kt
│   ├── EnsureSuperadminUseCase.kt
│   ├── UserQueryUseCase.kt
│   └── RoleCatalogUseCase.kt
└── adapter/
    ├── web/
    │   ├── UserController.kt
    │   └── AuthDtos.kt
    └── persistence/
        ├── AuthJpaEntities.kt
        ├── AuthJpaRepositories.kt
        ├── AuthPersistenceAdapters.kt
        └── SuperadminSeedRunner.kt

src/main/resources/
├── application*.properties
└── db/migration/
    └── V<n>__ensure_auth_role_catalog.sql

src/test/kotlin/com/ctfind/productioncontrol/auth/
├── application/
└── adapter/

frontend/cabinet/src/
├── api/
│   ├── composables/use-users.ts
│   └── types/
├── components/layout/Sidebar.vue
├── pages/admin/UsersPage.vue
├── router/index.ts
└── i18n/

frontend/cabinet/tests/
└── unit/
```

**Structure Decision**: Keep all backend business behavior inside the existing `auth` module and add only adapter-level HTTP/persistence changes around it. Add a new `frontend/cabinet/src/pages/admin` area for admin-only cabinet screens while reusing existing API composable/router/sidebar patterns.

## Phase 0: Research Summary

See [research.md](./research.md). All planning unknowns are resolved: bootstrap behavior, role catalog source, list/search role visibility, password policy, audit expectation, and frontend routing approach are decided.

## Phase 1: Design Summary

See [data-model.md](./data-model.md), [contracts/rest-users.md](./contracts/rest-users.md), [contracts/frontend-users.md](./contracts/frontend-users.md), and [quickstart.md](./quickstart.md).

## Post-Design Constitution Check

- **ERP domain fit**: PASS — plan directly models role-based user onboarding for operational staff.
- **Constraint-aware operations**: PASS — feature does not alter production flow assumptions and enables correct role access to existing flow facts.
- **Architecture boundaries**: PASS — application use cases own validation/permissions; web/persistence remain adapters.
- **Traceability/audit**: PASS — design includes user-created and bootstrap audit events with password-free details.
- **API-only/security**: PASS — contracts use JSON API, JWT, 401/403, ADMIN-only user management.
- **Docker/verifiability**: PASS — quickstart includes Docker startup, health, admin login, creation, role-scoped login, and tests/build guidance.
- **Exception handling**: No exceptions.

## Complexity Tracking

No constitution violations or exceptional complexity.
