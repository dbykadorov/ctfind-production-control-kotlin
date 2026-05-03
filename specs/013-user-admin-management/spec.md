# Feature Specification: Superadmin Seed and User Management

**Feature Branch**: `013-user-admin-management`  
**Created**: 2026-04-30  
**Status**: Accepted  
**Input**: User description: "Close the Phase 01 gap by ensuring a primary superadmin exists and giving ADMIN users a cabinet section for listing, searching, creating users, and assigning roles."

## Clarifications

### Session 2026-04-30

- Q: What should happen in a production-like environment when no ADMIN exists and secure superadmin credentials are missing? → A: Fail startup with a clear operator-facing configuration error.
- Q: Can an ADMIN create another user with administrator privileges? → A: ADMIN can create users with any supported role, including ADMIN.
- Q: Should newly created users be forced to change the entered password on first login? → A: No forced first-login password change in this increment; the entered value is an initial password.
- Q: What is the minimum password policy for this increment? → A: Non-empty only, while leaving room for stricter password policy in future increments.
- Q: Should the user list and search results show assigned roles for each user? → A: User list and search results show assigned roles for each user.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Initial Superadmin Access (Priority: P1)

As an operator deploying or resetting the system, I need a guaranteed and safe way to obtain the first administrator account so that I can configure access without manual database intervention.

**Why this priority**: No other user-management work is useful if the first administrator cannot sign in and bootstrap the system.

**Independent Test**: Start from an environment with no administrator account and verify that the documented bootstrap path creates or exposes exactly one usable administrator account.

**Acceptance Scenarios**:

1. **Given** a local development environment with no administrator account, **When** the system is initialized, **Then** a documented local superadmin can sign in and access the cabinet.
2. **Given** a production-like environment with no administrator account, **When** the system is initialized with operator-provided superadmin credentials, **Then** a superadmin account is available without using any hardcoded default password.
3. **Given** a production-like environment with no administrator account and no secure operator-provided superadmin credentials, **When** the system is initialized, **Then** startup fails with a clear operator-facing configuration error.
4. **Given** an environment where an administrator already exists, **When** the system is initialized again, **Then** no duplicate superadmin account is created and existing administrator access remains unchanged.

---

### User Story 2 - Admin Views Users (Priority: P2)

As a superadmin, I want to open a "Users" section in the cabinet and search existing users so that I can understand who already has access to the system.

**Why this priority**: Visibility is required before safe creation and role assignment, and it validates that the section is protected for administrators.

**Independent Test**: Sign in as an administrator and as a non-administrator, then verify that only the administrator can see and open the "Users" section and search users.

**Acceptance Scenarios**:

1. **Given** a signed-in administrator, **When** the administrator opens the cabinet navigation, **Then** the "Users" section is visible.
2. **Given** a signed-in administrator, **When** they open the "Users" section, **Then** they see a searchable list of existing users with assigned roles shown for each user.
3. **Given** a signed-in non-administrator, **When** they open cabinet navigation or directly request the "Users" section, **Then** the section is unavailable and access is denied or redirected to a forbidden state.

---

### User Story 3 - Admin Creates Users with Roles (Priority: P3)

As a superadmin, I want to create a user with a login, display name, initial password, and one or more roles so that staff can sign in and access the cabinet sections relevant to their responsibilities.

**Why this priority**: This is the core business value of the feature after bootstrap and protected visibility are in place.

**Independent Test**: Create a warehouse user from the "Users" section, sign out, sign in as the created user with the initial password, and verify that the available cabinet sections match the assigned role.

**Acceptance Scenarios**:

1. **Given** a signed-in administrator in the "Users" section, **When** they submit a valid new user with one or more roles, **Then** the user is created and appears in the user list.
2. **Given** a newly created user, **When** that user signs in with the initial password, **Then** they can access only the cabinet sections allowed by their assigned roles.
3. **Given** a signed-in administrator, **When** they attempt to create a user with a duplicate login, **Then** creation is rejected with a clear conflict message and no duplicate account is created.

---

### User Story 4 - Admin-Only User Creation (Priority: P4)

As a system owner, I want user creation to be restricted to administrators so that non-administrators cannot elevate access for themselves or others.

**Why this priority**: The feature changes access control and must preserve the security boundary even if users attempt direct or unsupported access paths.

**Independent Test**: Attempt user creation as an unauthenticated user, a non-administrator, and an administrator; verify that only the administrator succeeds.

**Acceptance Scenarios**:

1. **Given** an unauthenticated user, **When** they attempt to create a user, **Then** the system requires authentication.
2. **Given** a signed-in non-administrator, **When** they attempt to create a user, **Then** the system denies access.
3. **Given** a signed-in administrator, **When** they create a valid user, **Then** the system accepts the request and never exposes the user's password after creation.

---

### Edge Cases

