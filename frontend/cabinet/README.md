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
- Login submit is intentionally a placeholder and does not authorize users.
- Legacy Frappe API/socket/boot modules remain as migration references, but login rendering does not require Frappe runtime.
