# Research: Phase 1 Spring Integration Scenarios

## Decision: Use a Dedicated Integration Test Source Set

**Decision**: Add a dedicated backend integration test source set, conventionally `src/integrationTest/kotlin`, with a separate Gradle task and root Make target.

**Rationale**: These tests require Docker-backed PostgreSQL and full Spring wiring, so they should not make the existing fast `make backend-test` loop depend on Docker. A separate target keeps developer feedback fast while making release verification explicit.

**Alternatives considered**:

- Put scenarios in the existing `test` source set: rejected because it would make normal unit test runs slower and Docker-dependent.
- Use only existing unit/slice tests: rejected because the feature goal is to catch wiring, security, migration, and JPA integration issues.

## Decision: Use Spring Boot + MockMvc for HTTP-Level Scenarios

**Decision**: Use Spring Boot application context with MockMvc-style HTTP requests and real security filters.

**Rationale**: The scenarios need to exercise controllers, JSON binding, validation, security, use cases, persistence, and response mapping without starting the full external Docker Compose stack.

**Alternatives considered**:

- Direct controller calls: rejected because they skip routing, filters, validation, and real request/response serialization.
- Full external server on a random port: acceptable but heavier than needed for these backend-only scenarios.

## Decision: Use Testcontainers PostgreSQL

**Decision**: Use PostgreSQL Testcontainers for integration scenarios.

**Rationale**: The project uses PostgreSQL and Flyway. Testcontainers keeps migrations, SQL behavior, locking, constraints, and JPA mappings close to runtime behavior.

**Alternatives considered**:

- In-memory H2: rejected because it can hide PostgreSQL-specific schema/query behavior.
- Existing Docker Compose database: rejected because tests need isolated deterministic data and should not depend on developer stack state.

## Decision: Authenticate Through the Real Login Flow

**Decision**: Scenario actors should obtain tokens through the same login endpoint as clients.

**Rationale**: Login, token creation, role claims, and security filters are part of the wiring risk being tested.

**Alternatives considered**:

- Mock security context: rejected for the main scenarios because it bypasses important auth wiring. It may be used only for narrowly documented setup shortcuts.
- Seed static bearer tokens: rejected because token generation and claims would not be validated.

## Decision: Keep Scenario Count and Assertions Small

**Decision**: Implement at most six scenario classes for Phase 1, with MVP limited to Auth + Users, Production Task Lifecycle, and Warehouse/BOM/Consumption. Each scenario includes one happy path and 2-4 high-value negative/security/conflict checks.

**Rationale**: Existing unit tests already cover business-rule matrices. Integration tests should verify cross-layer wiring, not duplicate all combinations.

**Alternatives considered**:

- One integration class per controller: rejected because it duplicates adapter tests and does not express business flows.
- Full e2e matrix for every role and status: rejected because it would become slow and brittle.

## Decision: Document Residual Risks Explicitly

**Decision**: Each scenario should document what it covers and what remains covered by unit tests or manual smoke checks.

**Rationale**: The suite is intentionally minimal; reviewers need a quick view of coverage boundaries.

**Alternatives considered**:

- Rely only on test names: rejected because names rarely explain omitted flows or deferred categories.
- Add all omitted behavior into integration tests: rejected because it conflicts with the small-suite goal.
