# Quickstart: Superadmin Seed and User Management

Manual verification for Phase 01 user administration.

## Preconditions

Build and start the local stack:

```sh
make docker-reset && make docker-up-detached && make health
# -> {"status":"UP"}
```

Expected local behavior:

- A local administrator account is available through the documented local seed.
- The administrator can open the cabinet at `http://localhost:5173/cabinet/login`.
- Production-like environments must not rely on hardcoded local credentials; they require secure operator-provided bootstrap credentials when no `ADMIN` exists.

## Scenario 1 - Local Superadmin Bootstrap

1. Start from a clean local database.
2. Sign in to the cabinet as the documented local administrator.
3. Open the sidebar.
4. **Check**: "Пользователи" is visible.
5. Open `/cabinet/users`.
6. **Check**: user-management page loads and shows existing users with assigned roles.

## Scenario 2 - Create Warehouse User

1. Stay signed in as administrator.
2. Open `/cabinet/users`.
3. Click "Создать пользователя".
4. Fill:
   - login: `warehouse.demo`
   - display name: `Warehouse Demo`
   - initial password: `demo`
   - roles: `WAREHOUSE`
5. Submit.
6. **Check**: user is created, appears in list/search, and the row shows role `WAREHOUSE`.
7. **Check**: the initial password is not displayed after creation.

## Scenario 3 - Login as Created User

1. Sign out.
2. Sign in as `warehouse.demo` with password `demo`.
3. **Check**: warehouse section is visible.
4. **Check**: audit and users sections are not visible.
5. Open `/cabinet/users` directly.
6. **Expected**: forbidden/denied state.

## Scenario 4 - Duplicate Login

1. Sign back in as administrator.
2. Open `/cabinet/users`.
3. Try creating `warehouse.demo` again.
4. **Expected**: duplicate-login error and no second account.

## Scenario 5 - Validation Errors

As administrator, verify each case:

1. Empty login -> correction message.
2. Empty display name -> correction message.
3. Empty initial password -> correction message.
4. No selected roles -> correction message.
5. Unsupported role code through direct API call -> validation error.

## Scenario 6 - ADMIN Can Create ADMIN

1. Sign in as administrator.
2. Create `admin.backup` with role `ADMIN`.
3. Sign out and sign in as `admin.backup`.
4. **Check**: users and audit sections are visible.

## Scenario 7 - Production-Like Bootstrap Guard

Use a production-like configuration in a clean database with no `ADMIN` user.

1. Start without secure superadmin credentials.
2. **Expected**: application startup fails with a clear operator-facing configuration error.
3. Provide secure superadmin credentials.
4. Start again.
5. **Expected**: exactly one administrator is created and can sign in.
6. Restart three times.
7. **Expected**: no duplicate administrators are created.

## Verification Commands

Run the relevant checks before marking implementation complete:

```sh
make backend-test
make frontend-test
make frontend-build
make test
make build
```

For runtime behavior:

```sh
make docker-up-detached && make health
```
