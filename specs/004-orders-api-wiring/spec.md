# Feature Specification: Orders API + Frontend Wiring

**Feature Branch**: `004-orders-api-wiring`  
**Created**: 2026-04-26  
**Status**: Accepted  
**Input**: User description: "Feature 004: Orders API + Frontend Wiring. Минимальный состав: Backend domain: Customer, CustomerOrder, CustomerOrderItem, OrderStatusChange. Flyway migration для таблиц клиентов, заказов, позиций, истории статусов. REST API: GET /api/orders POST /api/orders GET /api/orders/{id} PUT /api/orders/{id} POST /api/orders/{id}/status GET /api/customers. RBAC: ADMIN и менеджер заказов могут создавать/редактировать остальные пока read-only или forbidden. Audit: создание заказа, изменение заказа, изменение статуса. Frontend: заменить текущие пустые placeholders в use-orders, use-customers, dashboard widgets на Spring endpoints."

## Clarifications

### Session 2026-04-26

- Q: Which Phase 1 roles can view orders, and which roles can write? → A: All authenticated Phase 1 roles can view orders read-only; only ADMIN and order managers can create, edit, and change status.
- Q: How are customers managed in this feature? → A: Customers are prefilled or existing; the UI can search and select active customers but cannot create new customers in this feature.
- Q: Which order status transitions are allowed? → A: Only direct forward transitions are allowed: "новый" → "в работе" → "готов" → "отгружен"; reverse transitions are forbidden.
- Q: How is the order number assigned? → A: The system automatically assigns a unique human-readable order number, and users can search by it.
- Q: What order history detail is required now? → A: History stores key business-field diffs now and must remain extensible to full before/after snapshots later.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View And Search Orders (Priority: P1)

As an order manager or administrator, I need to see real customer orders after signing in so that I can monitor current work instead of an empty placeholder screen.

**Why this priority**: This restores the first useful Phase 1 business workflow after removing legacy runtime dependencies. Without a real order list, create/edit/status flows cannot be validated by users.

**Independent Test**: Can be tested by signing in as a permitted user, opening the orders area, and confirming that seeded and newly created orders appear with customer, delivery date, status, and search/filter behavior.

**Acceptance Scenarios**:

1. **Given** a signed-in order manager and existing orders, **When** the user opens the orders list, **Then** the system shows orders with customer name, delivery date, current status, and last update information.
2. **Given** a signed-in order manager and multiple orders, **When** the user searches by order number or customer and applies status, customer, active, overdue, or date filters, **Then** the list updates to show only matching orders.
3. **Given** any authenticated Phase 1 role, **When** the user opens the orders area, **Then** the system allows read-only order visibility appropriate for that role.

---

### User Story 2 - Create Customer Order (Priority: P1)

As an order manager, I need to create a customer order with delivery date, notes, and order items so that new sales work can enter production control.

**Why this priority**: Order creation is a core MVP requirement and the first criterion for Phase 1 completion.

**Independent Test**: Can be tested by creating a new order from the UI using an existing customer, adding at least one item, saving it, and verifying that the order is visible in the list and detail view.

**Acceptance Scenarios**:

1. **Given** a signed-in order manager and an active customer, **When** the user creates an order with required fields and one or more items, **Then** the system saves the order with initial status "новый", automatically assigns a unique human-readable order number, and shows it in the orders list.
2. **Given** missing required customer, delivery date, or order item information, **When** the user attempts to save, **Then** the system prevents creation and identifies the fields that need correction.
3. **Given** a signed-in user other than an administrator or order manager, **When** the user attempts to create an order, **Then** the system denies the action and does not create any order.

---

### User Story 3 - Edit Order Details (Priority: P2)

As an order manager, I need to edit an order before it is shipped so that customer, delivery date, notes, and item composition stay accurate as work changes.

**Why this priority**: Order changes are common during sales and production preparation, and Phase 1 requires order editing and history.

**Independent Test**: Can be tested by opening an existing non-shipped order, changing editable fields, saving, and confirming that the updated values appear after reload and are recorded in history.

**Acceptance Scenarios**:

