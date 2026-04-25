# CTfind Production Control Contlin

New Spring Boot + Kotlin attempt for CTfind Production Control.

## Stack

- Kotlin
- Spring Boot
- Gradle Kotlin DSL
- Java 21
- PostgreSQL
- Flyway
- Spring Security
- Spring Data JPA
- Bean Validation
- Actuator

## Local Checks

Requires JDK 21 available on `PATH` or via `JAVA_HOME`.

```bash
./gradlew test
```

## Local Container Runtime

The primary local startup path is containerized. It starts the backend app and
its local PostgreSQL dependency without requiring a host JDK.

### Prerequisites

- Docker Compose-compatible container runtime installed and running
- Host ports `8080` and `15432` available

Check runtime availability:

```bash
docker --version
docker compose version
```

### Start

From the repository root:

```bash
docker compose up --build
```

Detached mode:

```bash
docker compose up --build --wait
```

The application is available at `http://localhost:8080`.

### Readiness Check

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

### Logs

Application logs:

```bash
docker compose logs -f app
```

Database logs:

```bash
docker compose logs -f postgres
```

Service status:

```bash
docker compose ps
```

### Stop

```bash
docker compose down
```

This stops services and preserves the local PostgreSQL volume.

### Reset Local Data

```bash
docker compose down -v
```

Use this when you need a clean local database on the next startup.

### Troubleshooting

If Docker is not running:

```bash
docker info
```

If ports are occupied:

```bash
ss -ltnp | rg ':8080|:15432'
```

If the health check is not `UP`, inspect both services:

```bash
docker compose logs app
docker compose logs postgres
```

Common causes are a still-starting database, datasource configuration errors,
or a port conflict on the host.

## Product Context

The first implementation target is Phase 1:

- users, employees, roles, and access control;
- orders;
- production tasks;
- basic inventory;
- audit log;
- internal notifications.

Phase 2 is expected to add Theory of Constraints concepts on top of the operational facts captured in Phase 1.
