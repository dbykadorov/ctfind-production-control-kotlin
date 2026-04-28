# Implementation Plan: Журнал действий администратора (Phase 1 §8 #10)

**Branch**: `007-audit-log-viewer` | **Date**: 2026-04-28 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/007-audit-log-viewer/spec.md`

## Summary

Unified admin-only audit log page that reads from three existing audit tables (`auth_audit_event`, `order_audit_event`, `production_task_audit_event`) and presents a paginated, filterable feed. Backend: new `audit` hexagonal module with read-only access to audit tables via `GET /api/audit` (ADMIN-only), plus `GET /api/users` in the auth module for the actor picker. Frontend: new `AuditLogPage.vue` at `/cabinet/audit` with filter panel (dates, categories, actor, search), table, pagination, and target-object links. Closes Phase 1 §8 #10 — the last remaining Phase 1 completion criterion.

## Technical Context

**Language/Version**: Kotlin 1.9+ / Java 21 (backend), TypeScript 5.7 strict (frontend)
**Primary Dependencies**: Spring Boot 3.x, Spring Security (JWT/OAuth2 Resource Server), JPA/Hibernate (backend); Vue 3.5, vue-router, Pinia, Tailwind, vue-i18n, axios, date-fns (frontend)
**Storage**: PostgreSQL 15+ — read-only access to existing audit tables; no new migrations
**Testing**: JUnit 5 + Spring Boot Test + MockMvc (backend); Vitest + vue-test-utils (frontend)
**Target Platform**: Docker Compose (postgres + app + frontend); browser (desktop + 10-12" tablet landscape)
**Project Type**: Web application (modular monolith backend + SPA frontend)
**Performance Goals**: 10K events in DB for chosen interval renders page in seconds (SC-005); pagination 50/page; ≤30s to find event (SC-001)
**Constraints**: ADMIN-only access; read-only over audit tables; no new Flyway migrations; consistent with hexagonal module pattern
**Scale/Scope**: 3 source tables, 1 new backend module (~12 source files), 1 auth module extension (~4 files), 1 new frontend page + composable + component (~8 source files), ~10 test files

## Constitution Check

*GATE: Pre-design check — PASS. Post-design re-check below.*

- **ERP domain fit**: Closes §8 #10 "просматривать журнал действий" — the last Phase 1 completion criterion. Gives ADMIN cross-entity audit visibility over auth, orders, and production tasks.
- **Constraint-aware operations**: Read-only feature. Does not introduce new facts, state transitions, or priority models. All existing audit-event streams remain untouched.
- **Architecture boundaries**: New `audit` module follows hexagonal layout (domain / application / adapter). Domain contains `AuditCategory` enum and `AuditLogRow` view model — pure Kotlin, no Spring. Application contains `AuditLogQueryUseCase` (ADMIN permission check) and `AuditLogQueryPort`. Adapter/web contains controller + DTOs. Adapter/persistence contains read-only JPA entities and the port implementation. No business rules in controllers.
- **Traceability/audit**: This feature IS the audit UI. It reads existing audit data, does not write. No new auditable mutations introduced. Existing write paths in auth/orders/production remain untouched.
- **API-only/security**: Backend returns 403 for non-ADMIN, 401 for unauthenticated. No browser form login. Frontend route guard restricts to ADMIN. No new auth/role mechanism — uses existing JWT + role-code check pattern.
- **Docker/verifiability**: No infrastructure changes. Docker stack unchanged. Verification: backend unit tests, frontend tests + typecheck + build, docker health, API smoke (admin 200, executor 403), manual smoke, legacy runtime guard.
- **Exception handling**: No constitution violations. No complexity exceptions needed.

### Post-Design Re-Check

- **Cross-module data access**: Audit module reads from three audit tables via its own read-only JPA entities (R-003 in research.md). This follows the `ProductionOrderSourcePort` pattern — the audit module does not import from `auth.*`, `orders.*`, or `production.*` application/domain packages. The audit tables are cross-cutting infrastructure. ✓ Justified in research.md.
- **Auth module extension**: `GET /api/users` added to auth module — user lookup is an auth concern. Follows existing `ProductionTaskAssigneeQueryUseCase` pattern. ✓ Consistent with architecture.
- **No new Flyway migration**: Read-only JPA entities map to existing columns. `ddl-auto=validate` ensures no drift. ✓

## Project Structure

### Documentation (this feature)

```text
specs/007-audit-log-viewer/
├── plan.md                           # This file
├── spec.md                           # Feature specification
├── research.md                       # Research decisions (R-001 through R-009)
├── data-model.md                     # Entity mapping and normalization
├── quickstart.md                     # Verification flow
├── contracts/
│   ├── backend-audit-api.md          # GET /api/audit + GET /api/users
│   └── frontend-audit-log.md         # Composable, page, types, tests
├── checklists/
│   └── requirements.md               # Specification quality checklist
└── tasks.md                          # Task list (/speckit-tasks output)
```

### Source Code (repository root)

```text
# Backend — new audit module
src/main/kotlin/com/ctfind/productioncontrol/audit/
├── domain/
│   └── AuditLogModels.kt             # AuditCategory enum, AuditLogRow view model
├── application/
│   ├── AuditLogQueryUseCase.kt       # ADMIN check + delegates to port
│   ├── AuditLogModels.kt             # AuditLogQuery, AuditLogPageResult
│   └── AuditLogPorts.kt              # AuditLogQueryPort interface
└── adapter/
    ├── web/
    │   ├── AuditController.kt        # GET /api/audit, JWT principal, DTO mapping
    │   └── AuditDtos.kt              # AuditLogPageResponse, AuditLogRowResponse
    └── persistence/
        ├── AuditJpaEntities.kt       # Read-only entities for 3 audit tables
        ├── AuditJpaRepositories.kt   # JPA repos with date-range queries
        └── AuditPersistenceAdapter.kt # Merges 3 sources, filters, sorts, paginates