1. **Given** a non-shipped order, **When** an order manager edits customer, delivery date, notes, or items, **Then** the system saves the changes and shows the updated order.
2. **Given** an order that has changed since the user opened it, **When** the user attempts to save stale data, **Then** the system prevents overwriting newer changes and asks the user to reload or resolve the conflict.
3. **Given** a shipped order, **When** a non-admin user attempts to edit regular order fields, **Then** the system keeps the order read-only.
4. **Given** an order manager changes key business fields, **When** the change is saved, **Then** the order history records which key fields changed and remains suitable for future full before/after snapshot expansion.

---

### User Story 4 - Change Order Status (Priority: P2)

As an order manager or administrator, I need to move an order through its basic lifecycle so that everyone can see whether the order is new, in work, ready, or shipped.

**Why this priority**: Status visibility is essential for production control, dashboard metrics, and later production task workflows.

**Independent Test**: Can be tested by opening an order and moving it through the allowed lifecycle while confirming that each status change appears in the order and status history.

**Acceptance Scenarios**:

1. **Given** an order in status "новый", **When** a permitted user starts the order, **Then** the status changes to "в работе" and the change is recorded.
2. **Given** an order in status "в работе", **When** a permitted user marks it ready, **Then** the status changes to "готов" and the change is recorded.
3. **Given** an order in status "готов", **When** a permitted user ships it, **Then** the status changes to "отгружен" and regular editing becomes unavailable.
4. **Given** an invalid lifecycle jump, **When** a user attempts the change, **Then** the system rejects it and leaves the previous status unchanged.

---

### User Story 5 - Dashboard Reflects Orders (Priority: P3)

As an administrator or order manager, I need the dashboard widgets to use real order data so that the home screen shows useful operational signals without legacy errors.

**Why this priority**: Dashboard value depends on the order data foundation, but it can be delivered after list/create/edit/status flows.

**Independent Test**: Can be tested by opening the dashboard with existing orders and verifying that counts, distribution, recent orders, and trend widgets reflect saved orders and no legacy placeholder behavior remains.

**Acceptance Scenarios**:

1. **Given** existing orders in multiple statuses, **When** the user opens the dashboard, **Then** the KPI and status distribution widgets reflect the saved orders.
2. **Given** overdue non-shipped orders, **When** the dashboard loads, **Then** overdue count includes those orders and excludes shipped orders.
3. **Given** no orders, **When** the dashboard loads, **Then** widgets show an empty but valid state without errors.

---

### Edge Cases

- A customer is inactive after an order was created: existing orders still display the customer, but new order creation can only choose active customers.
- A user attempts to edit or change status after another user has already changed the same order: the system prevents silent overwrites.
- A user attempts to ship an order that is not ready: the system rejects the invalid transition.
- A user attempts to move an order backward to a previous status: the system rejects the transition and leaves the current status unchanged.
- An order has no optional notes or contact details: list and detail screens remain readable.
- A filtered order list has no matches: the system shows an empty result state without treating it as an error.
- A user loses permission between loading a screen and submitting an action: the system denies the action and preserves existing data.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST maintain customers with display name, active/inactive state, and optional contact information.
- **FR-002**: Users with order access MUST be able to find and select active existing customers while creating or editing orders.
- **FR-003**: System MUST maintain customer orders with order number, customer, delivery date, status, notes, item composition, creator, creation time, and last update information.
- **FR-004**: System MUST maintain order items with item name, quantity, unit of measure, and display order within the order.
- **FR-005**: System MUST create new orders in the initial status "новый".
- **FR-005a**: System MUST automatically assign each new order a unique human-readable order number.
- **FR-006**: System MUST support the order statuses "новый", "в работе", "готов", and "отгружен".
- **FR-007**: System MUST allow permitted users to list, search, and filter orders by search text, status, customer, active/non-shipped state, overdue state, and delivery date range.
- **FR-008**: System MUST allow permitted users to create an order with a customer, delivery date, optional notes, and at least one order item.
- **FR-009**: System MUST validate required order fields and item values before saving.
- **FR-010**: System MUST allow permitted users to view full order details including customer information, items, status, and relevant history.
- **FR-011**: System MUST allow permitted users to edit non-shipped orders.
- **FR-012**: System MUST prevent regular editing of shipped orders.
- **FR-013**: System MUST prevent stale updates from silently overwriting newer order changes.
- **FR-014**: System MUST allow permitted users to move orders only through direct forward lifecycle transitions: "новый" → "в работе" → "готов" → "отгружен".
- **FR-015**: System MUST reject invalid order status transitions, including skipped stages and reverse transitions, and preserve the current status.
- **FR-016**: System MUST record every order status change with previous status, new status, actor, timestamp, and optional note.
- **FR-017**: System MUST record audit events for order creation, order updates, and order status changes.
- **FR-017a**: System MUST record key business-field diffs for order updates, including customer, delivery date, notes, item summary, and status when applicable.
- **FR-017b**: Order history records MUST remain extensible to store full before/after snapshots in a later feature without replacing the user-visible history model.
- **FR-018**: System MUST allow all authenticated Phase 1 roles to view orders read-only.
- **FR-019**: System MUST allow only administrators and order managers to create, edit, and change order status.
- **FR-020**: System MUST deny order write actions for authenticated users without order write permission.
- **FR-021**: System MUST keep protected order and customer operations available only to authenticated users.
- **FR-022**: System MUST seed local development data with several customers and orders so the list and dashboard are meaningful immediately after signing in.
- **FR-023**: System MUST connect the existing order list, order detail, order creation, customer picker, and dashboard views to the new order/customer data source.
- **FR-024**: System MUST not use legacy Frappe runtime endpoints, Frappe realtime, or placeholder order/customer data for this feature.
- **FR-025**: System MUST NOT include customer creation or customer editing in this feature.

