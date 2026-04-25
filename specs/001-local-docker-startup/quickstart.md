# Quickstart: Local Container Startup

This quickstart describes the target developer workflow for the local container runtime.

## 1. Prerequisites

Install and start a Docker Compose-compatible container runtime.

Verify it is available:

```bash
docker --version
docker compose version
```

Make sure ports `8080` and `5432` are not already used by another local process.

## 2. Start The Local Environment

From the repository root:

```bash
docker compose up --build
```

For background mode:

```bash
docker compose up --build -d
```

Expected result:

- `postgres` starts with a local development database.
- `app` builds and starts.
- The application is reachable on `http://localhost:8080`.

## 3. Check Readiness

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

## 4. View Logs

Application logs:

```bash
docker compose logs -f app
```

Database logs:

```bash
docker compose logs -f postgres
```

## 5. Stop The Environment

```bash
docker compose down
```

This stops services and keeps local database state for the next startup.

## 6. Reset Local Data

Use this only when a clean local database is needed:

```bash
docker compose down -v
```

The next startup recreates the local database from scratch.

## 7. Troubleshooting

### Container Runtime Is Not Running

Symptom: commands fail before services start.

Check:

```bash
docker info
```

Start your container runtime and retry.

### Port Is Already In Use

Symptom: the application or database cannot bind to its host port.

Check which process owns the port:

```bash
ss -ltnp | rg ':8080|:5432'
```

Stop the conflicting process or change the local port mapping in a future planned change.

### Health Check Is Not UP

Check application and database logs:

```bash
docker compose logs app
docker compose logs postgres
```

Common causes:

- The database is still starting.
- The application cannot connect to the local database.
- Required local configuration is missing or invalid.
