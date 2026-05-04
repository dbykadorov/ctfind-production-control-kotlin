# Contract: Backend Integration Test Command

## Required Command Behavior

The repository must provide a documented command for Docker-backed backend integration scenarios.

Recommended command name:

```bash
make backend-integration-test
```

Implemented command:

```bash
make backend-integration-test
```

Backend Gradle task:

```bash
./gradlew springIntegrationTest
```

## Command Expectations

- Runs only the Spring integration scenario suite.
- Starts or reuses isolated PostgreSQL-compatible test infrastructure through the test framework.
- Runs real Flyway migrations for the test database.
- Does not require the developer's local Docker Compose application stack to be running.
- Fails if Docker/Testcontainers infrastructure is unavailable.
- Does not replace `make backend-test` as the fast unit/slice test target unless the team explicitly decides to do so later.
- Uses `src/integrationTest/kotlin` and a dedicated `springIntegrationTest` Gradle source set.
- Isolates Spring contexts between scenario classes so cached contexts do not point at stopped Testcontainers databases.

## Verification Reporting

Release or implementation summaries should record:

- Command executed.
- Scenario classes included.
- Pass/fail result.
- Any skipped/deferred scenario and residual-risk reason.

## Optional Aggregate Target

The repository may also add:

```bash
make backend-check
```

to run both fast backend tests and integration scenarios in sequence.

Implemented optional aggregate target: `make backend-check`.
