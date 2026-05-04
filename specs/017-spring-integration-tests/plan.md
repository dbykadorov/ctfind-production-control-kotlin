# Implementation Plan: Phase 1 Spring Integration Scenarios

**Branch**: `017-spring-integration-tests` | **Date**: 2026-05-03 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/017-spring-integration-tests/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Add a small Spring integration scenario suite for Phase 1 backend flows. The suite will verify real application wiring across HTTP request handling, Spring Security, controllers, application use cases, JPA persistence, Flyway migrations, and PostgreSQL-compatible behavior. MVP contains three scenario classes: Auth + Users Security, Production Task Lifecycle, and Warehouse/BOM/Consumption. The remaining three scenarios cover Order Lifecycle, Notifications, and Audit Feed.

## Technical Context

**Language/Version**: Kotlin 2.2.21, Java 21, Spring Boot 4.0.6  
**Primary Dependencies**: Spring Boot Web MVC/Security/Data JPA/Flyway test starters, JUnit 5, MockMvc, PostgreSQL JDBC, new Testcontainers PostgreSQL dependencies for integration tests  
**Storage**: PostgreSQL-compatible isolated test database with real Flyway migrations  
**Testing**: Existing `make backend-test` remains the fast unit/slice test target; add a documented Docker-backed backend integration test target for scenario tests  
**Target Platform**: Backend JVM test runtime under `production-control-api`; local developer machines and CI agents with Docker available  
**Project Type**: Backend web service integration test feature in a modular monolith  
**Performance Goals**: No more than 6 scenario classes; MVP 3 classes; integration target should stay small enough for release verification rather than every edit loop  
**Constraints**: Do not duplicate unit-test matrices; use normal login flow for authenticated behavior; use real security filters, controllers, persistence adapters, migrations, and PostgreSQL-compatible database; keep frontend untouched except documentation if needed  
**Scale/Scope**: Phase 1 backend modules: auth, orders, production, inventory, notifications, audit

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **ERP domain fit**: PASS. Scenarios validate coherent Phase 1 ERP/control workflows across users, roles, orders, production tasks, inventory, notifications, and audit.
- **Constraint-aware operations**: PASS. Scenarios preserve and verify task lifecycle/history, status pressure, overdue signals, inventory consumption facts, and audit trails needed for later TOC work.
- **Architecture boundaries**: PASS. Tests exercise public application paths and do not move business rules into controllers or DTOs.
- **Traceability/audit**: PASS. Production, inventory, notification, and audit scenarios verify state-change traces where applicable.
- **API-only/security**: PASS. The suite explicitly validates authenticated, unauthenticated, admin, non-admin, writer, executor, supervisor, and warehouse access behavior via API paths.
- **Docker/verifiability**: PASS. Integration suite will have a documented Docker-backed command and fresh evidence. Existing root Docker runtime remains unchanged unless Makefile targets are added.
- **Exception handling**: No constitution violations identified.

## Project Structure

### Documentation (this feature)

```text
specs/017-spring-integration-tests/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── integration-scenarios.contract.md
│   └── backend-integration-command.contract.md
└── tasks.md
```

### Source Code (repository root)

```text
production-control-api/
├── build.gradle.kts
├── src/main/kotlin/com/ctfind/productioncontrol/
├── src/main/resources/db/migration/
├── src/test/kotlin/com/ctfind/productioncontrol/
└── src/integrationTest/kotlin/com/ctfind/productioncontrol/integration/
    ├── IntegrationTestSupport.kt
    ├── AuthUsersSecurityIntegrationTest.kt
    ├── ProductionTaskLifecycleIntegrationTest.kt
    ├── WarehouseConsumptionIntegrationTest.kt
    ├── OrderLifecycleIntegrationTest.kt
    ├── NotificationsIntegrationTest.kt
    └── AuditFeedIntegrationTest.kt

Makefile
AGENTS.md
```

**Structure Decision**: Use a dedicated Gradle `integrationTest` source set under `production-control-api/src/integrationTest/kotlin` so Docker-backed scenario tests do not slow or destabilize the existing fast `test` task. Shared test helpers live only in the integration test source set.

## Complexity Tracking

No constitution violations or complexity exceptions.

## Phase 0: Research

Research is complete in [research.md](./research.md). Key decisions:

- Use a dedicated `integrationTest` source set and explicit Make target instead of folding Docker-backed tests into the normal unit loop.
- Use Spring Boot integration testing with MockMvc and real security filters.
- Use Testcontainers PostgreSQL so Flyway/JPA behavior matches production database semantics.
- Authenticate through the real login endpoint for scenario actors.
- Keep six scenario classes maximum and avoid detailed business-rule permutations already covered by unit tests.

## Phase 1: Design & Contracts

Design artifacts generated:

- [data-model.md](./data-model.md) defines integration scenario, actor, fixture, evidence, and residual-risk documentation entities.
- [contracts/integration-scenarios.contract.md](./contracts/integration-scenarios.contract.md) defines the six scenario contracts and MVP boundary.
- [contracts/backend-integration-command.contract.md](./contracts/backend-integration-command.contract.md) defines the expected verification command contract.
- [quickstart.md](./quickstart.md) defines setup and validation steps for the integration suite.

## Post-Design Constitution Check

- **ERP domain fit**: PASS. The contracts map directly to Phase 1 operational workflows.
- **Constraint-aware operations**: PASS. Production, notification, audit, and inventory scenarios verify TOC-relevant facts survive real application wiring.
- **Architecture boundaries**: PASS. Scenario tests exercise existing application boundaries rather than changing domain placement.
- **Traceability/audit**: PASS. Audit and history/notification assertions are required where state changes occur.
- **API-only/security**: PASS. The command and scenario contracts require real API authentication paths and explicit 401/403 checks.
- **Docker/verifiability**: PASS. A separate documented command isolates Docker-backed verification while keeping standard fast tests available.
- **Exception handling**: No exceptions.
