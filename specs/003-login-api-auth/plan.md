# Implementation Plan: Login API Authentication

**Branch**: `003-login-api-auth` | **Date**: 2026-04-26 | **Spec**: `specs/003-login-api-auth/spec.md`
**Input**: Feature specification from `specs/003-login-api-auth/spec.md`

## Summary

Implement the first app-owned authentication slice for the migrated cabinet: persist local users and roles in PostgreSQL, seed an idempotent local `admin` / `admin` account, expose API-only login/logout/me endpoints, issue 8-hour Bearer JWTs, record authentication audit events, throttle repeated failed attempts by login/IP, and wire the Vue cabinet login flow to the new Spring/Kotlin backend instead of the current placeholder.

The technical approach keeps authentication business rules in a dedicated application/domain module while REST controllers, Spring Security, JPA, Flyway, and frontend storage remain adapters around that core.

## Technical Context

**Language/Version**: Kotlin 2.2.21, Java 21, TypeScript 5.7, Vue 3
**Primary Dependencies**: Spring Boot 4.0.6, Spring Security, Spring Data JPA, Flyway, PostgreSQL, Bean Validation, Spring Security OAuth2 Resource Server/JOSE for JWT support, Vue Router, Pinia, axios, Vite
**Storage**: PostgreSQL via Flyway-managed schema; frontend JWT persisted in browser `localStorage` for this local MVP
**Testing**: JUnit 5/Spring Boot tests for backend unit, repository, MVC/security, and migration coverage; Vitest/vue-test-utils for cabinet auth service/store/router behavior; Docker Compose smoke checks
**Target Platform**: Local Docker Compose runtime on Linux-compatible containers; browser-based cabinet SPA at `/cabinet/*`; Spring API at `/api/*`
**Project Type**: Web application with Spring Boot API backend and Vue/Vite cabinet frontend
**Performance Goals**: Login and authenticated `/api/auth/me` calls respond comfortably within local interactive use; authentication checks add negligible overhead to Phase 1 cabinet navigation
**Constraints**: Backend must remain API-only with no browser form login or HTTP Basic challenge; JWT lifetime is 8 hours; local seed must be idempotent; raw passwords must never be logged or audited; root `docker compose up --build --wait` remains the primary verification path
**Scale/Scope**: One local bootstrap administrator, minimal role foundation for cabinet access, one frontend login flow, one authentication audit stream, and simple in-memory throttling suitable for local MVP rather than production lockout policy

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **ERP domain fit**: Strengthens the user, role, permission, and audit foundation needed before orders, production tasks, inventory, and internal notifications can be operated securely.
- **Constraint-aware operations**: Does not model production flow directly, but preserves accountable actor identity for future task ownership, bottleneck decisions, buffer adjustments, and audit-backed operational history.
- **Architecture boundaries**: Authentication decisions, credential verification, token issuing, throttling, seeding, and audit recording will live in domain/application services. Controllers, DTOs, JPA repositories, Spring Security filters, and frontend components remain adapters.
- **Traceability/audit**: Login success, login failure, logout, and seed activity are required audit events. Audit records identify outcome and context without storing submitted passwords or token values.
- **API-only/security**: Spring Security remains stateless and API-only. Login is an explicit JSON API; protected routes return `401`/`403`; browser-native login pages and Basic Auth challenges stay disabled.
- **Docker/verifiability**: Runtime docs and quickstart will verify fresh Docker startup, seeded login, failed login, throttling, logout, backend health, and frontend build/test checks.
- **Exception handling**: JWT in `localStorage` and in-memory throttling are accepted local-MVP constraints. They are documented as non-production security choices and do not require Complexity Tracking because they match the approved feature spec.

## Project Structure

### Documentation (this feature)

```text
specs/003-login-api-auth/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── auth-api.md
│   └── frontend-auth.md
└── tasks.md
```

### Source Code (repository root)

```text
src/main/kotlin/com/ctfind/productioncontrol/
├── CtfindProductionControlContlinApplication.kt
├── auth/
│   ├── domain/
│   │   ├── AuthenticationAuditEvent.kt
│   │   ├── LoginThrottlePolicy.kt
│   │   ├── Role.kt
│   │   └── UserAccount.kt
│   ├── application/
│   │   ├── AuthenticationAuditPort.kt
│   │   ├── AuthenticateUserUseCase.kt
│   │   ├── JwtTokenIssuer.kt
│   │   ├── LocalAdminSeedUseCase.kt
│   │   └── LogoutUseCase.kt
│   └── adapter/
│       ├── persistence/
│       └── web/
├── config/
│   └── SecurityConfig.kt
└── infrastructure/security/

src/main/resources/
├── application-local.properties
└── db/migration/

src/test/kotlin/com/ctfind/productioncontrol/
├── auth/
├── config/
└── CtfindProductionControlContlinApplicationTests.kt

frontend/cabinet/src/
├── api/
│   ├── auth-service.ts
│   └── frappe-client.ts
├── stores/auth.ts
└── router/index.ts

frontend/cabinet/tests/unit/
├── api/
├── pages/
├── router/
└── stores/
```

**Structure Decision**: Use the existing single Spring Boot backend project plus migrated `frontend/cabinet` SPA. Add a backend `auth` module under the root package with clean/hexagonal subpackages (`domain`, `application`, `adapter`) and keep Spring/JPA/security wiring outside the domain model.

## Complexity Tracking

No constitution violations requiring complexity exceptions.

## Phase 0: Research

Research completed in `specs/003-login-api-auth/research.md`.

Key decisions:

- Use Spring Security Resource Server/JOSE primitives for JWT issuing and validation, with a local HMAC secret configured for the local profile.
- Store credentials as BCrypt password hashes and seed only when the local admin login is absent.
- Use Flyway migrations for user, role, assignment, and authentication audit tables.
- Implement simple in-memory throttle buckets keyed by normalized login and request IP for this local MVP.
- Replace the frontend placeholder with explicit `/api/auth/*` calls and a single localStorage token boundary.

## Phase 1: Design & Contracts

Design artifacts:

- Data model: `specs/003-login-api-auth/data-model.md`
- API contract: `specs/003-login-api-auth/contracts/auth-api.md`
- Frontend contract: `specs/003-login-api-auth/contracts/frontend-auth.md`
- Quickstart: `specs/003-login-api-auth/quickstart.md`

## Phase 1 Constitution Re-Check

- **ERP domain fit**: PASS. User, role, credential, and audit structures become the foundation for Phase 1 ERP access control.
- **Constraint-aware operations**: PASS. Authenticated actor identity and audit timestamps support future operational traceability.
- **Architecture boundaries**: PASS. Planned modules keep rules in domain/application services and adapters at the edges.
- **Traceability/audit**: PASS. Audit events are explicit data-model and contract concerns.
- **API-only/security**: PASS. Contracts specify JSON APIs, Bearer tokens, stateless security, and `401`/`403` outcomes.
- **Docker/verifiability**: PASS. Quickstart includes Docker startup, API smoke checks, frontend route checks, and test/build commands.

## Phase 2: Task Planning Notes

`/speckit-tasks` should generate tasks grouped by independently testable stories:

1. Seeded admin can sign in and reach a protected cabinet page.
2. Protected cabinet routes and backend routes honor Bearer authentication.
3. Failed login, logout, audit, and throttling behave safely and traceably.

Each story should include backend tests, frontend tests, and at least one Docker/local smoke verification task where applicable.
