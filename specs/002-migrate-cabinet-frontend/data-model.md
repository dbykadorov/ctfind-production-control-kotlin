# Data Model: Migrate Cabinet Frontend

This feature does not introduce new business-domain persistence. The model describes frontend
runtime objects and migration boundaries needed to make the existing cabinet app run independently
from Frappe.

## Entity: Frontend Cabinet App

Represents the migrated SPA source owned by the new project.

### Fields

- `sourcePath`: `frontend/cabinet/`.
- `entryPoint`: frontend application entry point.
- `routes`: public and protected frontend routes.
- `pages`: login, common pages, and office pages copied from the legacy cabinet.
- `components`: layout, domain, and UI components copied from the legacy cabinet.
- `styles`: global styles, tokens, fonts, and component styles.
- `assets`: SVGs, logos, and other static frontend assets.
- `tests`: existing unit/component tests retained where practical.

### Validation Rules

- Must not require files from the old Frappe project at runtime.
- Must start from the new repository Docker workflow.
- Must open the login screen without requiring old Frappe endpoints.
- Must preserve transferred source structure enough for future feature slices to continue migration.

## Entity: Login Screen

Represents the first visible frontend state for unauthenticated users.

### Fields

- `route`: documented local login route.
- `usernameField`: user-editable login/email field.
- `passwordField`: user-editable password field.
- `submitAction`: placeholder auth behavior.
- `errorState`: visible message explaining that new-platform authorization is not connected yet.
- `brandingAssets`: migrated organization/product logos and decorative background.
- `localeText`: login labels and messages from migrated i18n dictionaries.

### Validation Rules

- Must be accessible without authentication.
- Must render required assets without browser 404s.
- Must allow field input.
- Must not create an authenticated frontend session in this slice.
- Must remain on login after submit.

## Entity: Authentication Placeholder

Represents temporary auth behavior until the new backend auth feature exists.

### Fields

- `status`: always unauthenticated for this slice.
- `submitResult`: deterministic error or unavailable-auth outcome.
- `messageKey`: i18n key shown by the login page.
- `redirectTarget`: preserved safe return path for future auth integration.

### Validation Rules

- Must never report success.
- Must never call old Frappe login endpoint.
- Must not store a real session, token, or role set.
- Must produce a user-readable message.

## Entity: Docker Frontend Service

Represents the local container runtime for the migrated cabinet.

### Fields

- `serviceName`: stable service name in root compose workflow.
- `hostPort`: documented browser port.
- `containerPort`: frontend service port inside the container.
- `buildContext`: frontend cabinet directory.
- `startCommand`: command used by the container to serve the frontend.
- `healthCheck`: readiness signal used by Docker workflow.
- `logsCommand`: documented command for troubleshooting.

### Validation Rules

- Must start with the root Docker workflow.
- Must be independently observable through service status and logs.
- Must not require host Node or pnpm.
- Must not depend on old Frappe runtime to become healthy.

## Entity: Legacy Integration Boundary

Represents the copied modules that were previously tied to Frappe.

### Fields

- `authService`: old login/logout integration point.
- `apiClient`: old Frappe method call layer.
- `socketClient`: old realtime integration.
- `bootPayload`: old startup/session data assumptions.
- `migrationMode`: current behavior in this slice: disabled, stubbed, or retained-but-not-started.

### Validation Rules

- Must not block login screen rendering.
- Must not perform old Frappe login during placeholder auth.
- Must be visible enough for future API/auth planning.
- Must avoid noisy browser failures during first startup.

## State Transitions

### Frontend Runtime

```text
not_present -> copied -> configured -> running -> healthy
running -> failed
healthy -> stopped
```

### Login Placeholder

```text
idle -> editing -> submitting -> unavailable_error -> idle
```

No transition to `authenticated` exists in this feature.
