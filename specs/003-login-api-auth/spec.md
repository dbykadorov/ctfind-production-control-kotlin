# Feature Specification: Login API Authentication

**Feature Branch**: `003-login-api-auth`  
**Created**: 2026-04-26  
**Status**: Draft  
**Input**: User description: "теперь нам надо реализовать апи для логина, базу данных, сид базы с юзером admin / admin и возможность залогиниться через фронтэнд"

## Clarifications

### Session 2026-04-26

- Q: How should the frontend hold authenticated state after successful login? → A: Bearer JWT token.
- Q: Where should the frontend store the JWT for this slice? → A: Store JWT in `localStorage`;
  survives refresh and browser restart.
- Q: What token lifetime should the bootstrap login use? → A: 8 hours.
- Q: Which authentication events must be audited in this slice? → A: Login success, login failure,
  logout, and seed activity.
- Q: Should repeated failed login attempts be limited in this slice? → A: Simple throttle after
  repeated failures for the same login/IP.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sign in with seeded local administrator (Priority: P1)

A developer starts a fresh local environment, opens the cabinet login screen, enters the documented
local administrator credentials `admin` / `admin`, and reaches the protected cabinet area as an
authenticated administrator.

**Why this priority**: this is the smallest complete authentication slice. It replaces the current
placeholder login with app-owned authentication and proves that the migrated frontend can use the
new platform instead of the old Frappe runtime.

**Independent Test**: starting from an empty local environment, the tester opens the cabinet login
screen, submits `admin` / `admin`, and sees an authenticated cabinet page instead of the placeholder
message.

**Acceptance Scenarios**:

1. **Given** a fresh local installation with no manually created users, **When** the local platform
   is started, **Then** an administrator account with login `admin` and password `admin` is available
   for local development sign-in.
2. **Given** the cabinet login screen is open, **When** the developer enters `admin` / `admin` and
   submits the form, **Then** the user becomes authenticated and is routed to the cabinet area.
3. **Given** the local platform is restarted without clearing data, **When** startup seed runs again,
   **Then** it does not create duplicate administrator accounts or change an already customized
   account unexpectedly.

---

### User Story 2 - Protect cabinet routes with app-owned authentication (Priority: P2)

An unauthenticated user who opens a protected cabinet route is sent to the login screen. After a
successful sign-in, the user returns to the originally intended safe cabinet route.

**Why this priority**: route protection makes the login useful for real cabinet navigation rather than
just proving credentials on a standalone page.

**Independent Test**: opening a protected cabinet route while unauthenticated shows login; signing in
with `admin` / `admin` returns the user to the protected route.

**Acceptance Scenarios**:

1. **Given** an unauthenticated user opens `/cabinet/orders`, **When** the cabinet handles navigation,
   **Then** the user sees the login screen and the intended route is preserved for return after
   successful sign-in.
2. **Given** a preserved return route exists, **When** the user signs in successfully, **Then** the
   cabinet opens that safe route instead of dropping the user at an unrelated page.
3. **Given** the user is authenticated, **When** the user refreshes the cabinet page, **Then** the
   cabinet still recognizes the authenticated state until logout or session expiration.

---

### User Story 3 - Handle failed sign-in safely and traceably (Priority: P3)

An unauthenticated user enters wrong credentials or submits an incomplete form and receives a clear
failure message without gaining access. Administrators and developers can later inspect enough
traceability to understand authentication outcomes.

**Why this priority**: authentication is a security boundary. Failures must be predictable, safe, and
auditable before the system grows into broader ERP roles and permissions.

**Independent Test**: submitting wrong credentials never creates an authenticated state, shows a clear
message, and records the attempt outcome for troubleshooting and audit.

**Acceptance Scenarios**:

1. **Given** the login form has empty required fields, **When** the user submits it, **Then** the user
   remains on the login screen and sees a clear validation message.
2. **Given** the user enters a wrong password, **When** the user submits the form, **Then** no
   authenticated state is created and a generic sign-in failure message is shown.
3. **Given** a sign-in attempt succeeds or fails, **When** the attempt completes, **Then** the system
   retains trace information sufficient to distinguish success, invalid credentials, and operational
   failure without exposing sensitive credential data.

---

### Edge Cases

- Startup seed runs multiple times against an existing local database.
- The default administrator password has already been changed in local data.
- A user submits empty credentials, whitespace credentials, or a wrong password.
- A user repeatedly submits wrong credentials for the same login or from the same network source.
- A user opens a protected route directly before authentication.
- A previously authenticated user reloads the browser or opens a second tab.
- A session expires or is no longer recognized by the platform.
- The old Frappe runtime is stopped or unavailable.
- The backend receives unauthenticated requests outside the frontend flow.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST own and persist user accounts needed for cabinet authentication.
- **FR-002**: System MUST create a local development administrator account with login `admin` and
  initial password `admin` when local data is initialized.
- **FR-003**: Startup seeding MUST be idempotent and MUST NOT create duplicate administrator accounts.
- **FR-004**: The local administrator MUST have enough permissions to enter the cabinet protected area.
- **FR-005**: The login screen MUST submit credentials to the new platform authentication flow, not to
  the old Frappe runtime.
