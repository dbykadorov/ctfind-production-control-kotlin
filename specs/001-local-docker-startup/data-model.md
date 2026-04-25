# Data Model: Local Container Startup

This feature does not introduce business-domain entities. The model describes the local runtime
objects that must exist for a reproducible developer startup.

## Entity: Local Environment

Represents the complete developer runtime started from the repository root.

### Fields

- `name`: stable environment name used in documentation and container labels.
- `services`: application and mandatory local dependencies.
- `network`: private local network shared by services.
- `volumes`: local persistent state used by dependencies.
- `startCommand`: documented command for starting the environment.
- `stopCommand`: documented command for stopping the environment.
- `logCommand`: documented command for reading logs.
- `resetCommand`: optional command for deleting local runtime state when a developer wants a clean
  database.

### Validation Rules

- Must be startable from repository root with one primary command.
- Must not require production secrets.
- Must keep persistent runtime state outside the source tree.
- Must expose a documented readiness check.

## Entity: Application Service

Represents the backend application process in the local environment.

### Fields

- `containerName`: stable local service identifier.
- `imageBuildContext`: repository root.
- `runtimeProfile`: development/local profile.
- `httpPort`: host port used by developers to access the application.
- `healthUrl`: readiness endpoint documented for local checks.
- `dependsOn`: mandatory local dependencies required before the app can become ready.

### Validation Rules

- Must connect to the local database dependency, not an external or production database.
- Must expose an HTTP endpoint reachable from the host.
- Must fail loudly if mandatory configuration is missing.
- Must become healthy only after the application can start with its required local dependencies.

## Entity: Database Service

Represents the local PostgreSQL dependency.

### Fields

- `containerName`: stable local service identifier.
- `databaseName`: local development database name.
- `username`: local development username.
- `password`: local development password.
- `port`: optional host port for developer diagnostics.
- `volume`: named local volume for database state.
- `healthCheck`: database readiness command.

### Validation Rules

- Must be created automatically during local startup.
- Must use development-only credentials.
- Must not store data files in tracked repository paths.
- Must preserve state across ordinary stop/start cycles.

## Entity: Readiness Check

Represents a developer-facing ready/not-ready signal.

### Fields

- `url`: local endpoint used for application readiness.
- `expectedHealthyResult`: response indicating successful startup.
- `failureMeaning`: documented interpretation when the check fails.

### Validation Rules

- Must be executable by a developer after startup.
- Must return success only when the application is running and its mandatory dependency is available.
- Must be referenced from quickstart documentation.

## State Transitions

### Local Environment

```text
not_created -> starting -> healthy -> stopping -> stopped
healthy -> failed
starting -> failed
stopped -> starting
```

### Database State

```text
absent -> initialized -> running -> stopped
running -> stopped -> running
initialized -> reset -> absent
```