- A user is created with a login that already exists.
- A user is created with an empty login or empty display name.
- A user is created with an empty initial password.
- A user is created with no roles.
- A user is created with an unknown or unsupported role.
- The first superadmin bootstrap runs more than once.
- The first superadmin bootstrap runs when no role catalog is available.
- A superadmin creates another user with administrator privileges; this is allowed and remains subject to administrator-only access and audit expectations.
- A user is created with roles that do not grant any cabinet section.
- A non-administrator attempts to discover or use the user-management section directly.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide an idempotent bootstrap path that ensures a primary administrator account can exist when no administrator account is present.
- **FR-002**: The system MUST NOT create a production-like superadmin account with hardcoded default credentials.
- **FR-003**: Local development environments MAY provide a documented default administrator account for developer convenience.
- **FR-004**: The system MUST not create duplicate superadmin accounts when the bootstrap process runs repeatedly.
- **FR-004a**: In production-like environments, if no administrator exists and secure superadmin credentials are not provided, the system MUST fail startup with a clear operator-facing configuration error.
- **FR-005**: Only users with administrator privileges MUST be able to view the user-management section.
- **FR-006**: Only users with administrator privileges MUST be able to search and list users from the user-management section.
- **FR-006a**: User list and search results MUST show each user's assigned roles.
- **FR-007**: Only users with administrator privileges MUST be able to create users.
- **FR-008**: The user-management section MUST allow administrators to enter a login, display name, initial password, and at least one role for a new user.
- **FR-009**: The system MUST validate that the login is present and unique before creating a user.
- **FR-010**: The system MUST validate that the display name is present before creating a user.
- **FR-011**: The system MUST validate that the initial password is present and non-empty before creating a user.
- **FR-011a**: The system MUST keep the password validation model extensible so stricter password policy can be added in a future increment without changing the user-creation flow.
- **FR-012**: The system MUST validate that every selected role exists in the supported role catalog before creating a user.
- **FR-013**: The system MUST reject creation of a user with no roles.
- **FR-013a**: Administrators MUST be able to assign any supported role during user creation, including administrator privileges.
- **FR-014**: The system MUST store user passwords only in protected form and MUST never display or return a password after creation.
- **FR-014a**: The system MUST NOT require newly created users to change the initial password on first login in this increment.
- **FR-015**: A newly created user MUST be able to sign in with the assigned login and initial password.
- **FR-016**: A newly created user's cabinet access MUST reflect the roles assigned during creation.
- **FR-017**: The system MUST show clear, user-actionable errors for duplicate login, invalid roles, missing fields, empty initial password, unauthenticated access, and forbidden access.
- **FR-018**: The system SHOULD record a traceable administrative event when an administrator creates a user.
- **FR-019**: The administrator manual MUST be updated to describe the new user-management capability and the bootstrap behavior.
- **FR-020**: A quick manual verification scenario MUST cover creating a role-scoped user and signing in as that user.

### Key Entities *(include if feature involves data)*

- **Superadmin**: The first administrator-capable user used to bootstrap access management. It has administrator privileges and is created only through the documented bootstrap path.
- **User**: A person who can sign in to the cabinet. Key attributes are login, display name, enabled status, and assigned roles. Assigned roles are visible to administrators in user list and search results. Passwords are never visible after creation.
- **Role**: A permission grouping that controls which cabinet sections and operations a user can access. Supported initial role identifiers are Administrator, Order Manager, Warehouse, Production Supervisor, and Production Executor.
- **User Creation Request**: The administrator's intent to create a user, including login, display name, initial password, and selected roles.
- **Administrative Audit Event**: A traceable record that a privileged user created another user.

### Constitution Alignment *(mandatory)*

- **ERP/domain fit**: Strengthens Phase 01 operational access control by allowing administrators to onboard order, warehouse, and production staff without manual data changes.
- **TOC readiness**: Indirectly supports operational flow by enabling the correct people to access order, warehouse, and production work areas; no new throughput or buffer facts are introduced by this feature.
- **Traceability/audit**: User creation is a security-sensitive business-state change and should be traceable to the acting administrator, timestamp, and created user.
- **Security/API boundary**: The feature expands access administration and must preserve the administrator-only boundary for user listing and creation, with clear unauthenticated and unauthorized outcomes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In a fresh local environment, an administrator can sign in and open the "Users" section within 5 minutes of startup using documented steps.
- **SC-002**: An administrator can create a role-scoped user from the cabinet in under 2 minutes.
- **SC-003**: 100% of attempted user-creation actions by non-administrators are denied during acceptance testing.
- **SC-004**: 100% of created users can sign in with their assigned initial password and see cabinet sections consistent with their assigned roles during manual verification.
- **SC-005**: Re-running initialization 3 consecutive times creates no duplicate superadmin users.
- **SC-006**: Duplicate login, missing required fields, and unsupported roles each produce a clear error that identifies what the administrator must correct.
- **SC-007**: In production-like verification, missing secure bootstrap credentials with no existing administrator produces a clear startup failure before users can access the system.
- **SC-008**: During manual verification, 100% of user search results display the assigned roles for each returned user.

## Assumptions

- Administrator privilege means the existing administrator role used for audit and user search.
- The first production-like superadmin is created only when secure operator-provided credentials are available; otherwise startup fails with a clear operator-facing configuration error when no administrator exists.
- Local development may continue to use a documented default account for convenience.
- The initial user-management release includes listing, search, and creation only; editing, deletion, disabling, password reset, self-registration, external identity providers, and bulk import remain out of scope.
- The supported role catalog for this feature is Administrator, Order Manager, Warehouse, Production Supervisor, and Production Executor; administrators may create users with any of these roles, including Administrator.
- A new user must have at least one role because users without roles cannot complete useful cabinet work in Phase 01.
- First-login password rotation is out of scope for this increment; administrators communicate initial passwords through an operational process outside the system.
- The initial password policy for this increment is non-empty only; future increments may add length or composition rules.
