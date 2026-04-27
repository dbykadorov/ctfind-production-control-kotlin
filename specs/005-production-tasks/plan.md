# Implementation Plan: Production Tasks

**Branch**: `005-production-tasks` | **Date**: 2026-04-27 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/005-production-tasks/spec.md`

## Summary

Implement the Phase 1 production execution slice as a new Spring/Kotlin modular-monolith module and wire the existing Vue cabinet to it. The slice introduces production tasks linked to customer orders/order items, manual multiple tasks per order item with distinct purpose, assignment to exactly one executor, supervisor/executor status workflows, task history/audit, list/detail/filter screens, and frontend workflow buttons. It preserves API-only JWT security, existing order workflows, optimistic update protection, and TOC-ready production flow facts.

## Technical Context

**Language/Version**: Kotlin 2.2.21, Java 21, TypeScript 5.7, Vue 3  
**Primary Dependencies**: Spring Boot 4.0.6, Spring Web MVC, Spring Security OAuth2 Resource Server, Spring Data JPA, Flyway, PostgreSQL driver, Jackson Kotlin module, Vue Router, Pinia, Axios, Vitest  
**Storage**: PostgreSQL via Flyway-managed schema migrations and JPA persistence adapters  
**Testing**: JUnit 5/Spring Boot test slices for backend domain/application/web/persistence behavior; Vitest/vue-tsc/Vite build for frontend composables and UI wiring  
**Target Platform**: Docker Compose local runtime with backend API, PostgreSQL, and frontend dev container  
**Project Type**: Web application with API-only backend and SPA frontend  
**Performance Goals**: Task list search/filter results are usable within 2 seconds for the Phase 1 expected dataset; authorized users can create tasks for a multi-item order in under 2 minutes  
**Constraints**: No legacy Frappe runtime endpoints or realtime; backend remains stateless/JWT-protected; tasks link to existing orders/items; assignment is exactly one executor; work areas, routing, capacity scheduling, inventory reservation, and material availability remain out of scope  
**Scale/Scope**: Phase 1 MVP for up to 50 users, several hundred orders, and production task counts proportional to order items; no standalone tasks, team assignment, custom workflows, warehouse automation, or TOC buffer board in this feature

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **ERP domain fit**: Pass. The feature strengthens the order-to-production workflow by turning customer order demand into executable production tasks with assignment, status, due dates, and history.
- **Constraint-aware operations**: Pass. It preserves status history, blocked reasons, executor context, planned dates, start/finish timestamps, and WIP facts needed for future waiting-time, throughput, bottleneck, and buffer analysis. It intentionally defers work-area modeling while keeping the domain extensible.
- **Architecture boundaries**: Pass. Business rules belong in a new `production` domain/application module: task lifecycle policy, assignment/visibility permissions, duplicate-purpose rules, stale update prevention, and audit/history decisions. Controllers only adapt HTTP requests; JPA entities only adapt persistence.
- **Traceability/audit**: Pass. Task creation, assignment changes, planning changes, status changes, block/unblock events, and completion produce task history and audit records with actor, timestamp, and business context.
- **API-only/security**: Pass. The backend remains JWT-protected and API-only. Unauthenticated access returns 401, forbidden actions return 403, and production-specific roles are explicit in contracts and tests.
- **Docker/verifiability**: Pass. Existing root Docker workflow remains the local runtime. Completion requires backend tests, frontend tests/build, Docker startup, API smoke checks, frontend manual smoke verification, and legacy runtime guard checks.
- **Exception handling**: No constitution exceptions identified.

## Project Structure

### Documentation (this feature)

```text
specs/005-production-tasks/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── production-tasks-api.md
│   └── frontend-production-tasks.md
└── tasks.md
```

### Source Code (repository root)

```text
src/main/kotlin/com/ctfind/productioncontrol/
├── production/
│   ├── domain/
│   │   ├── ProductionTask.kt
│   │   ├── ProductionTaskStatus.kt
│   │   ├── ProductionTaskHistoryEvent.kt
│   │   └── ProductionTaskPolicies.kt
│   ├── application/
│   │   ├── ProductionTaskCommands.kt
│   │   ├── ProductionTaskQueries.kt
│   │   ├── ProductionTaskUseCases.kt
│   │   ├── ProductionTaskPorts.kt
│   │   ├── ProductionTaskPermissions.kt
│   │   └── ProductionTaskAuditService.kt
│   └── adapter/
│       ├── persistence/
│       │   ├── ProductionTaskJpaEntities.kt
│       │   ├── ProductionTaskJpaRepositories.kt
│       │   └── ProductionTaskPersistenceAdapters.kt
│       └── web/
│           ├── ProductionTaskController.kt
│           └── ProductionTaskDtos.kt
├── orders/
├── auth/
└── infrastructure/

