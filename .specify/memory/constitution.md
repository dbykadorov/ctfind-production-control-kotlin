<!--
Sync Impact Report
Version change: template -> 1.0.0
Modified principles:
- Placeholder principle 1 -> I. Domain-First ERP Core
- Placeholder principle 2 -> II. Constraint-Aware Operations
- Placeholder principle 3 -> III. Domain-Centered Modular Architecture
- Placeholder principle 4 -> IV. Traceability & Auditability
- Placeholder principle 5 -> V. API-Only Backend & Explicit Security
- Added: VI. Docker-First Verifiable Delivery
Added sections:
- Technical Constraints
- Architecture Rules
- Quality Gates
- Development Workflow
Removed sections:
- Placeholder SECTION_2_NAME
- Placeholder SECTION_3_NAME
Templates requiring updates:
- ✅ .specify/templates/plan-template.md
- ✅ .specify/templates/spec-template.md
- ✅ .specify/templates/tasks-template.md
- ✅ .specify/templates/commands/*.md (no command templates present)
Runtime guidance:
- ✅ README.md reviewed; current Docker/frontend/runtime guidance is aligned
Follow-up TODOs:
- None
-->

# CTfind Production Control Contlin Constitution

## Core Principles

### I. Domain-First ERP Core

The system is an ERP-oriented production control platform. Every feature MUST
strengthen a coherent operational model: orders, customers, products,
production tasks, inventory, employees, roles, audit events, and internal
notifications.

Business workflows MUST be modeled as domain behavior, not as isolated screens
or database tables. UI, API, and persistence exist to serve the domain model,
not replace it.

### II. Constraint-Aware Operations

The system MUST be designed to evolve toward Theory of Constraints-based
production management.

Phase 1 MAY implement basic ERP/control functions, but it MUST preserve the
facts needed for future TOC features: task flow history, waiting time,
start/finish timestamps, blocking reasons, resource/work-center context,
due-date pressure, and bottleneck impact.

The project MUST NOT hardcode a single priority model such as FIFO or
created-date sorting because future buffer management is a planned direction.

### III. Domain-Centered Modular Architecture

The backend evolves as a modular monolith with clean/hexagonal boundaries.

Domain and application logic MUST NOT depend directly on HTTP controllers,
Spring MVC concerns, database schema details, JPA persistence mechanics, or
frontend DTOs. Infrastructure adapts to the domain, not the domain to
infrastructure.

Controllers MUST NOT contain business rules. DTOs MUST NOT become domain
objects. Use cases SHOULD be testable without the web layer and, where
practical, without a real database.

### IV. Traceability & Auditability

Every significant business-state change MUST be traceable.

For orders, production tasks, inventory movements, roles, and user actions,
the system MUST preserve who changed what, when, from which state to which
state, and why or under what context. Auditability is a product feature, not
an optional technical add-on.

### V. API-Only Backend & Explicit Security

The Spring Boot backend is an API service.

It MUST NOT expose browser form login, HTTP Basic browser challenges, or
backend-rendered login pages for application users. Protected API access MUST
return explicit `401` or `403` responses.

Authentication, authorization, roles, sessions, tokens, and permission checks
MUST be designed explicitly through specs, contracts, and negative scenarios.
Security MUST NOT be added implicitly while implementing unrelated features.

### VI. Docker-First Verifiable Delivery

The primary local runtime is Docker Compose from the repository root.

Every feature that affects runtime behavior MUST preserve or update the root
Docker workflow, documented ports, health checks, logs, and quickstart
validation. A feature is not complete until its relevant tests, builds, health
checks, or smoke checks have fresh verification evidence.

## Technical Constraints

- Backend stack: Spring Boot, Kotlin, Java 21, Gradle, PostgreSQL, Flyway.
- Frontend stack: Vue 3, Vite, TypeScript, Pinia, vue-router, Tailwind.
- Backend is a modular monolith before any service split.
- PostgreSQL schema changes MUST use Flyway migrations.
- Local startup MUST remain available through `docker compose up --build --wait`.
- Legacy Frappe/Tryton code MAY be used as a reference, but MUST NOT become a
  runtime dependency.
- Frontend code belongs to this repository after migration; old Frappe asset
  paths, socket assumptions, and API assumptions MUST be isolated or removed.
- Real authentication and authorization flows require their own specification
  before implementation.

## Architecture Rules

- Domain layer contains entities, value objects, domain services, policies, and
  state transitions.
- Application layer contains use cases, commands, queries, orchestration, and
  transaction boundaries.
- Adapters contain REST controllers, persistence mappings, external
  integrations, and UI/API DTO conversion.
- Infrastructure contains Spring, JPA, Flyway, security config, Docker, and
  runtime wiring.
- Business rules belong in domain/application code, not controllers or
  frontend components.
- Module boundaries MUST be explicit. Cross-module access SHOULD happen through
  application/domain interfaces, not by reaching into another module's
  repositories or tables.
- Persistence models MAY be simple at first, but they MUST NOT dictate business
  terminology when the domain needs a clearer model.

## Quality Gates

Every spec/plan MUST answer:

- What ERP/domain entity or workflow does this feature strengthen?
- Can business-state changes be audited?
- Does the feature preserve facts needed for lead time, waiting time,
  throughput, or TOC buffer analysis?
- Does it avoid blocking future buffer management, bottleneck boards, or
  resource/work-center modeling?
- Are business rules placed in domain/application code rather than controllers,
  DTOs, or persistence adapters?
- Can the main use case be tested without the web layer?
- Does the backend remain API-only?
- Are permissions, negative scenarios, and audit behavior considered for
  business operations?
- Does Docker startup still work from the repository root?
- Are tests/build/runtime checks documented and freshly verified?

## Development Workflow

- Features MUST be specified with independent user stories and measurable
  acceptance criteria.
- Plans MUST include a constitution check before design and after design.
- Tasks MUST be grouped by independently testable user stories.
- Implementation MUST mark tasks complete only after verification.
- Runtime changes MUST include quickstart updates.
- Security, auth, roles, audit, and persistence changes MUST include explicit
  contracts or tests.
- TOC-specific features MUST build on reliable ERP facts, not bypass them with
  separate ad-hoc UI state.

## Governance

This constitution guides all specs, plans, tasks, and implementation decisions.

If a feature intentionally violates a principle, the plan MUST document the
exception, the reason, the risk, and the compensating checks.

Constitution changes MUST be explicit. Principle removals or incompatible
redefinitions require a MAJOR version bump. New principles or materially
expanded guidance require a MINOR version bump. Clarifications and wording-only
changes require a PATCH version bump.

All future plans MUST review compliance through the Constitution Check section.
All implementation summaries MUST report the verification evidence used to mark
the work complete.

**Version**: 1.0.0 | **Ratified**: 2026-04-26 | **Last Amended**: 2026-04-26
