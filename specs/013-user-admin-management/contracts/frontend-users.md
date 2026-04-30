# Frontend Contract: Cabinet Users Section

## Route

| Route | Name | Access |
|-------|------|--------|
| `/cabinet/users` | `users.list` | Visible and accessible only when the current session is admin-capable |

## Sidebar

- Add "Users" / localized "Пользователи" item to the main cabinet sidebar.
- Item is visible only when current permissions indicate administrator access.
- Non-administrators must not see the item.

## Page Layout

The Users page contains:

- Header with page title and short description.
- Search input for login/display-name search.
- User list/table.
- "Create user" action.
- Create-user form/dialog.

## User List

Each returned row must display:

- Login.
- Display name.
- Assigned roles, shown as role labels/codes.

States:

- Loading while user search is in flight.
- Empty state when search returns no users.
- Error state when list/search fails.

## Create User Form

Fields:

| Field | Required | Notes |
|-------|----------|-------|
| Login | yes | Trimmed before submission |
| Display name | yes | Trimmed before submission |
| Initial password | yes | Non-empty only for this increment |
| Roles | yes | Multi-select from supported role catalog; may include Administrator |

Submit behavior:

- Disable submit while request is in flight.
- On success, close or reset the form and refresh list/search so the created user is visible.
- Never display the initial password after successful creation.

Error behavior:

| Error | UI expectation |
|-------|----------------|
| Duplicate login | Inline or form-level message that login already exists |
| Missing field | Field-specific or form-level correction message |
| Empty initial password | Correction message for password field |
| Invalid role | Refresh/verify role selection and show correction message |
| 401 | Existing auth/session handling |
| 403 | Forbidden state or message consistent with cabinet patterns |

## Role Catalog

- Fetch role catalog for the form from backend.
- Display role labels but submit canonical role codes.
- The catalog must contain Administrator, Order Manager, Warehouse, Production Supervisor, and Production Executor.

## Localization

Add Russian labels/messages consistent with the cabinet:

- `Пользователи`
- `Создать пользователя`
- `Логин`
- `Отображаемое имя`
- `Начальный пароль`
- `Роли`
- `Пользователь создан`
- Duplicate/validation/forbidden messages.

## Manual Verification Flow

1. Sign in as administrator.
2. Open `/cabinet/users` from sidebar.
3. Search existing users and verify role chips/labels are visible.
4. Create `warehouse.demo` with `WAREHOUSE`.
5. Sign out and sign in as `warehouse.demo`.
6. Verify warehouse access is present and audit/users are not visible.
