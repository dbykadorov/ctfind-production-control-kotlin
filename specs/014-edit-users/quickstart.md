# Quickstart: Edit Existing Users in Cabinet

Manual verification checklist for admin user editing in Phase 01.

## Preconditions

Start from repository root:

```sh
make docker-up-detached && make health
# -> {"status":"UP"}
```

Sign in to cabinet at `http://localhost:5173/cabinet/login` with an administrator account.

## Scenario 1 - Edit Display Name and Roles

1. Open `/cabinet/users`.
2. Find existing user `warehouse.demo` (or another non-admin account).
3. Click edit action in the user row.
4. Change display name to `Warehouse Demo Updated`.
5. Update roles, for example add `PRODUCTION_EXECUTOR` and keep `WAREHOUSE`.
6. Submit.
7. **Check**: success message is shown.
8. **Check**: list refresh shows updated display name and role set.

## Scenario 2 - Login Reflection

1. Sign out from admin session.
2. Sign in as edited user.
3. **Check**: cabinet sections correspond to the updated role set.
4. Sign out and sign back in as admin for next checks.

## Scenario 3 - Validation Errors

As admin in edit form, verify:

1. Empty display name -> validation error.
2. No selected roles -> validation error.
3. Invalid role code through direct API request -> `invalid_roles` response.

## Scenario 4 - Last Admin Guard

1. Ensure only one active admin remains in database.
2. Open edit form for that admin user.
3. Remove `ADMIN` role and submit.
4. **Expected**: request rejected with `last_admin_role_removal_forbidden`; data unchanged.

## Scenario 5 - Authorization Boundaries

1. Sign in as non-admin user.
2. Open `/cabinet/users` directly.
3. **Expected**: forbidden state/redirect according to existing cabinet guard.
4. Attempt `PUT /api/users/{id}` with non-admin JWT.
5. **Expected**: `403`.

## Scenario 6 - Missing Target User

1. Open edit form for a user in admin session.
2. Delete or invalidate target user out-of-band (test fixture or DB setup).
3. Submit edit request.
4. **Expected**: `user_not_found` outcome and UI refresh prompt/message.

## Verification Commands

Before closing implementation work, run:

```sh
make backend-test
make frontend-test
make frontend-build
make test
make build
```
