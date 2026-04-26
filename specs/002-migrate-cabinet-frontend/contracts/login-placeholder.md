# Contract: Login Placeholder

This contract defines how the migrated login screen behaves before the new platform has real
authentication.

## Scope

The login form is interactive but cannot authenticate a user in this feature.

Out of scope:

- token issuance;
- session creation;
- backend user lookup;
- role loading;
- two-factor authentication;
- password recovery;
- logout semantics.

## Initial State

When the user opens:

```text
http://localhost:5173/cabinet/login
```

Expected state:

- username field is visible and enabled;
- password field is visible and enabled;
- submit button is visible;
- no authenticated workspace is shown;
- no call to old Frappe login endpoint is required for initial render.

## Submit Behavior

Given any non-empty username and password, when the user submits the form:

Expected result:

- no successful authentication is recorded;
- user remains on the login screen;
- protected cabinet routes remain unavailable;
- UI shows a clear message equivalent to: "Authorization is not connected yet";
- no old Frappe `/api/method/login` request is made.

## Empty Form Behavior

Given empty username or password, when the user submits the form:

Expected result:

- user remains on the login screen;
- UI shows the existing validation/error state for empty credentials or an equivalent message;
- no backend auth call is made.

## Protected Route Redirect Behavior

Given unauthenticated state, when the user opens:

```text
http://localhost:5173/cabinet/orders
```

Expected result:

- app routes the user to login screen;
- route intent is preserved only as safe client-side state/query for future use;
- no successful access to protected pages occurs.

## Browser Auth Restrictions

The login placeholder must not trigger:

- browser Basic Auth popup;
- backend HTML login page;
- redirect to Spring Security login;
- redirect to old Frappe Desk/login.