# Backend — auth module extension (user lookup for actor picker)
src/main/kotlin/com/ctfind/productioncontrol/auth/
├── application/
│   ├── UserQueryUseCase.kt           # ADMIN-only user search
│   └── AuthenticationPorts.kt        # + UserQueryPort
└── adapter/
    ├── web/
    │   ├── UserController.kt         # GET /api/users
    │   └── AuthDtos.kt               # + UserSummaryResponse
    └── persistence/
        └── AuthPersistenceAdapters.kt # + UserQueryPort implementation

# Backend tests
src/test/kotlin/com/ctfind/productioncontrol/audit/
├── application/
│   └── AuditLogQueryUseCaseTests.kt
└── adapter/
    ├── web/
    │   └── AuditControllerTests.kt
    └── persistence/
        └── AuditPersistenceAdapterTests.kt

# Frontend — new page + composable + component
frontend/cabinet/src/
├── api/
│   ├── composables/
│   │   ├── use-audit-log.ts          # Fetch audit log with filters
│   │   └── use-users-search.ts       # Fetch users for actor picker
│   └── types/
│       └── audit-log.ts              # TS types for audit API
├── components/domain/
│   └── AuditActorPicker.vue          # Actor picker component
├── pages/audit/
│   └── AuditLogPage.vue              # Main audit log page
├── i18n/
│   ├── ru.ts                         # + audit i18n keys
│   └── en.ts                         # + audit i18n keys
├── router/
│   └── index.ts                      # + audit route
└── components/layout/
    └── Sidebar.vue                   # + audit nav entry (ADMIN-only)

# Frontend tests
frontend/cabinet/tests/unit/
├── composables/
│   └── use-audit-log.test.ts
├── pages/
│   ├── AuditLogPage.test.ts
│   ├── AuditLogPageFilters.test.ts
│   └── AuditLogPageForbidden.test.ts
├── components/
│   └── AuditActorPicker.test.ts
└── router/
    └── guard.test.ts                 # + audit route cases
```

**Structure Decision**: New backend `audit` module follows the hexagonal layout established by `auth`, `orders`, and `production`. Cross-module access to audit tables uses read-only JPA entities in the audit adapter — audit tables are cross-cutting infrastructure, not owned by a single module (R-003). Auth module extended with user lookup endpoint (R-007). Frontend follows existing page + composable + types pattern (R-009).

## Complexity Tracking

No constitution violations. No complexity exceptions needed.