- **FR-006**: Successful login with valid credentials MUST issue a Bearer JWT token that is recognized
  by both protected frontend routes and protected backend requests.
- **FR-007**: Failed login MUST NOT create an authenticated state and MUST show a generic, user-readable
  failure message.
- **FR-008**: Empty required fields MUST be rejected before authentication is attempted.
- **FR-009**: Unauthenticated access to protected cabinet routes MUST redirect to the login screen.
- **FR-010**: Successful login after a protected-route redirect MUST return the user only to a safe
  preserved cabinet route.
- **FR-011**: The frontend MUST store the Bearer JWT in browser `localStorage` for this slice so
  authenticated state survives normal page refresh and browser restart.
- **FR-012**: System MUST provide a way for the authenticated user to end the authenticated state, and
  logout MUST remove the stored token from browser `localStorage`.
- **FR-013**: Bearer JWT tokens issued by this feature MUST expire 8 hours after successful login.
- **FR-014**: Authentication audit MUST record login success, login failure, logout, and local seed
  activity without storing or exposing submitted password values in audit records or logs.
- **FR-015**: Repeated failed login attempts for the same login or network source MUST trigger a
  simple temporary throttle before another attempt is accepted.
- **FR-016**: Throttled login attempts MUST NOT create an authenticated state and MUST show a generic,
  user-readable retry-later message.
- **FR-017**: Protected backend access MUST remain API-only and return explicit unauthorized or forbidden
  outcomes instead of browser login pages or Basic Auth challenges.
- **FR-018**: The local run documentation MUST describe how to start the platform, sign in with the
  seeded local administrator, test a failed login, and stop/reset local data.
- **FR-019**: The default `admin` / `admin` credential MUST be treated as a local development bootstrap
  credential and MUST NOT be presented as a production-ready security setup.

### Key Entities *(include if feature involves data)*

- **User Account**: an application-owned identity that can authenticate and hold cabinet permissions.
- **Credential**: the protected verification data associated with a user account; raw submitted
  passwords are never business records.
- **Role/Permission Assignment**: the relationship that determines which cabinet areas and operations
  an authenticated user may access.
- **Bearer JWT Token**: an authentication token issued after successful sign-in, valid for 8 hours,
  stored in browser `localStorage` for this slice, and used by the frontend when accessing protected
  backend operations.
- **Authenticated Session**: the frontend user's authenticated state derived from a valid Bearer JWT
  token and cleared by logout, token removal, or token expiration.
- **Authentication Audit Event**: a trace record for login success, login failure, logout, and local
  seed activity, excluding sensitive credential values.
- **Local Seed Administrator**: the bootstrap administrator account created for local development use.

### Constitution Alignment *(mandatory)*

- **ERP/domain fit**: Establishes the user, credential, role, and session foundation required before
  orders, production tasks, inventory, audit, and notifications can be operated securely.
- **TOC readiness**: Does not add production-flow facts directly, but it enables accountable user
  actions that future TOC workflows will need for task ownership, bottleneck decisions, and audit.
- **Traceability/audit**: Login success, login failure, logout, and seed activity must be traceable
  without exposing submitted passwords.
- **Security/API boundary**: Replaces the frontend placeholder with explicit app-owned authentication
  while preserving the API-only backend rule and avoiding browser-native login challenges. Token
  persistence in `localStorage` is accepted for this local MVP and must be revisited before production
  hardening.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer can start a fresh local environment and sign in as `admin` within 5 minutes
  after services report readiness.
- **SC-002**: In 100% of tested local fresh installs, exactly one local administrator account is
  available after startup seeding.
- **SC-003**: 100% of valid `admin` / `admin` login attempts in a fresh local environment reach a
  protected cabinet page instead of the login placeholder message.
- **SC-004**: 100% of tested wrong-password attempts remain on the login screen and do not create
  authenticated access.
- **SC-005**: 100% of tested unauthenticated protected-route visits show the login screen and preserve
  a safe return route.
- **SC-006**: Browser-native login popups and backend-rendered login pages appear in 0 tested
  authentication flows.
- **SC-007**: Authentication attempt trace records distinguish success and failure outcomes for all
  tested sign-in attempts without exposing submitted password values.
- **SC-008**: 100% of tested authenticated browser refreshes and browser restarts retain access while
  the stored token remains valid, and 100% of tested logout actions remove the stored token.
- **SC-009**: 100% of tested expired-token attempts after the 8-hour validity window are rejected and
  require a new successful sign-in.
- **SC-010**: 100% of tested login success, login failure, logout, and local seed actions produce
  trace records that identify the outcome type without containing submitted password values.
- **SC-011**: 100% of tested repeated failed login attempts for the same login or network source are
  throttled temporarily and do not create authenticated access.

## Assumptions

- The `admin` / `admin` account is a local development bootstrap account only.
- JWT persistence in `localStorage` is accepted for this development-focused slice and will need a
  later production security review.
- Production-grade password policy, password reset, user management screens, and multi-factor
  authentication are separate future features.
- The first authenticated cabinet page can be an existing migrated cabinet route, even if its business
  data remains unavailable until later ERP feature slices.
- The old Frappe login endpoint remains out of scope and must not be used for this feature.
- The local Docker workflow from earlier features is the expected validation environment.
