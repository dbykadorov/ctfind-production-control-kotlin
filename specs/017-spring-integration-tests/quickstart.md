# Quickstart: Phase 1 Spring Integration Scenarios

## 1. Confirm Current Feature

```bash
cat .specify/feature.json
```

Expected: `specs/017-spring-integration-tests`.

## 2. Run Fast Backend Tests

```bash
make backend-test
```

Expected: existing unit/slice/backend tests pass without requiring Docker-backed integration scenarios.

## 3. Run Integration Scenarios

```bash
make backend-integration-test
```

Expected: Docker-backed Spring integration scenarios run against isolated PostgreSQL-compatible test infrastructure.

Implementation command contract:

- Root command: `make backend-integration-test`
- Backend Gradle task: `springIntegrationTest`
- Source set: `production-control-api/src/integrationTest/kotlin`
- Database: Testcontainers PostgreSQL with real Flyway migrations
- Compile checkpoint: `./gradlew springIntegrationTestClasses`

## 4. Validate MVP Scenario Coverage

Confirm the integration suite includes:

- `AuthUsersSecurityIntegrationTest`
- `ProductionTaskLifecycleIntegrationTest`
- `WarehouseConsumptionIntegrationTest`

Each MVP scenario must include a happy path plus at least two negative/security/conflict checks.

## 5. Validate Full Scenario Coverage

Confirm the planned full suite includes no more than six scenario classes or equivalent scenario groups:

- `AuthUsersSecurityIntegrationTest`
- `ProductionTaskLifecycleIntegrationTest`
- `WarehouseConsumptionIntegrationTest`
- `OrderLifecycleIntegrationTest`
- `NotificationsIntegrationTest`
- `AuditFeedIntegrationTest`

## 6. Validate Residual Risk Notes

Review scenario documentation or implementation summary and confirm:

- Unit tests remain responsible for detailed business-rule permutations.
- Integration scenarios document omitted coverage.
- Skipped/deferred categories have explicit residual-risk notes.

## 7. Verification Record

After implementation, record:

- Date: 2026-05-03.
- `make backend-test`: PASS.
- `./gradlew springIntegrationTest`: PASS, executed all six scenario classes.
- `make backend-integration-test`: PASS outside sandbox; sandbox run failed before test execution because Gradle could not determine a usable wildcard IP for file-lock services.
- Scenario classes executed:
  - `AuthUsersSecurityIntegrationTest`
  - `ProductionTaskLifecycleIntegrationTest`
  - `WarehouseConsumptionIntegrationTest`
  - `OrderLifecycleIntegrationTest`
  - `NotificationsIntegrationTest`
  - `AuditFeedIntegrationTest`
- Skipped/deferred scenarios: none.
- Residual-risk notes:
  - Detailed business-rule matrices remain in unit/application tests.
  - Frontend browser smoke coverage remains outside this backend integration suite.
  - Integration tests use one representative happy path and focused negative/security/conflict checks per scenario.
