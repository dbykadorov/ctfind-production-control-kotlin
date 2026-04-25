# Contract: Local Runtime

This contract defines the developer-facing local startup interface. It is intentionally operational:
the feature is successful when these commands, ports, and checks behave as documented.

## Prerequisites

- A Docker Compose-compatible container runtime is installed and running.
- Repository root is the current working directory.
- Host ports required by the local environment are available.

## Commands

### Start

```bash
docker compose up --build
```

Expected behavior:

- Builds the backend application image if needed.
- Starts the database service.
- Starts the application service.
- Streams logs in the foreground.
- Leaves application reachable from the host when startup succeeds.

### Start In Background

```bash
docker compose up --build --wait
```

Expected behavior:

- Starts the same environment in detached mode.
- Returns control to the shell after services are healthy or a service fails readiness.

### Stop

```bash
docker compose down
```

Expected behavior:

- Stops application and database services.
- Preserves named local database volume.
- Does not require manual process cleanup before the next start.

### Reset Local State

```bash
docker compose down -v
```

Expected behavior:

- Stops services.
- Removes named local database volume.
- Next start initializes a clean local database.

### Logs

```bash
docker compose logs -f app
docker compose logs -f postgres
```

Expected behavior:

- Shows application logs for `app`.
- Shows database logs for `postgres`.
- Supports troubleshooting failed local startup.

## Ports

| Service | Host Port | Purpose |
|---------|-----------|---------|
| app | 8080 | Local application HTTP access |
| postgres | 15432 | Optional local database diagnostics mapped to container port 5432 |

## Readiness Check

```bash
curl http://localhost:8080/actuator/health
```

Expected healthy result:

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

Failure interpretation:

- Connection refused: application service is not ready or port is unavailable.
- Non-healthy response: application started but a mandatory dependency or health contributor is not
  ready.
- Authentication challenge on other routes is acceptable; the health check is the readiness contract
  for this slice.

## Environment Contract

The local environment must provide these development-only values to the application:

| Name | Purpose |
|------|---------|
| `SPRING_PROFILES_ACTIVE` | Selects local development configuration |
| `SPRING_DATASOURCE_URL` | Points application to the local database service |
| `SPRING_DATASOURCE_USERNAME` | Local database username |
| `SPRING_DATASOURCE_PASSWORD` | Local database password |

All values are development-only and must not be reused as production secrets.

## Version Control Contract

The following must not be committed:

- Build output directories.
- Local database files.
- Runtime logs generated outside container logs.
- Host-specific override files containing private secrets.
