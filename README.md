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
- Vue 3 / Vite / TypeScript frontend cabinet

## Local Checks

Requires JDK 21 available on `PATH` or via `JAVA_HOME`.

```bash
./gradlew test
```

Frontend checks live in the migrated cabinet app:

```bash
cd frontend/cabinet
pnpm test
pnpm build
```

## Local Container Runtime

The primary local startup path is containerized. It starts the backend app,
the migrated frontend cabinet, and the local PostgreSQL dependency without
requiring a host JDK or host Node runtime.

### Prerequisites

- Docker Compose-compatible container runtime installed and running
- Host ports `8080`, `15432`, and `5173` available

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

The backend API is available at `http://localhost:8080`.

The migrated cabinet frontend is available at:

```text
http://localhost:5173/cabinet/login
```

The old Frappe cabinet source was used only as a migration reference. Runtime
startup must not read from or require the old Frappe project.

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

Frontend logs:

```bash
docker compose logs -f frontend
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
ss -ltnp | rg ':8080|:15432|:5173'
```

If the health check is not `UP`, inspect both services:

```bash
docker compose logs app
docker compose logs frontend
docker compose logs postgres
```

Common causes are a still-starting database, datasource configuration errors,
or a port conflict on the host.

If the cabinet login screen is blank, inspect browser console and frontend logs.
Common causes are missing migrated assets, a stale Vite base path, or a legacy
Frappe integration running before login render.

## Product Context

The first implementation target is Phase 1:

- users, employees, roles, and access control;
- orders;
- production tasks;
- basic inventory;
- audit log;
- internal notifications.

Phase 2 is expected to add Theory of Constraints concepts on top of the operational facts captured in Phase 1.
