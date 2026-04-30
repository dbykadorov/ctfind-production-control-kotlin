# REST Contract: User Administration

All endpoints are API-only JSON endpoints protected by JWT Bearer authentication.

## Common Types

### RoleSummary

```json
{
  "code": "WAREHOUSE",
  "name": "Warehouse"
}
```

### UserSummary

```json
{
  "id": "uuid",
  "login": "warehouse.demo",
  "displayName": "Warehouse Demo",
  "roles": [
    { "code": "WAREHOUSE", "name": "Warehouse" }
  ]
}
```

### ErrorResponse

```json
{
  "code": "duplicate_login",
  "message": "User login already exists"
}
```

## GET /api/users

Searches users visible to administrators. Existing endpoint behavior is expanded so every returned user includes assigned roles.

### Authorization

- Requires authentication.
- Requires `ADMIN` in JWT roles.

### Query Parameters

| Name | Required | Rules |
|------|----------|-------|
| `search` | no | Trimmed substring matched against login or display name |
| `limit` | no | Capped to `1..100`; default `50` |

### Success: 200

```json
[
  {
    "id": "30c3b5ca-15e3-4e53-9fbb-f0afcd947cf0",
    "login": "warehouse.demo",
    "displayName": "Warehouse Demo",
    "roles": [
      { "code": "WAREHOUSE", "name": "Warehouse" }
    ]
  }
]
```

### Errors

| Status | Code | Meaning |
|--------|------|---------|
| 401 | `unauthorized` | Missing or invalid JWT |
| 403 | `forbidden` | Authenticated user is not `ADMIN` |

## POST /api/users

Creates an enabled user with one or more supported roles.

### Authorization

- Requires authentication.
- Requires `ADMIN` in JWT roles.

### Request

```json
{
  "login": "warehouse.demo",
  "displayName": "Warehouse Demo",
  "initialPassword": "demo",
  "roleCodes": ["WAREHOUSE"]
}
```

### Validation

- `login`: required after trim; normalized for uniqueness.
- `displayName`: required after trim.
- `initialPassword`: required and non-empty.
- `roleCodes`: required, non-empty, all values must be supported role codes.
- `ADMIN` is allowed in `roleCodes`.

### Success: 201

```json
{
  "id": "30c3b5ca-15e3-4e53-9fbb-f0afcd947cf0",
  "login": "warehouse.demo",
  "displayName": "Warehouse Demo",
  "roles": [
    { "code": "WAREHOUSE", "name": "Warehouse" }
  ]
}
```

### Errors

| Status | Code | Meaning |
|--------|------|---------|
| 400 | `validation_error` | Required field missing or initial password empty |
| 400 | `invalid_roles` | Unknown or unsupported role code supplied |
| 401 | `unauthorized` | Missing or invalid JWT |
| 403 | `forbidden` | Authenticated user is not `ADMIN` |
| 409 | `duplicate_login` | Normalized login already exists |

### Security Notes

- Password and password hash are never returned.
- User creation records an audit event without password values.

## GET /api/users/roles

Returns the supported role catalog for the user creation form.

### Authorization

- Requires authentication.
- Requires `ADMIN` in JWT roles.

### Success: 200

```json
[
  { "code": "ADMIN", "name": "Administrator" },
  { "code": "ORDER_MANAGER", "name": "Order Manager" },
  { "code": "WAREHOUSE", "name": "Warehouse" },
  { "code": "PRODUCTION_SUPERVISOR", "name": "Production Supervisor" },
  { "code": "PRODUCTION_EXECUTOR", "name": "Production Executor" }
]
```

### Errors

| Status | Code | Meaning |
|--------|------|---------|
| 401 | `unauthorized` | Missing or invalid JWT |
| 403 | `forbidden` | Authenticated user is not `ADMIN` |

## Bootstrap Behavior Contract

### Local Development

- If no `ADMIN` user exists, local startup may create the documented local administrator account.
- Re-running startup must not create duplicate administrators.

### Production-Like Environments

- If an enabled `ADMIN` user exists, startup skips superadmin creation.
- If no enabled `ADMIN` user exists and secure superadmin credentials are present, startup creates exactly one superadmin.
- If no enabled `ADMIN` user exists and secure superadmin credentials are missing, startup fails with a clear configuration error before the application is considered ready.