src/main/resources/db/migration/
└── V5__create_production_task_tables.sql

src/test/kotlin/com/ctfind/productioncontrol/
└── production/
    ├── domain/
    ├── application/
    └── adapter/

frontend/cabinet/src/api/
├── composables/use-production-tasks.ts
├── composables/use-production-task-detail.ts
├── composables/use-production-task-workflow.ts
└── types/production-tasks.ts

frontend/cabinet/src/pages/production/
├── ProductionTasksListPage.vue
└── ProductionTaskDetailPage.vue

frontend/cabinet/tests/unit/
├── composables/
├── pages/
└── router/
```

**Structure Decision**: Add a new `production` backend module parallel to `orders` with `domain`, `application`, and `adapter` subpackages. The module consumes order context through application/persistence ports rather than putting task rules into the `orders` module. Frontend changes stay inside the existing cabinet SPA and continue using the Spring-only API client.

## Phase 0: Research

Research decisions are captured in [research.md](./research.md). Key outcomes:

- Use canonical backend role codes `PRODUCTION_SUPERVISOR` and `PRODUCTION_EXECUTOR`, while preserving `ADMIN` as full access and `ORDER_MANAGER` for task creation/planning.
- Model production task lifecycle as `NOT_STARTED -> IN_PROGRESS -> COMPLETED`, with `BLOCKED` as an interrupt state that stores and returns to the previous active status.
- Allow multiple tasks per order item only when each task has a distinct user-visible purpose.
- Assign each task to exactly one executor in Feature 005; defer work areas, teams, routings, and capacity scheduling.
- Store task history as structured business events and audit records to support both user timelines and future TOC analysis.

## Phase 1: Design & Contracts

Design artifacts:

- [data-model.md](./data-model.md): entities, relationships, validation, lifecycle transitions, visibility rules, and audit/history model.
- [contracts/production-tasks-api.md](./contracts/production-tasks-api.md): backend HTTP contract for task list/detail/create-from-order/assign/status/history/errors/permissions.
- [contracts/frontend-production-tasks.md](./contracts/frontend-production-tasks.md): frontend composable, route, permission, and UI wiring expectations.
- [quickstart.md](./quickstart.md): local verification sequence for Docker, API smoke checks, and frontend user flows.

## Post-Design Constitution Check

- **ERP domain fit**: Pass. The data model and contracts keep production tasks, order/item links, assignment, statuses, due dates, and history as explicit ERP concepts.
- **Constraint-aware operations**: Pass. Task status timestamps, blocked reasons, executor assignment, planned dates, and WIP query fields are available for later throughput, lead-time, bottleneck, and buffer analysis. Work-area fields are deferred without closing the model to future extension.
- **Architecture boundaries**: Pass. Contracts name controllers and DTOs as adapters; task lifecycle, permission, duplicate-purpose, assignment, stale update, and audit rules are assigned to domain/application services.
- **Traceability/audit**: Pass. Every create/assign/plan/status/block/unblock/complete operation has an explicit audit/history contract.
- **API-only/security**: Pass. All production task endpoints are JWT-protected; contracts define 401/403 outcomes and executor assigned-only visibility/update behavior.
- **Docker/verifiability**: Pass. Quickstart includes backend tests, frontend tests/build, Docker startup, API smoke tests, manual UI verification, and legacy runtime guard.
- **Exception handling**: No constitution exceptions.

## Complexity Tracking

No constitution violations or exceptional complexity are introduced by this plan.