### Key Entities *(include if feature involves data)*

- **Customer**: A company or person placing orders. Key attributes include name, display name, active state, and optional contact information.
- **Customer Order**: A sales/production order tied to one customer. It has an order number, delivery date, status, notes, creator, timestamps, and order items.
- **Customer Order Item**: A line in a customer order describing what must be produced or delivered, with item name, quantity, unit of measure, and ordering.
- **Order Status Change**: A trace record of an order lifecycle change, including previous status, new status, actor, timestamp, and optional note.
- **Order Change Diff**: A trace record of changed key business fields for an order update. It captures field names and before/after values for the fields visible in Phase 1 history and remains compatible with future full snapshot storage.
- **Audit Event**: A trace record of significant business actions such as creating an order, editing an order, or changing an order status.

### Constitution Alignment *(mandatory)*

- **ERP/domain fit**: Strengthens the core ERP order workflow and customer/order data foundation required before production tasks, inventory allocation, and operational reporting.
- **TOC readiness**: Preserves order lifecycle status history, delivery dates, overdue state, and change timestamps that can later support due-date pressure, throughput, lead time, and buffer analysis.
- **Traceability/audit**: Order creation, field changes, and status transitions must record who changed what, when, and from which state to which state.
- **Security/API boundary**: Uses explicit authenticated access and role-based permission outcomes for order visibility and write actions. Protected access must return clear unauthorized or forbidden outcomes rather than browser login prompts.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A permitted user can create a valid customer order with at least one item in under 1 minute.
- **SC-002**: A permitted user can find an existing order by order number or customer in under 15 seconds when up to 500 orders exist.
- **SC-003**: 100% of order creations, order edits, and order status changes leave a traceable business event with actor and timestamp.
- **SC-003a**: 100% of successful order edits record key business-field diffs that can be reviewed in order history.
- **SC-004**: 100% of unauthenticated order/customer access attempts receive a clear no-access outcome without exposing protected order data.
- **SC-005**: 100% of forbidden write attempts by read-only users leave existing order data unchanged.
- **SC-006**: Dashboard order widgets load without legacy placeholder errors and show valid empty states when there are no orders.
- **SC-007**: Seeded local data allows a reviewer to verify order list, order detail, order creation, status changes, and dashboard counts immediately after signing in.
- **SC-008**: Shipped orders are read-only for regular editing in all tested user flows.

## Assumptions

- Existing authentication and session restoration are reused.
- The local administrator account remains available for verification.
- The canonical order write role is treated as "order manager"; if role labels differ between UI and backend, they are mapped to the same business permission.
- All authenticated Phase 1 roles can view orders read-only; write and status-change permission is limited to administrators and order managers.
- Customer creation and customer editing are outside this feature; customers are provided by seed data or existing records.
- Full before/after order snapshots are outside this feature, but the history model must not block adding them later.
- Production tasks, task board, warehouse, notifications, realtime updates, external integrations, and advanced workflow rules are outside this feature.
- The feature prioritizes a reliable Phase 1 order foundation over comprehensive reporting.
