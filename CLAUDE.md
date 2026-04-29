# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

`AGENTS.md` is the authoritative rulebook (architecture boundaries, change rules, security/auth semantics, verification). Read it before non-trivial work; this file only adds the architectural big picture and a command quick‑reference.

## Commands

Always prefer root `make` targets (see `Makefile`) over ad‑hoc gradle/pnpm invocations:

- `make backend-test` / `make backend-build` / `make backend-run`
- `make frontend-install` / `make frontend-test` / `make frontend-build`
- `make test` (both) / `make build` (both)
- `make docker-up-detached` / `make docker-down` / `make docker-reset` (drops volumes)
- `make health` — hits `http://localhost:8080/actuator/health`

Single backend test:

```bash
./gradlew test --tests 'com.ctfind.productioncontrol.production.application.CreateProductionTasksFromOrderUseCaseTests'
```

Single frontend test (run from `frontend/cabinet`):

```bash
pnpm vitest run path/to/file.spec.ts
# or filter by name
pnpm vitest run -t 'name fragment'
```

Frontend also exposes (not wrapped by Make): `pnpm typecheck`, `pnpm lint`, `pnpm gen:types`.

Local stack ports: backend `8080`, postgres `15432`, cabinet `5173` (login at `http://localhost:5173/cabinet/login`, seeded `admin`/`admin`). Auth is JWT Bearer via `/api/auth/login`, stored in browser `localStorage`.

## Backend architecture

Single Gradle module, package root `com.ctfind.productioncontrol`. Each business module (`auth`, `orders`, `production`, plus shared `infrastructure`) follows a hexagonal layout that **must be preserved**:

```
<module>/
  domain/        # entities, value objects, policies — pure Kotlin, no Spring
  application/   # use cases + Ports (interfaces) + models — orchestration & policy
  adapter/
    web/         # @RestController + DTOs, adapts HTTP to use cases
    persistence/ # JPA entities, repositories, Port implementations, query filters
```

Rules that follow from this layout (do not violate without explicit reason):

- **Domain has no framework dependencies.** Business invariants live here (e.g., `production/domain/ProductionTaskPolicies.kt`).
- **Application layer owns use cases and ports.** Permission checks live at the use-case boundary (e.g., `ProductionTaskPermissions.kt`), not in controllers or adapters. Each use case is its own file (`*UseCase.kt`); ports are grouped per module (`ProductionTaskPorts.kt`, `AuthenticationPorts.kt`).
- **Cross-module access goes through ports.** `production` reads order context via `ProductionOrderSourcePort` (implemented in `production/adapter/persistence`, sourcing data from the orders module). Do **not** import `orders.*` from `production.application` or move production rules into `orders`.
- **Controllers adapt HTTP only.** They translate request DTOs into use-case calls and map results to response DTOs; no policy decisions, no direct repository access.
- **Persistence adapters map data, not lifecycle.** JPA entities are isolated in `*JpaEntities.kt`/`*JpaRepositories.kt`; the `*PersistenceAdapters.kt` file implements the application ports and is the only place that bridges JPA ↔ domain.

Auth/authorization conventions (enforced in security config + use cases):

- Unauthenticated → 401, unauthorized (authenticated but lacking role) → 403. Never weaken role checks for production workflows.
- Local-only dev secrets live in `application-local.properties`; production config relies on env vars (`SPRING_DATASOURCE_*`).

Schema changes require a new Flyway migration under `src/main/resources/db/migration/` (`V<n>__<desc>.sql`); JPA runs with `ddl-auto=validate`, so entity changes without a matching migration will fail boot.

Tests mirror the source tree (`src/test/kotlin/.../{domain,application,adapter}`). Add tests at the layer where the behavior change lives.

## Frontend architecture

Vue 3 SPA in `frontend/cabinet` (pnpm, Vite, TS strict via `vue-tsc`). Layout:

- `src/api/api-client.ts` — axios instance with JWT interceptor; `auth-service.ts` handles login/logout/token storage.
- `src/api/composables/` — one composable per backend resource (e.g., `use-production-tasks.ts`). **Reuse these patterns** when adding new endpoints rather than calling axios directly from pages.
- `src/api/types/` — TS types mirroring backend DTOs. `pnpm gen:types` regenerates from the backend (when applicable).
- `src/pages/{auth,office,production,common}` — route-level views; `src/components/{ui,common,domain,layout}` — `ui` is the shadcn-style primitive set, `domain` holds feature-specific composites.
- `src/router`, `src/stores` (Pinia), `src/i18n` (vue-i18n), `src/styles` (Tailwind + design tokens).

Frontend changes for production-task UI must stay inside `frontend/cabinet`; the old Frappe cabinet is reference-only and must not be a runtime dependency.

## Spec-driven workflow

Active feature work lives under `specs/<NNN>-<slug>/` (current: `006-production-tasks-board-m4`). When behavior or contracts change, update the relevant `specs/.../{spec,plan,tasks}.md` alongside code — `AGENTS.md` requires this.

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan:
`specs/009-notification-triggers/plan.md`
<!-- SPECKIT END -->
