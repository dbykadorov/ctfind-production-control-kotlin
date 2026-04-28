# Research: Журнал действий администратора (Phase 1 §8 #10)

**Feature**: 007-audit-log-viewer
**Date**: 2026-04-28

## R-001: Query Strategy — 3 JPA Queries + In-Memory Merge

**Decision**: Fetch from each of the three audit tables via separate JPA repository queries with SQL-level date-range filtering, then merge, sort, and paginate in memory.

**Rationale**: Consistent with the existing codebase pattern (production tasks and orders both use `findAll()` + in-memory filtering + manual pagination). The date-range filter at the SQL level bounds the dataset size. With a 7-day default window, the volume is manageable (hundreds to low thousands of events).

**Alternatives considered**:
- SQL UNION ALL via native query / EntityManager: more efficient for large datasets, but introduces a new infrastructure pattern (no native SQL queries in the current codebase). Viable Phase 2 optimization if in-memory merge becomes a bottleneck.
- Database VIEW with UNION: requires a Flyway migration for a read-only concern; over-engineers Phase 1.

## R-002: Module Structure — New `audit` Hexagonal Module

**Decision**: Create a new `audit` module under `com.ctfind.productioncontrol.audit` following the established hexagonal layout: `domain/`, `application/`, `adapter/web/`, `adapter/persistence/`.

**Rationale**: Audit log is a distinct cross-cutting concern that reads from all three audit tables. It doesn't belong in `auth`, `orders`, or `production` — each of those only writes to its own audit table. The unified read is a new responsibility.

**Alternatives considered**:
- Extend `auth` module: violates single-responsibility; auth writes auth events, audit reads all.
- Shared `infrastructure` package: the audit log has its own use case, domain model, and API contract — it's a proper module, not infrastructure wiring.

## R-003: Cross-Module Data Access — Read-Only JPA Entities

**Decision**: The audit module's persistence adapter defines its own read-only JPA `@Entity` classes mapping to `auth_audit_event`, `order_audit_event`, and `production_task_audit_event`. These entities include only the columns needed for the audit log view (not all columns).

**Rationale**: Audit tables are cross-cutting infrastructure — they're written by their respective modules but read cross-module by the audit viewer. Creating read-only projections avoids coupling the audit module to the internal JPA entities of auth/orders/production. This follows the same pattern as `ProductionOrderSourcePort` (production reads order data via its own adapter without importing `orders.*`).

**Alternatives considered**:
- Reuse existing JPA entities: creates compile-time coupling between audit module and auth/orders/production adapter packages.
- Use ports from other modules: no existing port exposes audit queries; adding one to each module for audit's benefit is over-engineered.

## R-004: Actor Display Name Resolution

**Decision**: LEFT JOIN `app_user` on `actor_user_id` (or `user_id` for auth events) in each JPA query. Fallback chain: `display_name` → `login` column from the event → placeholder «Удалённый пользователь».

**Rationale**: Auth events can have `user_id = null` (failed login for unknown user) — in that case the event's `login` field is the best identifier. For order/production events, `actor_user_id` is NOT NULL but the referenced user may have been deleted (SET NULL foreign key on auth events; no cascade on order/production) — fallback to the login stored in JWT claims at event time is not available, so placeholder is the last resort.

**Alternatives considered**:
- Snapshot display name at write time into each audit event: requires a migration + code changes to all three audit writers — too invasive for Phase 1.

## R-005: Auth Event Summary Generation

**Decision**: Map `event_type` + `outcome` + `login` to a human-readable Russian summary in the audit persistence adapter. Examples:
- `LOGIN_SUCCESS` + `SUCCESS` + `admin` → `«Вход в систему: admin»`
- `LOGIN_FAILURE` + `INVALID_CREDENTIALS` + `unknown` → `«Неудачный вход: unknown — неверные учётные данные»`
- `LOGOUT` + `LOGGED_OUT` + `admin` → `«Выход из системы: admin»`
- `LOCAL_SEED` + `SEEDED` + `admin` → `«Инициализация учётных данных: admin»`

**Rationale**: Auth events have no `summary` column (unlike order/production). The mapping is deterministic from the finite enum values. Placing it in the persistence adapter keeps the domain model clean — `AuditLogRow.summary` is always a ready-to-display string.

**Alternatives considered**:
- Frontend-side mapping: leaks domain knowledge into the UI layer.
- Store summary at write time: requires auth module code change + migration.

## R-006: Search Implementation — ILIKE on 3 Fields

**Decision**: Per FR-013 clarification, search uses `ILIKE '%term%'` (case-insensitive substring) on: `summary` (or `details` for auth), `target_id::text` (for order/production), and `actor login` (direct column for auth, joined from `app_user.login` for order/production).

**Rationale**: Substring search is simple, covers the identified use cases (finding events by order number, task number, user login), and doesn't require full-text search infrastructure. Performance is bounded by the date-range filter.

**Alternatives considered**:
- PostgreSQL tsvector/tsquery: over-engineered for Phase 1; valuable if event volume grows significantly.
- Prefix-only search: too restrictive for investigative use cases ("find events mentioning order X").

## R-007: User Search for Actor Picker — New ADMIN-Only Endpoint

**Decision**: Add `GET /api/users?search=&limit=` endpoint in the auth module. ADMIN-only. Returns `[{id, login, displayName}]`. Used by the frontend actor picker on the audit page.

**Rationale**: The existing `GET /api/production-tasks/assignees` only returns users with `PRODUCTION_EXECUTOR` role. The audit log needs ALL users as potential actors. A separate endpoint in the auth module is clean and reusable.

**Alternatives considered**:
- Extend the assignees endpoint: would mix production-task concerns with general user lookup.
- Text input instead of picker: spec FR-012 says "выбор пользователя из системы" — implies a proper picker.

## R-008: No Flyway Migration Required

**Decision**: No new database migrations. The feature reads from existing tables. JPA entities in the audit module use `ddl-auto=validate` (project default) and map only the columns they need — all of which already exist.

**Rationale**: The three audit tables (V2, V4, V5 migrations) and `app_user` (V2) are already in production schema. Adding read-only JPA mappings requires no DDL changes. The `GET /api/users` endpoint reads from the existing `app_user` table.

## R-009: Frontend Architecture — Existing Patterns

**Decision**: Follow established patterns from `ProductionTasksListPage.vue` and `use-production-tasks.ts`:
- New composable `useAuditLog` with `{ data, loading, error, refetch }` interface
- Filter state in component refs with computed filters object
- Debounced search (300ms)
- AbortController for request cancellation
- Error mapping: 403 → forbidden state, network error → error banner
- Pagination controls with page/totalPages/totalItems
- Loading skeleton, empty state, error state
- Route guard with `meta.roles: ['ADMIN']`
- Nav item visible only for ADMIN via permissions composable

**Rationale**: Consistency with existing frontend architecture reduces learning curve and review friction.
