# Research: Superadmin Seed and User Management

## R-001: Superadmin Bootstrap Strategy

**Decision**: Keep local developer convenience, but introduce production-like bootstrap that fails startup when no `ADMIN` user exists and secure operator-provided credentials are missing.

**Rationale**: The spec requires a safe primary administrator without hardcoded production credentials. Failing startup is more operationally visible than silently starting without an administrator and avoids insecure fallback accounts.

**Alternatives considered**:

- Start normally without creating a superadmin: rejected because it can leave a system inaccessible without manual database changes.
- Expose only a health/config warning: rejected because it is easier to miss and still allows a partially unusable deployment.
- Create `admin/admin` everywhere: rejected by security requirements.

## R-002: Role Catalog Source

**Decision**: Treat backend role codes as the source of truth and expose supported roles to the cabinet from the auth module. Ensure the Phase 01 role catalog contains `ADMIN`, `ORDER_MANAGER`, `WAREHOUSE`, `PRODUCTION_SUPERVISOR`, and `PRODUCTION_EXECUTOR`.

**Rationale**: Existing permissions already use backend role codes. The frontend has legacy/display role strings for route compatibility, but new user creation should not duplicate that as an independent source of truth.

**Alternatives considered**:

- Hardcode roles only in the frontend: rejected because it can drift from backend permissions and database state.
- Allow arbitrary role codes in the create form: rejected because unsupported roles would create confusing or inaccessible accounts.
- Only list roles currently present in `app_role`: insufficient unless the catalog is guaranteed to be seeded before admin user creation.

## R-003: User List/Search Shape

**Decision**: Expand user summaries used by admin list/search to include assigned roles.

**Rationale**: Administrators need to understand current access, not just identity fields. This also satisfies the clarified requirement that search results display roles.

**Alternatives considered**:

- Identity-only list: rejected because it weakens user-management value and would likely require a follow-up detail view.
- Roles only after creation: rejected because existing users would remain opaque.

## R-004: Create User Password Policy

**Decision**: Require only a non-empty initial password in this increment, while modeling password validation so stricter rules can be introduced later.

**Rationale**: The feature explicitly excludes password-change and reset flows. Non-empty validation matches the clarified Phase 01 scope and avoids adding first-login rotation dependencies.

**Alternatives considered**:

- Minimum 8 or 12 characters: reasonable security improvement, but not requested for this increment.
- Composition rules: rejected for now because they add UX and validation complexity without supporting password lifecycle features.

## R-005: Administrator Creating Administrators

**Decision**: Allow an `ADMIN` user to create another user with any supported role, including `ADMIN`.

**Rationale**: The administrator role is the highest operational access boundary in Phase 01. Allowing additional administrators reduces single-admin lockout risk, provided creation remains admin-only and auditable.

**Alternatives considered**:

- Disallow creating `ADMIN`: rejected because it forces out-of-band setup for routine operational redundancy.
- Require a special confirmation flow: useful later, but out of scope for minimal Phase 01 user creation.

## R-006: Audit of User Creation

**Decision**: Record user-creation and bootstrap activity as authentication/audit events in the existing auth audit stream, with details that never include password values.

**Rationale**: User creation changes security state. The constitution requires traceability for roles and user actions, and existing `AuthenticationAuditEvent` already guards against password leakage in details.

**Alternatives considered**:

- No audit for user creation: rejected because privileged access changes must be traceable.
- New audit subsystem: rejected because existing auth audit is sufficient for this increment.

## R-007: Frontend Placement

**Decision**: Add an admin-only Users page in the cabinet, visible in the sidebar only for `isAdmin`, and protected by route metadata requiring `ADMIN`.

**Rationale**: This follows existing Audit/Warehouse patterns and keeps the page discoverable to administrators while hidden from operational roles.

**Alternatives considered**:

- Put creation inside Audit: rejected because user management is a distinct administration task.
- Hide route but expose only via direct URL: rejected because the spec requires a visible cabinet section for superadmins.
