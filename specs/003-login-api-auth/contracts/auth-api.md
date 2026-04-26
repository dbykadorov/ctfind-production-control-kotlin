# Contract: Authentication API

## General Rules

- Base path: `/api/auth`
- Request/response media type: `application/json`
- Protected endpoints require `Authorization: Bearer <token>`
- Backend remains API-only: no browser form login, no HTTP Basic challenge, no backend-rendered login page
- Error responses must be generic enough to avoid revealing whether a login exists

## Error Response Shape

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "Invalid login or password"
}
```

Required fields:

- `code`: stable machine-readable error code.
- `message`: user-readable message safe for display.

## POST /api/auth/login

Authenticate credentials and issue an 8-hour Bearer JWT.

**Authentication**: public.

**Request**:

```json
{
  "login": "admin",
  "password": "admin"
}
```

**Validation**:

- `login` is required and must not be blank after trimming.
- `password` is required and must not be blank.

**Success response: 200**:

```json
{
  "tokenType": "Bearer",
  "accessToken": "<jwt>",
  "expiresAt": "2026-04-27T02:04:00Z",
  "user": {
    "login": "admin",
    "displayName": "Local Administrator",
    "roles": ["ADMIN"]
  }
}
```

**Failure responses**:

- `400 AUTH_VALIDATION_FAILED`: login or password is blank.
- `401 AUTH_INVALID_CREDENTIALS`: login/password is invalid, or the account cannot authenticate.
- `429 AUTH_THROTTLED`: repeated failed attempts for the same login/IP are temporarily throttled.

**Audit behavior**:

- `LOGIN_SUCCESS` on success.
- `LOGIN_FAILURE` on invalid credentials, disabled account, operational authentication failure, and throttled attempts.
- Audit details must not include the submitted password or token value.

## GET /api/auth/me

Return the authenticated user represented by the Bearer token.

**Authentication**: required.

**Success response: 200**:

```json
{
  "login": "admin",
  "displayName": "Local Administrator",
  "roles": ["ADMIN"],
  "expiresAt": "2026-04-27T02:04:00Z"
}
```

**Failure responses**:

- `401 AUTH_UNAUTHORIZED`: missing, invalid, or expired Bearer token.
- `403 AUTH_FORBIDDEN`: authenticated token is valid but lacks required access for a protected resource.

## POST /api/auth/logout

End the frontend authenticated state and record logout traceability.

**Authentication**: required.

**Request**: empty JSON body or no body.

**Success response: 204**: no response body.

**Failure responses**:

- `401 AUTH_UNAUTHORIZED`: missing, invalid, or expired Bearer token.

**Audit behavior**:

- `LOGOUT` event is recorded for valid authenticated logout.
- The frontend must remove the stored token regardless of whether the network request succeeds.

## Protected API Behavior

All protected backend routes outside explicitly public endpoints must return:

- `401` when no valid Bearer token is present.
- `403` when the user is authenticated but not allowed.

They must not return HTML login pages or `WWW-Authenticate: Basic` browser challenges.
