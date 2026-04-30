# Data Model: Edit Existing Users in Cabinet

## Entity: User Account Profile

Represents an existing user account that can be updated by an administrator.

### Fields

- `id`: stable unique identifier.
- `login`: unique authentication login (immutable in this feature).
- `displayName`: mutable human-readable name.
- `enabled`: active flag used for authentication and admin-floor validation.
- `roleCodes`: normalized set of assigned role codes.
- `createdAt`: creation timestamp.
- `updatedAt`: timestamp of last mutation.

### Validation

- `login` is read-only for edit operations.
- `displayName` must be non-empty after trim.
- `roleCodes` must be non-empty after normalization.
- Every role code must exist in the supported catalog.
- Removing `ADMIN` is forbidden when target user is the last active admin.

### Relationships

- Many-to-many with `Role` via user-role assignments.
- Successful updates produce one `User Update Audit Record`.

## Entity: Role Assignment Set

Represents canonical role membership used for permissions and cabinet routing.

### Fields

- `codes`: unique set of role codes (`Set<String>` semantic model).
- `labels`: role display names resolved from role catalog.

### Rules

- Input codes are trimmed and deduplicated.
- Unknown codes are rejected.
- Empty resulting set is rejected.
- Set may include `ADMIN` and combinations with operational roles.

## Entity: Update User Command

Represents one admin mutation request for a target user.

### Fields

- `actorLogin`: authenticated user login.
- `actorRoles`: authenticated role codes.
- `targetUserId`: identifier of edited user.
- `displayName`: requested new display name.
- `roleCodes`: requested new role codes.

### Validation Outcomes

- `Success(updatedUserSummary)`: target updated and returned.
- `Forbidden`: actor lacks `ADMIN`.
- `NotFound`: target user does not exist anymore.
- `ValidationError`: empty display name or empty normalized role set.
- `InvalidRoles`: unknown/unsupported role codes supplied.
- `LastAdminRemovalForbidden`: update would remove `ADMIN` from last active admin.

## Entity: User Role Delta

Represents change-set for audit details.

### Fields

- `displayNameChanged`: boolean plus before/after values when changed.
- `addedRoleCodes`: role codes present after update but absent before.
- `removedRoleCodes`: role codes absent after update but present before.

### Rules

- Delta is computed from normalized role sets.
- Empty delta is allowed (idempotent save) and still returns success.

## Entity: User Update Audit Record

Represents traceability for successful user edit operations.

### Fields

- `eventType`: `USER_UPDATED` (or equivalent auth audit event type).
- `actorUserId`/`actorLogin`: who performed the change.
- `targetUserId`/`targetLogin`: whose profile was updated.
- `details`: structured summary of changed fields (display name and role delta).
- `occurredAt`: event timestamp.

### Privacy Rules

- Never stores password or password hash.
- Never stores token/session secrets.

## State Transitions

### User Edit Lifecycle

```text
existing user
  -> (valid admin update) updated user profile + updated role assignment set + audit record
  -> (invalid input) unchanged
  -> (target missing) unchanged
  -> (last-admin guard violation) unchanged
```

### Authorization Transition

```text
request without JWT -> 401 unauthorized
request with non-admin JWT -> 403 forbidden
request with ADMIN JWT -> evaluated by business validation and guardrails
```
