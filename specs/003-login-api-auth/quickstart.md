# Quickstart: Login API Authentication

## Prerequisites

- Docker Compose-compatible runtime is installed and running.
- Host ports `8080`, `15432`, and `5173` are available.

## Fresh Local Startup

From the repository root:

```bash
docker compose down -v
docker compose up --build --wait
```

Expected services:

```bash
docker compose ps
```

The backend health endpoint should be `UP`:

```bash
curl http://localhost:8080/actuator/health
```

Expected JSON includes:

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

## Verify Seeded Login in Browser

Open:

```text
http://localhost:5173/cabinet/login
```

Sign in with local bootstrap credentials:

```text
login: admin
password: admin
```

Expected result:

- Login placeholder message is gone.
- User reaches a protected cabinet page.
- Refreshing the browser keeps the user authenticated while the token is valid.

## Verify Login API Directly

```bash
curl -i \
  -H 'Content-Type: application/json' \
  -d '{"login":"admin","password":"admin"}' \
  http://localhost:8080/api/auth/login
```

Expected:

- `200 OK`
- JSON body with `tokenType: "Bearer"`, `accessToken`, `expiresAt`, and user `admin`.

Use the returned token:

```bash
TOKEN='<paste accessToken here>'
curl -i -H "Authorization: Bearer ${TOKEN}" http://localhost:8080/api/auth/me
```

Expected:

- `200 OK`
- JSON body with login `admin` and role `ADMIN`.

Logout:

```bash
curl -i -X POST -H "Authorization: Bearer ${TOKEN}" http://localhost:8080/api/auth/logout
```

Expected:

- `204 No Content`

## Verify Failed Login

```bash
curl -i \
  -H 'Content-Type: application/json' \
  -d '{"login":"admin","password":"wrong"}' \
  http://localhost:8080/api/auth/login
```

Expected:

- `401 Unauthorized`
- Generic invalid-credentials response.
- No token returned.

Repeat wrong attempts for the same login/IP until throttle is reached.

Expected:

- `429 Too Many Requests`
- Generic retry-later response.
- No authenticated state.

## Verify API-Only Security

Without a token:

```bash
curl -i http://localhost:8080/api/auth/me
```

Expected:

- `401 Unauthorized`
- No browser login page.
- No HTTP Basic browser prompt.

## Local Test Commands

Backend:

```bash
./gradlew test
```

Frontend:

```bash
cd frontend/cabinet
pnpm test
pnpm build
```

## Reset

Stop services while preserving data:

```bash
docker compose down
```

Reset the local database and seeded admin:

```bash
docker compose down -v
```

## Verification Record

Last verified: 2026-04-26

- Backend tests: `docker run --rm -v ctfind-contlin-gradle-home:/root/.gradle -v "$PWD":/workspace -w /workspace eclipse-temurin:21-jdk ./gradlew --project-cache-dir /tmp/ctfind-contlin-gradle-cache test` passed.
- Frontend tests: `pnpm test` passed with 202 tests.
- Frontend build: `pnpm build` passed.
- Docker runtime: `docker compose up --build --wait` passed; `app`, `frontend`, and `postgres` were healthy.
- API smoke: `/actuator/health` returned `UP`, `admin` / `admin` login returned `200`, `/api/auth/me` returned `admin` with `ADMIN`, wrong password returned `401`, repeated wrong password returned `429`, and logout returned `204`.
- Frontend smoke: `http://localhost:5173/cabinet/login` returned successfully.
