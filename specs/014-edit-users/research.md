# Research: Edit Existing Users in Cabinet

## R-001: Update API Contract Shape

**Decision**: Introduce a dedicated admin endpoint `PUT /api/users/{userId}` that updates only `displayName` and `roleCodes` for an existing user and returns the updated summary.

**Rationale**: The cabinet already works around `UserSummary` and admin-only user APIs. A dedicated update endpoint keeps behavior explicit and avoids overloading create/list contracts with partial update semantics.

**Alternatives considered**:

- `PATCH /api/users/{userId}` with sparse fields: rejected for this increment because only one immutable update shape is needed and full replacement of editable fields is simpler.
- Reusing `POST /api/users` with optional `id`: rejected because it mixes creation and update behavior and weakens validation clarity.

## R-002: Concurrency Behavior for Parallel Edits

**Decision**: Use authoritative last-write behavior at API level for this increment (no optimistic lock token in UI contract), while still handling missing target user as `404`.

**Rationale**: Current user-management contracts do not expose revision fields. Introducing optimistic concurrency now would require broader DTO migration and UI state handling beyond the requested scope.

**Alternatives considered**:

- Add `updatedAt`/version precondition and reject stale writes: useful, but deferred to a future increment focused on concurrent admin editing.
- Lock row/session in UI while editing: rejected because it introduces coordination complexity and poor UX for distributed operators.

## R-003: Last Active Admin Guardrail

**Decision**: Reject updates that remove `ADMIN` from the last active admin user. Return a business conflict response (`409`) with explicit error code.

**Rationale**: This preserves minimum recoverability of the system and directly satisfies FR-011 plus security requirements from the constitution.

**Alternatives considered**:

- Allow removal and rely on bootstrap restart behavior: rejected because it can still leave a running environment without admin capability.
- Allow removal only for self-edit restrictions: insufficient, because another admin could still remove the final admin role incorrectly.

## R-004: Role Validation and Normalization Rules

**Decision**: Accept role codes as a set-like input, trim and deduplicate values, reject empty resulting set, and reject unknown/unsupported role codes.

**Rationale**: This aligns update behavior with user creation rules and avoids accidental duplicate assignments while keeping API contract stable.

**Alternatives considered**:

- Persist duplicates as-is: rejected because user-role semantics are set-based and duplicates are meaningless.
- Auto-fallback to default role when empty set submitted: rejected because it masks operator mistakes and can grant unintended access.

## R-005: Audit Event Scope for User Updates

**Decision**: Record successful updates as authentication audit events using a dedicated event type (for example, `USER_UPDATED`) containing actor login/id, target login/id, and changed attributes (`displayName`, added roles, removed roles), with no sensitive fields.

**Rationale**: User/role edits are privileged security operations and must remain traceable without exposing passwords or hashes.

**Alternatives considered**:

- No audit for updates: rejected by constitution traceability requirements.
- Store full before/after entity snapshot: rejected because it can leak unnecessary data and increase payload size.

## R-006: Cabinet Edit UX Pattern

**Decision**: Extend the existing `UsersPage` create-flow pattern with an edit action that opens a prefilled modal form, submits to update API, and triggers list refresh with success/error feedback.

**Rationale**: Reusing current UX primitives minimizes implementation risk, keeps behavior consistent for administrators, and avoids introducing a separate detail page.

**Alternatives considered**:

- Separate `/cabinet/users/:id` page: rejected as unnecessary navigation complexity for Phase 01.
- Inline row editing in table: rejected because role multi-select and validation feedback are more complex inline.
