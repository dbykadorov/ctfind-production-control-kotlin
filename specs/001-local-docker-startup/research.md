# Research: Local Container Startup

## Decision: Use Docker Compose-compatible local orchestration

**Rationale**: The feature requires one-command startup, predictable stop/start, local dependency
orchestration, logs, and readiness checks. A Compose file is the most direct fit for a single backend
service plus PostgreSQL and is familiar to most developers.

**Alternatives considered**:

- Manual host setup: rejected because it requires installing and configuring the database and Java
  runtime directly on every developer machine.
- Full Kubernetes stack: rejected as unnecessary complexity for Phase 1 local development.
- Buildpacks-only local run: rejected because it does not by itself describe the database and
  developer stop/log workflow.

## Decision: Build the application image with a multi-stage JVM container build

**Rationale**: The host currently has no Java runtime available, and the local startup feature should
avoid requiring developers to install all application runtime dependencies directly. A multi-stage
build can use the Gradle wrapper inside a builder stage and copy only the packaged application into a
runtime stage.

**Alternatives considered**:

- Host `./gradlew bootRun`: rejected as the primary path because it depends on a host JDK and does
  not satisfy containerized local startup.
- Prebuilt artifact checked into source control: rejected because build outputs should not be
  versioned.
- Single-stage image with build tools in runtime: rejected because it creates a larger runtime image
  and mixes build and run responsibilities.

## Decision: Use PostgreSQL as the required local database

**Rationale**: The project already includes Spring Data JPA, Flyway, and the PostgreSQL driver.
PostgreSQL should therefore be the local database from the first operational slice so later Phase 1
data work does not need to migrate away from an in-memory substitute.

**Alternatives considered**:

- H2 or another embedded database: rejected because behavior can diverge from PostgreSQL and hide
  migration or SQL compatibility issues.
- External shared database: rejected because it violates local isolation and makes new developer
  setup dependent on shared infrastructure.

## Decision: Use development-only credentials and configuration

**Rationale**: The feature explicitly forbids production secrets and production service access. Local
credentials should be predictable, documented, and safe because they only apply to local containers.

**Alternatives considered**:

- Require `.env` with secret-like values: rejected for the initial slice because it adds setup steps
  without improving local safety.
- Reuse production-like secret management: rejected as out of scope for local Phase 1 development.

## Decision: Use Actuator health as the readiness signal

**Rationale**: The project already includes Actuator, and developers need a clear ready/not-ready
check. Health is suitable for local diagnostics and can include database readiness once the
application connects to PostgreSQL.

**Alternatives considered**:

- Check the root page: rejected because there is no user-facing UI yet and security may protect
  application routes.
- Check container status only: rejected because a running process does not prove application
  readiness.

## Decision: Keep local state in a named container volume

**Rationale**: Developers need repeatable stop/start without manual cleanup, while local database
state must not enter version control. A named local volume preserves data across normal restarts and
can be explicitly removed when a reset is needed.

**Alternatives considered**:

- Bind-mount database files into the repository: rejected because it risks polluting the working
  tree.
- Delete state on every stop: rejected because it makes local development inconvenient and reduces
  repeatability for manual testing.
