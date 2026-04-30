# Frontend Contract: Cabinet User Editing

## Route and Access

| Route | Name | Access |
|-------|------|--------|
| `/cabinet/users` | `users.list` | Accessible only for admin-capable sessions |

No new route is required. Editing is part of the existing users page.

## Users List Extension

Each user row keeps current fields and adds an edit action:

- Login (read-only identifier in list and form).
- Display name.
- Roles chips/labels.
- Edit button/action visible only to admins.

## Edit User Modal/Form

Opening behavior:

- Triggered from selected row action.
- Prefills current user values.
- Fetches/uses role catalog from existing API composable.

Fields:

| Field | Editable | Required | Notes |
|-------|----------|----------|-------|
| Login | no | yes | Display-only for context |
| Display name | yes | yes | Trim before submit |
| Roles | yes | yes | Multi-select from backend role catalog |

Submit behavior:

- Disable submit while request is in flight.
- Call update API with `userId`, `displayName`, `roleCodes`.
- On success, close/reset modal and refresh list/search results.
- Show success toast/banner.

Cancel behavior:

- Close form without changing list data.
- Clear local edit state.

## API Composable Contract

Extend existing users composable with:

- `updateUser(userId, payload)` -> returns updated user summary.
- Shared request error mapping into typed UI-facing cases.

Payload shape:

```ts
type UpdateUserRequest = {
  displayName: string
  roleCodes: string[]
}
```

## Error Mapping

| Backend outcome | UI expectation |
|-----------------|----------------|
| `validation_error` | Field/form validation message |
| `invalid_roles` | Role-selection correction message |
| `user_not_found` | Inform user target disappeared and refresh list |
| `last_admin_role_removal_forbidden` | Blocking message explaining admin-floor guard |
| `401` | Existing session-expired auth handling |
| `403` | Forbidden state/message consistent with cabinet patterns |
| network/unknown | Generic recoverable error banner |

## Localization Additions

Add keys/messages for edit flow in RU/EN locale files:

- `users.actions.edit`
- `users.edit.title`
- `users.edit.subtitle`
- `users.edit.success`
- `users.edit.errors.userNotFound`
- `users.edit.errors.lastAdminGuard`

Reuse existing field labels where possible.

## Manual Verification Flow

1. Login as `ADMIN` and open `/cabinet/users`.
2. Edit a non-admin user: change display name and add/remove role.
3. Confirm success notification and refreshed list data.
4. Attempt submit with empty display name -> validation error shown.
5. Attempt to remove `ADMIN` from last active admin -> blocking message.
6. Login as non-admin and verify edit controls are absent and direct API call returns `403`.
