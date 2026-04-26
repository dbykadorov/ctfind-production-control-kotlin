# Contract: Cabinet Frontend Authentication

## Scope

The migrated cabinet frontend replaces the current placeholder login behavior with app-owned authentication against the Spring backend.

## Token Storage

- Storage key: `ctfind.cabinet.authToken`
- Storage medium: browser `localStorage`
- Token type: Bearer JWT
- Lifetime: backend-issued `expiresAt`, 8 hours after login

The storage key is local-MVP behavior and must be revisited before production hardening.

## Login Flow

1. User opens `/cabinet/login`.
2. User enters login and password.
3. `auth-service.ts` validates non-empty fields.
4. Frontend sends `POST /api/auth/login`.
5. On success:
   - store `accessToken` in `localStorage`;
   - update Pinia auth state with returned user and roles;
   - route to sanitized `from` query value or `/cabinet`.
6. On invalid credentials:
   - do not store token;
   - stay on login page;
   - show a generic invalid-login message.
7. On throttling:
   - do not store token;
   - stay on login page;
   - show a generic retry-later message.

## Refresh and Browser Restart

On application startup:

1. Read token from `localStorage`.
2. If absent, treat user as unauthenticated.
3. If present, attach it to `GET /api/auth/me`.
4. If `/api/auth/me` succeeds, populate auth state from the response.
5. If `/api/auth/me` returns `401`, clear the token and treat the session as expired.

## Logout Flow

1. User triggers logout.
2. Frontend calls `POST /api/auth/logout` with the Bearer token when available.
3. Frontend removes `ctfind.cabinet.authToken` even if the network call fails.
4. Pinia auth state is cleared.
5. User is routed to `/cabinet/login`.

## HTTP Client Behavior

The shared axios client must:

- attach `Authorization: Bearer <token>` when a token is present;
- avoid sending legacy Frappe CSRF assumptions to new auth endpoints;
- treat `401` from protected APIs as session expiration;
- preserve existing safe redirect behavior through `sanitizeFrom`.

## Route Guard Behavior

- Public route: `/cabinet/login`.
- Protected cabinet routes require `auth.isAuthenticated`.
- Unauthenticated visits to protected routes redirect to `/cabinet/login?from=<safe path>`.
- Authenticated visits to `/cabinet/login` redirect to sanitized `from` or `/cabinet`.
- Routes requiring roles use backend role codes returned from auth responses.

## I18n Message Keys

Minimum frontend error keys:

- `login.error.empty`: login or password is empty.
- `login.error.invalid`: invalid login or password.
- `login.error.rateLimit`: repeated failed attempts; retry later.
- `login.error.network`: backend unavailable or unexpected network error.
- `login.error.unavailable`: removed from the normal login path once this feature is implemented.
