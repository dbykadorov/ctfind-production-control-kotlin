# Implementation Plan: Orders API + Frontend Wiring

**Branch**: `004-orders-api-wiring` | **Date**: 2026-04-26 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/004-orders-api-wiring/spec.md`

## Summary

Implement the Phase 1 order foundation as a Spring/Kotlin modular-monolith slice and wire the existing Vue cabinet to it. The slice introduces customers, customer orders, order items, forward-only order status transitions, order status history, order change diffs, and audit events. It replaces the current frontend placeholder data with Spring API calls while preserving API-only JWT security, read-only order visibility for all authenticated Phase 1 roles, and write/status permissions for `ADMIN` and order managers.

## Technical Context

**Language/Version**: Kotlin 2.2.21, Java 21, TypeScript 5.7, Vue 3  
**Primary Dependencies**: Spring Boot 4.0.6, Spring Web MVC, Spring Security OAuth2 Resource Server, Spring Data JPA, Flyway, PostgreSQL driver, Jackson Kotlin module, Vue Router, Pinia, Axios, Vitest  
**Storage**: PostgreSQL via Flyway-managed schema migrations and JPA persistence adapters  
**Testing**: JUnit 5/Spring Boot test slices for backend domain/application/web/persistence behavior; Vitest/vue-tsc/Vite build for frontend composables and UI wiring  
**Target Platform**: Docker Compose local runtime with backend API, PostgreSQL, and frontend dev container  
**Project Type**: Web application with API-only backend and SPA frontend  
**Performance Goals**: Order search/filter results are usable within 15 seconds for up to 500 orders; order creation with at least one item completes in under 1 minute from the UI  
**Constraints**: No legacy Frappe runtime endpoints or realtime; backend remains stateless/JWT-protected; shipped orders are regular-edit read-only; only direct forward status transitions are allowed  
**Scale/Scope**: Phase 1 MVP for up to 50 users, several hundred orders, seeded local customers/orders, no production tasks/warehouse/notifications/realtime in this feature

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **ERP domain fit**: Pass. The feature strengthens the central ERP order workflow: customer selection, order creation, order editing, status lifecycle, order visibility, and dashboard signals.
- **Constraint-aware operations**: Pass. It preserves due dates, status timestamps, status history, order change diffs, and overdue facts needed for future lead time, throughput, and TOC buffer analysis. It avoids hardcoding a single future priority model by keeping sorting/filtering explicit and UI-driven.
- **Architecture boundaries**: Pass. Business rules belong in a new `orders` domain/application module: status transition policy, shipped-order editability, order number allocation, diff calculation, and permission decisions. Controllers only adapt HTTP requests; JPA entities only adapt persistence.
- **Traceability/audit**: Pass. Order creation, order edits, and status transitions produce audit/history records with actor, timestamp, changed fields, and before/after values for key business fields.
- **API-only/security**: Pass. The backend remains JWT-protected and API-only. Unauthenticated access returns 401, forbidden write actions return 403, and no browser auth challenge is introduced.
- **Docker/verifiability**: Pass. Existing root Docker workflow remains the local runtime. Completion requires backend tests, frontend tests/build, Docker startup, API smoke checks, and frontend manual smoke verification.
- **Exception handling**: No constitution exceptions identified.

## Project Structure

### Documentation (this feature)

```text
specs/004-orders-api-wiring/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── orders-api.md
│   └── frontend-orders.md
└── tasks.md
```

### Source Code (repository root)

```text
src/main/kotlin/com/ctfind/productioncontrol/
├── orders/
│   ├── domain/
│   │   ├── Customer.kt
│   │   ├── CustomerOrder.kt
│   │   ├── CustomerOrderItem.kt
│   │   ├── OrderStatus.kt
│   │   ├── OrderStatusChange.kt
│   │   └── OrderPolicies.kt
│   ├── application/
│   │   ├── OrderCommands.kt
│   │   ├── OrderQueries.kt
│   │   ├── OrderUseCases.kt
│   │   ├── OrderPorts.kt
│   │   └── OrderAuditService.kt
│   └── adapter/
│       ├── persistence/
│       │   ├── OrderJpaEntities.kt
│       │   ├── OrderJpaRepositories.kt
│       │   └── OrderPersistenceAdapters.kt
│       └── web/
│           ├── OrderController.kt
│           ├── CustomerController.kt
│           └── OrderDtos.kt
├── auth/
└── infrastructure/

src/main/resources/db/migration/
└── V4__create_order_tables.sql

src/test/kotlin/com/ctfind/productioncontrol/
└── orders/
    ├── domain/
    ├── application/
    └── adapter/

frontend/cabinet/src/api/
├── composables/use-orders.ts
├── composables/use-customers.ts
├── composables/use-dashboard-stats.ts
├── composables/use-trend-data.ts
├── composables/use-recent-activity.ts
└── types/

frontend/cabinet/tests/unit/
├── api/
├── composables/
├── pages/
└── router/
```

**Structure Decision**: Use the existing modular-monolith package style established by `auth`: a new `orders` module with `domain`, `application`, and `adapter` subpackages. Frontend changes stay inside the existing cabinet SPA composables/pages and continue using the Spring-only API client.

## Phase 0: Research

Research decisions are captured in [research.md](./research.md). Key outcomes:

- Represent order write permission with canonical backend role code `ORDER_MANAGER`, while preserving `ADMIN` as full access.
- Generate human-readable order numbers server-side using a database-backed sequence format.
- Store key business-field diffs now and keep the history payload extensible to full before/after snapshots later.
- Keep customer creation/editing out of this feature; only active existing/seeded customer selection is in scope.

## Phase 1: Design & Contracts

Design artifacts:

- [data-model.md](./data-model.md): entities, relationships, validation, lifecycle transitions, and audit/history model.
- [contracts/orders-api.md](./contracts/orders-api.md): backend HTTP contract for customers, orders, status transitions, errors, permissions, and dashboard data.
- [contracts/frontend-orders.md](./contracts/frontend-orders.md): frontend composable contracts and UI wiring expectations.
- [quickstart.md](./quickstart.md): local verification sequence for Docker, API smoke checks, and frontend user flows.

## Post-Design Constitution Check

- **ERP domain fit**: Pass. The data model and contracts keep customers, orders, items, status history, and audit as explicit operational concepts.
- **Constraint-aware operations**: Pass. Order due dates, status timestamps, overdue filters, status history, and changed business-field diffs are available for later throughput/lead-time/buffer analysis.
- **Architecture boundaries**: Pass. Contracts name controllers and DTOs as adapters; rules for status transitions, permissions, stale update prevention, and diff generation are assigned to domain/application services.
- **Traceability/audit**: Pass. Every create/update/status operation has an explicit audit/history contract.
- **API-only/security**: Pass. All order/customer endpoints are JWT-protected; contracts define 401/403 outcomes.
- **Docker/verifiability**: Pass. Quickstart includes backend tests, frontend tests/build, Docker startup, API smoke tests, and manual UI verification.
- **Exception handling**: No constitution exceptions.

## Complexity Tracking

No constitution violations or exceptional complexity are introduced by this plan.
