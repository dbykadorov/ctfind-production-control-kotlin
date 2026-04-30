# REST Contract: Edit Existing Users

All endpoints are API-only JSON endpoints protected by JWT Bearer authentication.

## Common Types

### RoleSummary

```json
{
  "code": "PRODUCTION_SUPERVISOR",
  "name": "Production Supervisor"
}
```

### UserSummary

```json
{
  "id": "30c3b5ca-15e3-4e53-9fbb-f0afcd947cf0",
  "login": "supervisor.demo",
  "displayName": "Supervisor Demo",
  "roles": [
    { "code": "PRODUCTION_SUPERVISOR", "name": "Production Supervisor" }
  ]
}
```

### ErrorResponse

```json
{
  "code": "last_admin_role_removal_forbidden",
  "message": "Cannot remove ADMIN role from the last active administrator"
}
```

## PUT /api/users/{userId}

Updates editable fields of an existing user profile.

### Authorization

- Requires authentication.
- Requires `ADMIN` role in JWT.

### Path Parameters

| Name | Required | Rules |
|------|----------|-------|
| `userId` | yes | UUID of existing user |

### Request

```json
{
  "displayName": "Updated Supervisor",
  "roleCodes": ["PRODUCTION_SUPERVISOR", "WAREHOUSE"]
}
```

### Validation

- `displayName`: required after trim.
- `roleCodes`: required, non-empty after normalization.
- `roleCodes`: duplicates are allowed in input but normalized as a unique set.
- Every `roleCode` must exist in supported role catalog.
- Update must not remove `ADMIN` from the last active administrator.
- `login` is immutable and not accepted by this endpoint.

### Success: 200

```json
{
  "id": "30c3b5ca-15e3-4e53-9fbb-f0afcd947cf0",
  "login": "supervisor.demo",
  "displayName": "Updated Supervisor",
  "roles": [
    { "code": "PRODUCTION_SUPERVISOR", "name": "Production Supervisor" },
    { "code": "WAREHOUSE", "name": "Warehouse" }
  ]
}
```

### Errors

| Status | Code | Meaning |
|--------|------|---------|
| 400 | `validation_error` | Empty `displayName` or empty role set |
| 400 | `invalid_roles` | Unknown or unsupported role code |
| 401 | `unauthorized` | Missing or invalid JWT |
| 403 | `forbidden` | Authenticated user without `ADMIN` |
| 404 | `user_not_found` | User no longer exists |
| 409 | `last_admin_role_removal_forbidden` | Attempt to remove `ADMIN` from last active admin |

### Security Notes

- Passwords and password hashes are never accepted or returned by this endpoint.
- Successful updates must write an audit event with actor, target, and changed fields only.
- Audit details must not contain secrets (password/hash/token).

## Related Existing Endpoints

The edit flow reuses existing user-management endpoints from the previous feature:

- `GET /api/users` for list/search refresh.
- `GET /api/users/roles` for role selection catalog.
