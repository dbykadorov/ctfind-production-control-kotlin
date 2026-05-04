# AGENTS.md

## Purpose

This file defines practical rules for AI/code agents working in this repository.
Follow these instructions to keep changes consistent with project architecture and delivery flow.

## Project Snapshot

- Backend: Kotlin + Spring Boot, Gradle Kotlin DSL, Java 21
- Backend: Kotlin + Spring Boot under `production-control-api`
- Frontend: Vue 3 + Vite + TypeScript (`production-control-frontend`)
- DB: PostgreSQL, Flyway migrations
- Runtime: Docker Compose (`app`, `postgres`, `frontend`)

## Architecture Boundaries

- Keep business rules in domain/application modules, not in web or persistence adapters.
- Backend module split is intentional (`production`, `orders`, `auth`, `infrastructure`); do not collapse module boundaries.
- `production` logic should consume order context via ports/adapters, not by moving production rules into `orders`.
- Controllers should adapt HTTP and delegate use cases; avoid embedding policy decisions in controllers.
- Persistence adapters should map data and queries, not enforce business lifecycle rules.

## Daily Commands (Use Makefile)

Prefer root `make` targets over ad-hoc command variants:

- `make help` - list available targets
- `make backend-test` - backend tests
- `make backend-build` - backend build
- `make backend-run` - run backend locally
- `make frontend-install` - install frontend dependencies
- `make frontend-test` - frontend tests
- `make frontend-build` - frontend build
- `make test` - all tests
- `make build` - backend + frontend build
- `make docker-up-detached` - start local stack in detached mode
- `make docker-down` - stop local stack
- `make health` - check backend health endpoint

## Change Rules

- Make focused edits; avoid unrelated refactors.
- Preserve existing naming and packaging conventions.
- Keep API behavior JWT-protected and API-only.
- Keep compatibility with Docker local runtime.
- Do not add legacy Frappe runtime dependencies.

## Backend Guidelines

- Add/adjust tests for behavior changes in:
  - `production-control-api/src/test/kotlin/.../domain`
  - `production-control-api/src/test/kotlin/.../application`
  - `production-control-api/src/test/kotlin/.../adapter`
- For new persistence features, update Flyway migrations under `production-control-api/src/main/resources/db/migration`.
- Prefer explicit domain models and use-case ports over framework-heavy coupling.
- Enforce permission checks in application-layer policy/use-case boundaries.

## Frontend Guidelines

- Keep production-task UI changes inside `production-control-frontend`.
- Use existing API composable patterns in `production-control-frontend/src/api/composables`.
- Validate with:
  - `make frontend-test`
  - `make frontend-build`

## Verification Before Completion

Run checks relevant to changed areas before claiming done:

- Backend-only change: `make backend-test`
- Frontend-only change: `make frontend-test && make frontend-build`
- Cross-cutting/API+UI change: `make test && make build`
- If runtime behavior changed: `make docker-up-detached` and `make health`

## Security & Data Safety

- Never commit secrets, tokens, `.env` credentials, or private keys.
- Keep auth/authorization semantics explicit:
  - unauthenticated -> 401
  - unauthorized -> 403
- Do not weaken role checks for production workflows.

## Documentation Expectations

- If behavior/contract changes, update feature docs under `specs/005-production-tasks/` when relevant.
- Keep README and Make targets aligned when introducing new standard workflows.

<!-- SPECKIT START -->
For additional context about the current Spec Kit feature, read
`specs/017-spring-integration-tests/plan.md`.
<!-- SPECKIT END -->
