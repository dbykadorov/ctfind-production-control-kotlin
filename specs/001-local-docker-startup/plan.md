# Implementation Plan: Local Container Startup

**Branch**: `001-local-docker-startup` | **Date**: 2026-04-25 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `specs/001-local-docker-startup/spec.md`

## Summary

Provide a reproducible local container startup path for the backend application and its required
local dependencies. The implementation will add a development-only container composition with the
application service, PostgreSQL, health/readiness checks, predictable start/stop/log commands, and
documentation that lets a developer complete the local run cycle without installing application
services directly on the host.

## Technical Context

**Language/Version**: Kotlin 2.2.21, Java 21  
**Primary Dependencies**: Spring Boot 4.0.6, Spring WebMVC, Spring Security, Spring Data JPA, Flyway, Actuator, PostgreSQL driver, Gradle Kotlin DSL  
**Storage**: PostgreSQL for local runtime state, isolated in a development container volume  
**Testing**: Gradle `test`; local runtime verification through application health endpoint and container status checks  
**Target Platform**: Linux developer workstation with Docker-compatible container runtime  
**Project Type**: Backend web service  
**Performance Goals**: Local application health check available within 2 minutes after startup on a normal developer machine  
**Constraints**: One-command start from repository root; no production secrets; no production service connections; repeatable stop/start without manual cleanup  
**Scale/Scope**: Single backend application service plus mandatory local database dependency for Phase 1 development

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

The project constitution is still the generated template and contains no ratified project-specific
principles or enforceable gates. Planning proceeds with the following temporary quality gates derived
from the feature specification:

- Local runtime must not require production secrets or production service access.
- Local state must remain outside source control.
- Developer workflow must document start, readiness check, logs, and stop.
- Implementation must remain scoped to backend local startup; no Phase 1 business modules or UI work.

Gate status before Phase 0: PASS. No violations identified.

## Project Structure

### Documentation (this feature)

```text
specs/001-local-docker-startup/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── local-runtime.md
└── tasks.md
```

### Source Code (repository root)

```text
.
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
├── gradlew
├── src/
│   ├── main/
│   │   ├── kotlin/com/ctfind/productioncontrol/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-local.properties
│   └── test/kotlin/com/ctfind/productioncontrol/
└── specs/001-local-docker-startup/
```

**Structure Decision**: Keep the repository as a single backend service project. Add container
runtime artifacts at the repository root because they orchestrate the whole local environment. Keep
Spring configuration under `src/main/resources/`, with a local profile/config used only by the
containerized development environment.

## Complexity Tracking

No constitution violations or justified complexity exceptions.

## Phase 0: Research Summary

See [research.md](./research.md).

Key decisions:

- Use Docker Compose-compatible local orchestration for one-command startup.
- Use a multi-stage JVM image build so the runtime container does not need host Java.
- Use PostgreSQL as the required local database because the application already depends on JPA,
  Flyway, and the PostgreSQL driver.
- Use Actuator health as the readiness contract exposed to developers.

## Phase 1: Design Summary

See [data-model.md](./data-model.md), [contracts/local-runtime.md](./contracts/local-runtime.md), and
[quickstart.md](./quickstart.md).

Post-design constitution check: PASS. The design stays within backend local startup scope, keeps
development credentials local-only, isolates state in a container volume, and documents the full
developer workflow.
