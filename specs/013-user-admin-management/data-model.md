# Data Model: Superadmin Seed and User Management

## Entity: User

Represents a person who can authenticate into the cabinet.

### Fields

- `id`: stable unique identifier.
- `login`: normalized unique login used for authentication.
- `displayName`: human-readable name shown in cabinet/audit UI.
- `passwordHash`: protected password representation; never returned by API.
- `enabled`: whether the account can authenticate. Initial create flow creates enabled users only.
- `roleCodes`: set of assigned role codes.
- `createdAt`: creation timestamp.
- `updatedAt`: last update timestamp.

### Validation

- `login` is required, normalized, and unique.
- `displayName` is required.
- Initial password is required and non-empty.
- At least one supported role is required.
- Every role code must exist in the supported role catalog.

### Relationships

- Many-to-many with `Role` through user-role assignment.
- User creation may produce an `AdministrativeAuditEvent`.

## Entity: Role

Represents a permission grouping used by backend permissions and cabinet visibility.

### Fields

- `id`: stable unique identifier.
- `code`: canonical role code.
- `name`: display label.
- `createdAt`: creation timestamp.

### Supported Phase 01 Catalog

| Code | Display Name | Purpose |
|------|--------------|---------|
| `ADMIN` | Administrator | Full administrative access, including users and audit |
| `ORDER_MANAGER` | Order Manager | Orders, customers, BOM, production-task creation |
| `WAREHOUSE` | Warehouse | Materials, receipts, stock consumption |
| `PRODUCTION_SUPERVISOR` | Production Supervisor | Production list/board, assignment, status control |
| `PRODUCTION_EXECUTOR` | Production Executor | Assigned production-task execution |

### Validation

- `code` is required, uppercase, and unique.
- User creation can assign any supported code, including `ADMIN`.

## Entity: Superadmin Bootstrap

Represents the startup process that guarantees an initial administrator path.

### Inputs

- Environment type: local vs production-like.
- Optional operator-provided superadmin login.
- Optional operator-provided superadmin display name.
- Optional operator-provided superadmin password.

### Rules

- If at least one enabled user has `ADMIN`, bootstrap skips creation.
- Local development may create documented default `admin` credentials for convenience.
- Production-like environments must create a superadmin only from secure operator-provided credentials.
- Production-like startup must fail with a clear configuration error when no `ADMIN` user exists and secure credentials are missing.
- Repeated bootstrap runs must not create duplicates.

## Entity: User Creation Request

Represents an administrator action to create a user.

### Fields

- `login`: requested login.
- `displayName`: requested display name.
- `initialPassword`: initial password entered by administrator.
- `roleCodes`: selected role codes.

### Validation Results

- `Success`: user created and appears in list/search.
- `Forbidden`: actor lacks `ADMIN`.
- `LoginAlreadyExists`: normalized login already belongs to another account.
- `ValidationError`: required field missing or initial password empty.
- `InvalidRoles`: one or more role codes are unknown or unsupported.

## Entity: User Summary

Represents user data visible to administrators in list/search results.

### Fields

- `id`
- `login`
- `displayName`
- `roles`: assigned roles with code and display name.

### Privacy Rules

- Never includes password or password hash.
- Visible only to administrators.

## Entity: Administrative Audit Event

Represents traceability for privileged user administration.

### Events

- Superadmin seeded.
- Superadmin bootstrap skipped because an administrator already exists.
- User created by administrator.
- User creation rejected due to forbidden access may be captured in security logs or audit if existing patterns support it.

### Details

- Acting administrator.
- Created user's login or identifier.
- Assigned role codes.
- Timestamp.
- Outcome.
- Must not include password values.

## State Transitions

### User Account

```text
absent -> created(enabled=true, roles assigned)
```

No edit, disable, delete, password reset, or first-login password rotation transitions are in scope for this increment.

### Superadmin Bootstrap

```text
no ADMIN + local -> default local admin created
no ADMIN + production-like + secure credentials -> configured admin created
no ADMIN + production-like + missing credentials -> startup failure
ADMIN exists -> skip creation
```
