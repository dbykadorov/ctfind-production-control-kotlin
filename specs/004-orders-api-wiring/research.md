# Phase 0 Research: Orders API + Frontend Wiring

## Decision: Model Orders As A Dedicated Backend Module

Use a new `orders` module with `domain`, `application`, and `adapter` subpackages, matching the existing auth module structure.

**Rationale**: Orders contain business rules that should not live in controllers or frontend code: lifecycle transitions, shipped-order editability, optimistic conflict checks, order numbering, diff generation, and write permissions. A separate module keeps the ERP order core explicit and preserves a stable place for later production task, inventory, and TOC concepts to attach.

**Alternatives considered**:

- Add order controllers and repositories directly under generic infrastructure packages: rejected because it would mix business rules with adapters.
- Put orders inside the existing auth module: rejected because auth and order management are separate domains.

## Decision: Use Server-Generated Human-Readable Order Numbers

Generate order numbers in the backend using a database-backed monotonically increasing sequence with a stable display format such as `ORD-000001`.

**Rationale**: Order numbers must be unique, searchable, human-readable, and independent of frontend behavior. A database sequence avoids collisions under concurrent creates and keeps the format changeable later without exposing internal UUIDs.

**Alternatives considered**:

- Use UUIDs as visible order numbers: rejected because they are not human-friendly for office workflows.
- Generate numbers in the frontend: rejected because it cannot guarantee uniqueness.
- Use date-prefix numbering immediately: deferred because it adds reset/format policy decisions not required for Phase 1.

## Decision: Store UUID Primary Keys And Unique Order Number

Use UUID identifiers internally and expose both `id` and `orderNumber` in API responses. Users search by `orderNumber` or customer text.

**Rationale**: UUIDs are stable technical identifiers for API routes and relationships, while `orderNumber` supports business communication. Keeping both avoids coupling persistence identity to future numbering policy.

**Alternatives considered**:

- Route only by order number: rejected because business numbering may evolve.
- Route only by UUID and omit order number: rejected because the spec requires human-readable search.

## Decision: Permission Codes Use `ADMIN` And `ORDER_MANAGER`

Allow order writes for `ADMIN` and `ORDER_MANAGER`; allow read-only order access to any authenticated Phase 1 role.

**Rationale**: `ADMIN` already exists in the auth module. A canonical uppercase `ORDER_MANAGER` code fits the existing role model and keeps UI labels independent from backend authorization.

**Alternatives considered**:

- Use localized role names in permission checks: rejected because display labels should not drive security.
- Add fine-grained permissions table now: deferred because Phase 1 only needs one write permission boundary.

## Decision: Enforce Direct Forward Status Transitions In Domain/Application Code

Represent statuses as `NEW`, `IN_WORK`, `READY`, and `SHIPPED` in backend code, with localized labels mapped at the API/frontend boundary. Allow only `NEW -> IN_WORK -> READY -> SHIPPED`.

**Rationale**: The lifecycle is a business invariant and must be testable without HTTP or persistence. Internal English enum codes avoid encoding localized UI text into code paths while still returning user-facing Russian labels where needed.

**Alternatives considered**:

- Store and compare Russian status strings directly in domain code: rejected because it mixes UI text with business invariants.
- Allow arbitrary status changes with validation in the controller: rejected because controllers must not contain business rules.

## Decision: Use Optimistic Locking For Stale Updates

Add a numeric `version` field to customer orders and require update/status requests to include the expected version.

**Rationale**: The spec requires preventing silent overwrites. Optimistic locking is simple for a low-concurrency Phase 1 system, fits JPA/PostgreSQL, and provides a clear `409 Conflict` outcome to the frontend.

**Alternatives considered**:

- Last-write-wins updates: rejected because it violates stale update requirements.
- Pessimistic row locks for UI editing sessions: rejected as too heavy and brittle for browser workflows.

## Decision: Store Key Business-Field Diffs As JSONB

Record order update diffs in an `order_change_diff` table with field-level before/after values for customer, delivery date, notes, item summary, and status when applicable. Include nullable JSONB columns for future full before/after snapshots.

**Rationale**: Phase 1 needs reviewable business-field diffs now, and the user clarified that full snapshots should remain possible later. JSONB keeps the diff flexible while relational columns preserve queryable order, actor, and timestamp metadata.

**Alternatives considered**:

- Store only text audit summaries: rejected because they are hard to review and cannot support future structured history.
- Store full snapshots immediately: deferred because it is more expensive to design and test than Phase 1 requires.

## Decision: Seed Customers, Roles, And Example Orders In The Local Profile

Extend local seeding to create active/inactive customers, several orders across statuses, order items, and an order-manager role/user if needed for manual verification.

**Rationale**: The feature must be immediately reviewable after local startup. Seeded data also makes dashboard and list behavior meaningful without manual database setup.

**Alternatives considered**:

- Only seed customers: rejected because dashboard/list verification would remain weak.
- Require manual SQL setup: rejected because the project constitution requires Docker-first verifiable delivery.

## Decision: Dashboard Uses Lightweight Order Summary Endpoints

Expose order list/detail/customer APIs plus a dedicated dashboard summary contract for KPI counts, status distribution, recent changes, and trend data.

**Rationale**: Reconstructing dashboard metrics in the frontend from paginated order lists would duplicate rules and make overdue/status counting inconsistent. Summary endpoints keep the frontend simple and allow backend tests around metric definitions.

**Alternatives considered**:

- Fetch all orders and aggregate client-side: rejected for correctness and future scale.
- Leave dashboard empty until later: rejected because the spec includes dashboard wiring.
