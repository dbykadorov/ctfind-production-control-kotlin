# Contract: Frontend Orders Wiring

This contract describes how the existing cabinet SPA replaces placeholder order/customer/dashboard behavior with Spring API calls through `src/api/api-client.ts`.

## Shared Rules

- All requests use the existing `httpClient`, so the Bearer JWT is attached automatically.
- `401` responses use the existing session-expired redirect behavior.
- `403` write responses show a permission error and leave current UI state unchanged.
- No frontend code may call legacy Frappe endpoints, Frappe realtime, `frappeCall`, or `socket.io`.

## use-customers.ts

### searchCustomers

Input:

- `query: string`
- `activeOnly?: boolean`
- `limit?: number`

Behavior:

- Calls `GET /api/customers`.
- Defaults to active customers only.
- Returns customers suitable for `CustomerPicker`.
- Does not create or edit customers.

Result shape:

```ts
interface CustomerOption {
  id: string
  displayName: string
  status: 'ACTIVE' | 'INACTIVE'
  contactPerson?: string
  phone?: string
  email?: string
}
```

## use-orders.ts

### List Orders

Behavior:

- Calls `GET /api/orders` with search, status, customer, active, overdue, delivery-date range, page, and size filters.
- Maps API status codes to the existing UI labels and filter options.
- Empty result is a valid state, not an error.

Result shape:

```ts
interface OrderListRow {
  id: string
  orderNumber: string
  customerName: string
  deliveryDate: string
  status: 'NEW' | 'IN_WORK' | 'READY' | 'SHIPPED'
  statusLabel: string
  updatedAt: string
  version: number
  overdue: boolean
}
```

### Load Order Detail

Behavior:

- Calls `GET /api/orders/{id}`.
- Shows customer details, items, status history, and key business-field diffs.
- Keeps `version` for stale update protection.

### Create Order

Behavior:

- Calls `POST /api/orders`.
- Requires selected active existing customer, delivery date, and at least one item.
- On success, navigates to the created order detail or list according to existing page flow.
- On validation failure, maps backend field errors to form feedback.

### Update Order

Behavior:

- Calls `PUT /api/orders/{id}` with `expectedVersion`.
- Replaces editable fields and item composition.
- On `409 stale_order_version`, asks the user to reload before saving.
- Shipped orders are read-only in the UI for regular edit actions.

## use-workflow.ts

### Load Allowed Transitions

Behavior:

- Derives direct forward transition from current order status locally or from order detail metadata.
- Shows no transition action for `SHIPPED`.

### Apply Transition

Behavior:

- Calls `POST /api/orders/{id}/status` with `expectedVersion`, `toStatus`, and optional note.
- On success, refreshes order detail/list/dashboard caches.
- On invalid transition or stale version, shows a clear error and leaves local order data unchanged.

## Dashboard Composables

### use-dashboard-stats.ts

Behavior:

- Calls `GET /api/orders/dashboard`.
- Maps `totalOrders`, `activeOrders`, `overdueOrders`, and status counts to KPI cards.

### use-trend-data.ts

Behavior:

- Uses `trend` from `GET /api/orders/dashboard`.
- Shows valid empty series when there are no orders.

### use-recent-activity.ts

Behavior:

- Uses `recentChanges` from `GET /api/orders/dashboard`.
- Displays order number, customer, status transition, actor, and timestamp.

## Permission Behavior

- All authenticated roles may open read-only order list/detail.
- Only `ADMIN` and order-manager users see create/edit/status action affordances.
- If the backend returns `403`, the UI must show an access message even if the action was visible due to stale role state.

## Regression Expectations

- Unit tests cover API parameter mapping, response mapping, validation error mapping, stale update handling, and read-only permission behavior.
- Existing `no-frappe-runtime` regression must remain green.
- Frontend build must pass with no placeholder data used for order/customer/dashboard behavior.
