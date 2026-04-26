# Research: Login API Authentication

## R-001: JWT Issuing and Validation

**Decision**: Use Spring Security JWT primitives backed by `spring-security-oauth2-resource-server` and `spring-security-oauth2-jose`. Issue signed 8-hour Bearer tokens from the authentication application service and validate them through the stateless Spring Security filter chain.

**Rationale**: This keeps JWT parsing, signature validation, and Bearer request handling inside Spring Security rather than custom filters. It preserves the existing API-only security posture and integrates naturally with `SecurityFilterChain`.

**Alternatives considered**:

- Custom JWT parsing filter: rejected because it duplicates security-sensitive framework behavior.
- Server-side HTTP session: rejected because the spec requires Bearer JWT and stateless API behavior.
- Opaque database sessions: rejected for this slice because the user explicitly selected JWT.

## R-002: Token Signing Configuration

**Decision**: Configure a local HMAC signing secret through application properties/environment. The local profile may provide a development default, but the property name and README must make the value explicitly non-production.

**Rationale**: The local MVP needs reproducible Docker startup without external secret infrastructure. Keeping the secret configurable avoids baking production assumptions into code.

**Alternatives considered**:

- RSA key pair: more production-ready, but unnecessary for a single local issuer/verifier.
- Hardcoded source-code secret only: rejected because it would normalize unsafe production behavior.

## R-003: Password Storage

**Decision**: Store only BCrypt password hashes for user credentials. Seed `admin` / `admin` by hashing `admin` at runtime if the local administrator account is absent.

**Rationale**: BCrypt is provided by Spring Security, is suitable for password verification, and avoids raw credential storage. Runtime hashing keeps migrations deterministic and avoids placing a reusable password hash directly in SQL.

**Alternatives considered**:

- Plaintext seed password: rejected by security requirements.
- Precomputed hash in Flyway SQL: acceptable but less clear for idempotent application seeding and future password changes.

## R-004: Persistence and Schema Ownership

**Decision**: Add Flyway migrations for `app_user`, `app_role`, `app_user_role`, and `auth_audit_event`. Use JPA repositories as persistence adapters behind application ports.

**Rationale**: The constitution requires PostgreSQL schema changes through Flyway, and clean boundaries keep persistence details out of domain/application rules.

**Alternatives considered**:

- Hibernate DDL auto-update: rejected because local profile already uses `ddl-auto=validate` and constitution requires migrations.
- Authentication-only in-memory users: rejected because the spec requires database seed and persistence.

## R-005: Local Admin Seeding

**Decision**: Implement an idempotent local seed use case that runs on startup in the `local` profile. It creates the `admin` user and administrator role assignment only when absent and records a seed audit event.

**Rationale**: The feature is local-bootstrap focused, and idempotent seeding avoids duplicate records on restart while keeping production user provisioning out of scope.

**Alternatives considered**:

- Flyway-only seed data: rejected because password hashing and "do not overwrite customized user" behavior are clearer in application code.
- Always resetting admin password on startup: rejected because it would violate the spec edge case for customized local data.

## R-006: Authentication Audit

**Decision**: Record login success, login failure, logout, and local seed activity in `auth_audit_event` with event type, outcome, login/user reference when known, request IP/user agent when available, timestamp, and non-sensitive details.

**Rationale**: Authentication is a security boundary and future ERP operations need accountable actor identity. The audit model must never store submitted passwords or full token values.

**Alternatives considered**:

- Log-only audit: rejected because logs are not durable product records.
- Full request payload capture: rejected because it risks storing credentials.

## R-007: Failed Login Throttling

**Decision**: Use a simple in-memory throttle policy keyed by normalized login and request IP. After repeated failures within a rolling local window, temporarily reject attempts with a retry-later outcome and HTTP `429`.

**Rationale**: The user chose simple throttling for this slice. In-memory buckets are enough for local MVP and avoid introducing distributed rate-limiting infrastructure before production hardening.

**Alternatives considered**:

- No throttle: rejected by clarification answer.
- Persistent account lockout: rejected as a larger user-management workflow.
- External rate limiter: rejected as unnecessary for local MVP.

## R-008: Frontend Auth State

**Decision**: Replace the placeholder `loginViaCabinet` implementation with `/api/auth/login`, store the returned Bearer token in `localStorage`, expose authenticated user/roles from the auth store, remove the token on logout, and add the token to protected API calls through the shared axios client.

**Rationale**: This matches the spec-selected storage model and lets refresh/browser restart preserve auth state until token expiration. Keeping storage access in the auth service/store avoids scattering token logic across components.

**Alternatives considered**:

- Session cookie: rejected because the spec selected Bearer JWT.
- Per-component token reads: rejected because it would make logout and expiration handling brittle.

## R-009: Protected Route Return

**Decision**: Keep the existing router guard pattern and use `sanitizeFrom` for preserved cabinet return paths. After successful login, route to the sanitized `from` value or `/cabinet`.

**Rationale**: The migrated frontend already has route protection and safe return-path utilities. Extending the existing pattern minimizes migration risk.

**Alternatives considered**:

- Always redirect to dashboard: rejected because the spec requires returning to the originally intended safe route.
- Trust arbitrary `from` URLs: rejected because it can create open-redirect behavior.

## R-010: Verification Strategy

**Decision**: Combine backend unit/MVC/security tests, frontend unit tests, and Docker Compose smoke checks. Minimum backend coverage includes successful login, invalid login, throttled login, authenticated `/api/auth/me`, logout audit, seed idempotency, and API-only unauthorized behavior.

**Rationale**: Authentication touches persistence, security filters, frontend state, and local runtime. A single test layer would miss important integration failures.

**Alternatives considered**:

- Only frontend tests: rejected because backend security and database behavior are central.
- Only Docker smoke checks: rejected because failure diagnosis would be too coarse.
