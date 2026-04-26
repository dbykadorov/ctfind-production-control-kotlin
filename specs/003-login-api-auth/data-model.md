# Data Model: Login API Authentication

## Overview

This feature introduces the minimal authentication model needed for local cabinet login. The model is intentionally small but must remain compatible with future ERP roles, permissions, audit, and TOC-related accountability.

## Entity: User Account

Represents an application-owned identity that can authenticate and hold cabinet roles.

**Fields**:

- `id`: UUID primary key.
- `login`: unique normalized login used for sign-in. Required.
- `displayName`: human-readable name shown in the cabinet. Required for seeded admin.
- `passwordHash`: BCrypt hash of the current password. Required.
- `enabled`: whether the account can authenticate.
- `createdAt`: timestamp when the account was created.
- `updatedAt`: timestamp when account metadata or credential hash was changed.

**Validation rules**:

- `login` is trimmed, normalized to a stable case, non-empty, and unique.
- Raw passwords are never stored.
- Disabled users cannot receive a JWT.

**Relationships**:

- Has many `Role Assignment` records.
- Has many `Authentication Audit Event` records when the user is known.

## Entity: Role

Represents a named application role used by authorization and cabinet access.

**Fields**:

- `id`: UUID primary key.
- `code`: unique stable role code.
- `name`: display name.
- `createdAt`: timestamp when the role was created.

**Initial roles**:

- `ADMIN`: grants enough access for the local bootstrap administrator to enter protected cabinet routes.

**Validation rules**:

- `code` is required, unique, and uppercase snake-case.
- Role names are not used as security decisions directly; authorization uses stable codes.

## Entity: Role Assignment

Connects a user account to a role.

**Fields**:

- `userId`: references `User Account`.
- `roleId`: references `Role`.
- `createdAt`: timestamp when the role was assigned.

**Validation rules**:

- A user/role pair is unique.
- Assignments cannot reference missing users or roles.

## Entity: Local Seed Administrator

The bootstrap user created for local development.

**Identity**:

- `login`: `admin`
- Initial password: `admin`
- Required role: `ADMIN`

**State rules**:

- If no `admin` account exists during local startup, create the user, hash the initial password, assign `ADMIN`, and record a seed audit event.
- If the `admin` account already exists, do not duplicate it and do not overwrite its password hash.
- If the `ADMIN` role is missing during local startup, create it idempotently.

## Entity: Bearer JWT Token

A signed token issued after successful authentication. Tokens are not persisted as rows in this slice.

**Claims**:

- `sub`: user login or user id stable enough for backend lookup.
- `roles`: role codes for cabinet authorization.
- `iat`: issued-at timestamp.
- `exp`: expiration timestamp exactly 8 hours after successful login.

**Validation rules**:

- Expired tokens are rejected.
- Tokens with invalid signatures are rejected.
- Protected APIs require a valid Bearer token.

## Entity: Authenticated Session

Frontend state derived from a valid Bearer JWT and associated user payload.

**Fields**:

- `accessToken`: JWT stored in browser `localStorage`.
- `tokenType`: `Bearer`.
- `expiresAt`: timestamp supplied by the backend.
- `user.login`: authenticated login.
- `user.displayName`: display name.
- `user.roles`: role codes.

**State transitions**:

```text
Unauthenticated
  -> login success -> Authenticated
Authenticated
  -> logout -> Unauthenticated
Authenticated
  -> token removed -> Unauthenticated
Authenticated
  -> token expired/rejected -> Unauthenticated with session-expired UI
Unauthenticated
  -> login failure/throttle -> Unauthenticated
```

## Entity: Authentication Audit Event

Durable trace record for authentication-related outcomes.

**Fields**:

- `id`: UUID primary key.
- `eventType`: one of `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `LOGOUT`, `LOCAL_SEED`.
- `outcome`: stable outcome code such as `SUCCESS`, `INVALID_CREDENTIALS`, `THROTTLED`, `DISABLED`, `SEEDED`, `SKIPPED_EXISTING`.
- `login`: submitted or affected login when safe to store.
- `userId`: user reference when known.
- `requestIp`: request network source when available.
- `userAgent`: request user agent when available.
- `occurredAt`: timestamp of the event.
- `details`: optional non-sensitive JSON/text details.

**Validation rules**:

- Must not store submitted passwords.
- Must not store full JWT values.
- Login failure events may have no `userId` when login is unknown.
- Seed events may have no request IP/user agent.

## Entity: Login Throttle Bucket

Runtime-only structure for simple repeated-failure throttling.

**Fields**:

- `key`: normalized login plus request IP.
- `failureCount`: number of recent failures.
- `windowStartedAt`: start of current failure window.
- `throttledUntil`: timestamp until which attempts are rejected.

**Validation rules**:

- Empty or whitespace login attempts are rejected by validation before credential verification and do not need a throttle bucket.
- Successful login clears the bucket for the same login/IP.
- Throttled attempts do not create authenticated state.

## Suggested Tables

```text
app_user
app_role
app_user_role
auth_audit_event
```

The throttle bucket is intentionally not a table for this local MVP.
