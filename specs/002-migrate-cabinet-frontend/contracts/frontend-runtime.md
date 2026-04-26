# Contract: Frontend Runtime

This contract defines the local developer-facing runtime for the migrated cabinet frontend.

## Source Location

The migrated frontend app is expected under:

```text
frontend/cabinet/
```

The old Frappe path may be used as source reference during implementation, but runtime must not read
from it:

```text
/home/dbykadorov/Work/projects/ctfind-production-control/frappe-bench/apps/ctfind_production_control/cabinet_app
```

## Root Local Startup

### Start

```bash
docker compose up --build --wait
```

Expected behavior:

- Backend `app` service starts and remains API-only.
- `postgres` service starts as in the previous feature.
- New `frontend` service builds/starts from `frontend/cabinet/`.
- Docker returns after services are healthy or reports the failing service.

### Stop

```bash
docker compose down
```

Expected behavior:

- Backend, database, and frontend services stop.
- Local database volume is preserved.

## Frontend Service

Expected service identity:

| Field | Value |
|-------|-------|
| Compose service | `frontend` |
| Container name | `ctfind-contlin-frontend` |
| Host port | `5173` |
| Browser URL | `http://localhost:5173/cabinet/login` |
| Health URL | `http://localhost:5173/cabinet/login` |

## Required Browser Behavior

### Open Login

```text
GET http://localhost:5173/cabinet/login
```

Expected result:

- Browser displays the migrated login screen.
- No browser Basic Auth popup appears.
- No backend HTML login page appears.
- Mandatory login assets load successfully.

### Open Cabinet Root

```text
GET http://localhost:5173/cabinet
```

Expected result:

- Unauthenticated user is routed to login screen.

### Open Protected Route

```text
GET http://localhost:5173/cabinet/orders
```

Expected result:

- Unauthenticated user is routed to login screen.
- The intended route can be preserved for future auth redirect behavior.

## Logs

```bash
docker compose logs -f frontend
```

Expected result:

- Shows frontend build/startup output.
- Shows enough information to diagnose failed frontend startup.

## Independence From Frappe Runtime

The first login render must not require:

- old Frappe web server on `localhost:8000`;
- old Frappe socket server on `localhost:9000`;
- old Frappe asset public path;
- old Frappe session cookie or boot payload.
