# CTfind Cabinet Frontend

Migrated Vue/Vite cabinet SPA for the Spring/Kotlin CTfind Production Control platform.

## Local Runtime

The primary local path is the root Docker workflow:

```bash
docker compose up --build --wait
```

Open:

```text
http://localhost:5173/cabinet/login
```

Local development login:

```text
login: admin
password: admin
```

The cabinet signs in through the Spring/Kotlin backend at `/api/auth/login` and
stores the local MVP Bearer JWT in browser `localStorage`. This is a development
bootstrap flow only, not a production security setup.

## Development Commands

```bash
pnpm install
pnpm dev
pnpm test
pnpm build
```

## Migration Boundaries

- The old Frappe app path was used only as a source reference during migration.
- The Vite base path is `/cabinet/`, not the old Frappe asset path.
- Login submit uses the new Spring/Kotlin `/api/auth/*` endpoints and must not call the old Frappe login endpoint.
- Legacy Frappe API/socket/boot modules remain as migration references, but login rendering and authentication do not require Frappe runtime.
