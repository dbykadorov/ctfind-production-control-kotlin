# Research: Migrate Cabinet Frontend

## Decision: Preserve the existing Vue/Vite cabinet app

**Rationale**: The source app is already a Vue 3/Vite SPA with routes, login screen, layout,
components, Pinia stores, i18n, assets, and unit tests. Rewriting it would delay the first migrated
frontend slice and risk losing visual/workflow decisions already made in the Frappe version.

**Alternatives considered**:

- Rewrite frontend in another stack: rejected because the user asked to transfer the existing
  frontend, and the current frontend already contains the desired login UI.
- Rebuild only the login screen from scratch: rejected because the requested scope says "переносим
  все", and future slices need the existing pages/components preserved.
- Keep frontend in the old Frappe repository and reference it at runtime: rejected because the new
  platform must own and run independently from the Frappe version.

## Decision: Place migrated frontend under `frontend/cabinet/`

**Rationale**: The new repository currently has a backend service at the root. A nested
`frontend/cabinet/` directory keeps Node/Vite files, tests, assets, and package manager files scoped
to the frontend while preserving a single root Docker workflow.

**Alternatives considered**:

- Put frontend files at repository root: rejected because it would mix backend Gradle and frontend
  package manager concerns.
- Put frontend under `src/main/resources/static`: rejected for this slice because the requirement is
  Docker-run frontend, not backend-packaged static assets.
- Use a separate repository: rejected because the current project is intended to be the new platform
  workspace.

## Decision: Use a dedicated Docker `frontend` service for local startup

**Rationale**: The existing backend compose workflow already starts `app` and `postgres`. Adding a
`frontend` service keeps the root command stable while making frontend readiness/logs independently
observable. A separate service also avoids coupling frontend build/debug cycle to backend image
rebuilds.

**Alternatives considered**:

- Serve frontend through Spring Boot in this slice: rejected because it adds backend packaging work
  before the frontend migration is proven.
- Require host Node/pnpm: rejected because previous local startup deliberately avoided host runtime
  assumptions where possible.
- Use old Frappe asset pipeline: rejected because the new platform must not depend on Frappe runtime.

## Decision: Adapt Vite base and dev server for the new platform

**Rationale**: The old Vite config uses a Frappe asset base path and outputs into a Frappe public
directory. In the new project, the login screen should be available from the local frontend service
without old `/assets/ctfind_production_control/cabinet_app/` assumptions.

**Alternatives considered**:

- Keep the Frappe asset base path: rejected because it can break direct local frontend serving and
  obscure whether the migration is independent from Frappe.
- Immediately configure production CDN/static hosting: rejected as premature for a local Docker
  migration slice.

## Decision: Replace real login with an explicit placeholder outcome

**Rationale**: The feature requires the login screen to open but not authorize. The old
`auth-service.ts` talks to Frappe `/api/method/login`; that behavior must be disabled so that users
do not accidentally authenticate against the wrong runtime. A placeholder response keeps the UI
interactive and makes the current limitation clear.

**Alternatives considered**:

- Keep Frappe login endpoint temporarily: rejected because it violates the requirement that the new
  frontend does not depend on old Frappe for first startup.
- Add new backend auth now: rejected because auth/IAM is a separate future feature-slice.
- Disable the form entirely: rejected because the user asked to open login; an interactive form with
  clear placeholder feedback better validates the migrated UI.

## Decision: Stub or isolate Frappe-specific API/socket/boot integrations

**Rationale**: The migrated app contains Frappe client calls, socket integration, and assumptions
around boot/session data. Those dependencies should not block initial login rendering. Keeping the
modules present but making their startup behavior safe gives future API/auth slices a clear
reintegration boundary.

**Alternatives considered**:

- Delete Frappe-specific modules during migration: rejected because future migration work may need
  the old integration logic as reference.
- Let network calls fail in the browser: rejected because noisy failures make it hard to distinguish
  expected auth placeholders from broken migration.
