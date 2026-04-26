# Implementation Plan: Migrate Cabinet Frontend

**Branch**: `002-migrate-cabinet-frontend` | **Date**: 2026-04-26 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `specs/002-migrate-cabinet-frontend/spec.md`

## Summary

Перенести существующий cabinet SPA из старой Frappe-версии в новый проект как отдельный frontend
контур, запускаемый в Docker рядом с backend API. Первый implementation slice должен доказать, что
frontend отделен от Frappe runtime: локальный Docker workflow поднимает backend и frontend,
браузер открывает login screen, а submit формы не авторизует пользователя и показывает понятное
placeholder-сообщение до будущей IAM/auth интеграции.

## Technical Context

**Language/Version**: TypeScript 5.7, Vue 3.5, Java 21/Kotlin 2.2 backend remains unchanged  
**Primary Dependencies**: Vite 5, Vue Router, Pinia, vue-i18n, Tailwind CSS, existing cabinet UI/component dependencies; Docker Compose local runtime  
**Storage**: No new frontend persistence beyond existing browser state stores; backend PostgreSQL from `001-local-docker-startup` remains available  
**Testing**: Existing Vitest/unit tests from cabinet app where practical; Docker runtime validation through frontend URL and login placeholder behavior  
**Target Platform**: Linux developer workstation with Docker-compatible container runtime; browser client served from local frontend service  
**Project Type**: Web application with Spring/Kotlin backend API and migrated Vue/Vite frontend SPA  
**Performance Goals**: Login screen available within 2 minutes after local startup completes; no missing mandatory login assets  
**Constraints**: One root Docker workflow; no dependency on running old Frappe runtime for first login screen; no real authorization in this slice; backend remains API-only  
**Scale/Scope**: One migrated frontend app, local frontend service, login-only working state, preserved UI source for later API/auth reintegration

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

The project constitution is still the generated template and contains no ratified project-specific
principles or enforceable gates. Planning proceeds with temporary quality gates derived from the
specification and existing project decisions:

- Docker startup must remain reproducible from repository root.
- Backend must remain API-only and must not reintroduce browser login or Basic Auth.
- Frappe-specific frontend integrations must not block first login screen rendering.
- The migration must preserve existing UI source assets as project-owned code, not reference the old
  Frappe directory at runtime.
- Real authentication is out of scope and must be represented by an explicit placeholder state.

Gate status before Phase 0: PASS. No violations identified.

## Project Structure

### Documentation (this feature)

```text
specs/002-migrate-cabinet-frontend/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── frontend-runtime.md
│   └── login-placeholder.md
└── tasks.md
```

### Source Code (repository root)

```text
.
├── Dockerfile
├── docker-compose.yml
├── frontend/
│   └── cabinet/
│       ├── Dockerfile
│       ├── package.json
│       ├── pnpm-lock.yaml
│       ├── vite.config.ts
│       ├── tsconfig.json
│       ├── tailwind.config.js
│       ├── postcss.config.js
│       ├── public/
│       ├── scripts/
│       ├── src/
│       │   ├── api/
│       │   ├── assets/
│       │   ├── components/
│       │   ├── i18n/
│       │   ├── pages/
│       │   ├── router/
│       │   ├── stores/
│       │   ├── styles/
│       │   └── utils/
│       └── tests/
├── src/
│   ├── main/kotlin/com/ctfind/productioncontrol/
│   └── main/resources/
└── specs/002-migrate-cabinet-frontend/
```

**Structure Decision**: Keep backend at repository root as the existing Spring/Kotlin service and add
the migrated cabinet under `frontend/cabinet/`. This keeps frontend ownership clear, allows frontend
Docker build context to exclude backend artifacts, and avoids mixing Vite/Node files into the backend
source tree. Root `docker-compose.yml` remains the single orchestration entry point.

## Complexity Tracking

No constitution violations or justified complexity exceptions.

## Phase 0: Research Summary

See [research.md](./research.md).

Key decisions:

- Preserve Vue/Vite stack instead of rewriting the frontend.
- Place migrated source under `frontend/cabinet/`.
- Serve frontend via a Docker `frontend` service on a separate local port for this slice.
- Replace real login success with an explicit auth placeholder.
- Isolate Frappe-specific API/socket/boot integrations behind stubs or disabled paths until new API
  contracts are planned.

## Phase 1: Design Summary

See [data-model.md](./data-model.md), [contracts/frontend-runtime.md](./contracts/frontend-runtime.md),
[contracts/login-placeholder.md](./contracts/login-placeholder.md), and [quickstart.md](./quickstart.md).

Post-design constitution check: PASS. The design preserves the migrated app as project-owned source,
keeps backend API-only, documents the Docker frontend runtime, and explicitly prevents real
authorization in this slice.
