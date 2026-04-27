# Quickstart: Orders API + Frontend Wiring

This quickstart verifies Feature 004 after implementation.

## 1. Start Local Runtime

From the repository root:

```bash
docker compose up --build --wait
```

Expected services:

- Backend API on `http://localhost:8080`
- Frontend cabinet on `http://localhost:5173/cabinet/`
- PostgreSQL exposed on host port `15432`

Check backend health:

```bash
curl http://localhost:8080/actuator/health
```

Expected response includes `"status":"UP"`.

## 2. Log In

Open:

```text
http://localhost:5173/cabinet/
```

Use local credentials:

```text
login: admin
password: admin
```

Expected:

- Login succeeds.
- The cabinet opens without session-expired overlay.
- Order pages are reachable.

## 3. Smoke Test Customers API

```bash
TOKEN="$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"admin","password":"admin"}' | jq -r '.accessToken')"

curl -s "http://localhost:8080/api/customers?activeOnly=true&limit=5" \
  -H "Authorization: Bearer ${TOKEN}" | jq
```

Expected:

- Response includes seeded active customers.
- No customer creation endpoint is required for this feature.

## 4. Smoke Test Orders API

List seeded orders:

```bash
curl -s "http://localhost:8080/api/orders?size=10" \
  -H "Authorization: Bearer ${TOKEN}" | jq
```

Create a new order using an active `customerId` from the previous response:

```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId": "REPLACE_WITH_ACTIVE_CUSTOMER_ID",
    "deliveryDate": "2026-05-15",
    "notes": "Manual smoke test",
    "items": [
      { "itemName": "Smoke item", "quantity": 1, "uom": "шт" }
    ]
  }' | jq
```

Expected:

- Response status is `201`.
- Returned order has status `NEW`.
- Returned order has a unique `orderNumber`.
- Returned order has `version`.

## 5. Smoke Test Status Transition

Move the created order from `NEW` to `IN_WORK`:

```bash
curl -s -X POST "http://localhost:8080/api/orders/REPLACE_WITH_ORDER_ID/status" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{
    "expectedVersion": 0,
    "toStatus": "IN_WORK",
    "note": "Started from quickstart"
  }' | jq
```

Expected:

- Response status is `200`.
- Order status becomes `IN_WORK`.
- Status history includes `NEW -> IN_WORK`.

Attempt an invalid reverse transition:

```bash
curl -i -X POST "http://localhost:8080/api/orders/REPLACE_WITH_ORDER_ID/status" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{
    "expectedVersion": 1,
    "toStatus": "NEW"
  }'
```

Expected:

- Response is `422`.
- Existing order status remains unchanged.

## 6. Smoke Test Frontend

In the cabinet UI:

1. Open the orders list.
2. Confirm seeded orders appear with order number, customer, delivery date, status, and update time.
3. Search by order number and by customer.
4. Filter by status and active/non-shipped orders.
5. Create a new order using an existing active customer.
6. Open the created order and confirm detail, items, history, and versioned update behavior.
7. Move the order through `новый -> в работе -> готов -> отгружен`.
8. Confirm shipped order regular fields are read-only.
9. Open dashboard and confirm counts/distribution/recent activity reflect saved orders.

## 7. Verification Commands

Backend:

```bash
docker run --rm \
  -v "$PWD":/workspace \
  -w /workspace \
  eclipse-temurin:21-jdk \
  ./gradlew test
```

Frontend:

```bash
pnpm --dir frontend/cabinet test
pnpm --dir frontend/cabinet build
```

Legacy runtime guard:

```bash
rg "/api/method|frappeCall|frappe.client|frappe.auth|frappe-client|X-Frappe|socket.io" frontend/cabinet/src frontend/cabinet/tests
```

Expected:

- Backend tests pass.
- Frontend tests pass.
- Frontend build passes.
- Legacy runtime search returns no runtime offenders except intentional test guard definitions if present.

## Verification Record

- Backend tests: PASS on 2026-04-27. Command: `docker run --rm -e GRADLE_USER_HOME=/tmp/gradle-home -v "$PWD":/workspace -w /workspace eclipse-temurin:21-jdk ./gradlew --project-cache-dir /tmp/gradle-project-cache test`. Result: `BUILD SUCCESSFUL`, 5 tasks executed.
- Frontend tests: PASS on 2026-04-27. Command: `pnpm test` from `frontend/cabinet`. Result: 29 test files passed, 207 tests passed.
- Frontend build: PASS on 2026-04-27. Command: `pnpm build` from `frontend/cabinet`. Result: `vue-tsc --noEmit && vite build`, Vite build completed successfully.
- Docker startup: PASS on 2026-04-27. Command: `docker compose up --build --wait`. Result: backend, frontend, and PostgreSQL containers reported healthy.
- API smoke checks: PASS on 2026-04-27. Verified login, health `UP`, active customer lookup, order list, order creation (`NEW`), status transition to `IN_WORK`, invalid reverse transition `422`, dashboard totals, and frontend HTTP `200`.
- Manual frontend smoke: PASS on 2026-04-27. Verified login with `admin/admin`, dashboard KPI/recent widgets, orders list, order detail/history/actions, and absence of the session-expired overlay or legacy Frappe popup. During this smoke, an order-list cancellation regression was found and fixed; final frontend tests/build passed after the fix.
- Legacy runtime search: PASS on 2026-04-27. Search for `/api/method|frappeCall|frappe.client|frappe.auth|frappe-client|X-Frappe|socket.io` returned only intentional guard definitions in `frontend/cabinet/tests/unit/no-frappe-runtime.test.ts`; no runtime offenders found.
- Controller/persistence boundary review: PASS on 2026-04-27. `OrderController` and `OrderDashboardController` remain HTTP/DTO/result mappers; order write/status/dashboard rules live in application/domain services. Persistence adapters remain JPA mapping/query adapters without business-rule enforcement beyond persistence concerns.
