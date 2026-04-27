# Contract: Production Tasks API

All endpoints are JSON-only and protected by existing Bearer JWT authentication. Protected unauthenticated requests return `401 Unauthorized`; authenticated users without permission return `403 Forbidden`.

## Common Types

### ProductionTaskStatus

```json
"NOT_STARTED" | "IN_PROGRESS" | "BLOCKED" | "COMPLETED"
```

Display labels:

- `NOT_STARTED`: `не начато`
- `IN_PROGRESS`: `в работе`
- `BLOCKED`: `заблокировано`
- `COMPLETED`: `выполнено`

### ProductionTaskAction

```json
"ASSIGN" | "PLAN" | "START" | "BLOCK" | "UNBLOCK" | "COMPLETE"
```

### ApiError

```json
{
  "code": "validation_failed",
  "message": "Human readable error",
  "details": {
    "field": "reason"
  }
}
```

Common codes: `unauthorized`, `forbidden`, `not_found`, `validation_failed`, `invalid_task_status_transition`, `stale_production_task_version`, `order_item_not_found`.

## GET /api/production-tasks

List, search, and filter production tasks visible to the current user.

### Query Parameters

- `search?: string` - task number, order number, customer, item, or purpose text
- `status?: ProductionTaskStatus`
- `orderId?: UUID`
- `orderItemId?: UUID`
- `executorUserId?: UUID`
- `assignedToMe?: boolean`
- `blockedOnly?: boolean`
- `activeOnly?: boolean`
- `dueDateFrom?: yyyy-mm-dd`
- `dueDateTo?: yyyy-mm-dd`
- `page?: number` - zero-based, defaults to `0`
- `size?: number` - defaults to `20`, max `100`
- `sort?: string` - defaults to a stable business order such as `updatedAt,desc`

Executors receive only tasks assigned to them even if broader filters are supplied.

### Response 200

```json
{
  "items": [
    {
      "id": "2f7e1d0f-4b1f-4276-8e99-0e9d3f07e917",
      "taskNumber": "PT-000001",
      "purpose": "Раскрой",
      "order": {
        "id": "95849543-a34d-4d8b-828a-981f46bfb63f",
        "orderNumber": "ORD-000001",
        "customerDisplayName": "ООО Ромашка",
        "deliveryDate": "2026-05-15"
      },
      "orderItem": {
        "id": "4d8f5cb9-0c97-4129-b984-4d8c6002ca3c",
        "lineNo": 1,
        "itemName": "Столешница"
      },
      "quantity": 2,
      "uom": "шт",
      "status": "NOT_STARTED",
      "statusLabel": "не начато",
      "executor": {
        "id": "3d857609-f6e2-4cd4-88f9-dc7475024e2d",
        "displayName": "Иван Исполнитель"
      },
      "plannedStartDate": "2026-05-01",
      "plannedFinishDate": "2026-05-03",
      "blockedReason": null,
      "updatedAt": "2026-04-27T11:00:00Z",
      "version": 0
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

## GET /api/production-tasks/{id}

Return full production task detail if visible to the current user.

### Response 200

```json
{
  "id": "2f7e1d0f-4b1f-4276-8e99-0e9d3f07e917",
  "taskNumber": "PT-000001",
  "purpose": "Раскрой",
  "order": {
    "id": "95849543-a34d-4d8b-828a-981f46bfb63f",
    "orderNumber": "ORD-000001",
    "customerDisplayName": "ООО Ромашка",
    "status": "IN_WORK",
    "deliveryDate": "2026-05-15"
  },
  "orderItem": {
    "id": "4d8f5cb9-0c97-4129-b984-4d8c6002ca3c",
    "lineNo": 1,
    "itemName": "Столешница",
    "quantity": 2,
    "uom": "шт"
  },
  "quantity": 2,
  "uom": "шт",
  "status": "NOT_STARTED",
  "statusLabel": "не начато",
  "executor": {
    "id": "3d857609-f6e2-4cd4-88f9-dc7475024e2d",
    "displayName": "Иван Исполнитель"
  },
  "plannedStartDate": "2026-05-01",
  "plannedFinishDate": "2026-05-03",
  "blockedReason": null,
  "allowedActions": ["ASSIGN", "PLAN", "START", "BLOCK"],
  "history": [
    {
      "type": "CREATED",
      "actorDisplayName": "Administrator",
      "eventAt": "2026-04-27T11:00:00Z",
      "fromStatus": null,
      "toStatus": "NOT_STARTED",
      "note": null,
      "reason": null
    }
  ],
  "createdAt": "2026-04-27T11:00:00Z",
  "updatedAt": "2026-04-27T11:00:00Z",
  "version": 0
}
```

### Error 403

Executors receive `403` for tasks not assigned to them.

## POST /api/production-tasks/from-order

Create one or more production tasks from an existing order and selected order items. Requires `ADMIN` or `ORDER_MANAGER`.

### Request

```json
{
  "orderId": "95849543-a34d-4d8b-828a-981f46bfb63f",
  "tasks": [
    {
      "orderItemId": "4d8f5cb9-0c97-4129-b984-4d8c6002ca3c",
      "purpose": "Раскрой",
      "quantity": 2,
      "uom": "шт",
      "executorUserId": "3d857609-f6e2-4cd4-88f9-dc7475024e2d",
      "plannedStartDate": "2026-05-01",
      "plannedFinishDate": "2026-05-03"
    }
  ]
}
```

### Response 201

```json
{
  "items": [
    {
      "id": "2f7e1d0f-4b1f-4276-8e99-0e9d3f07e917",
      "taskNumber": "PT-000001",
      "status": "NOT_STARTED",
      "version": 0
    }
  ]
}
```

## PUT /api/production-tasks/{id}/assignment

Assign or reassign one executor and planned dates. Requires `ADMIN`, `ORDER_MANAGER`, or `PRODUCTION_SUPERVISOR`.

### Request

```json
{
  "expectedVersion": 0,
  "executorUserId": "3d857609-f6e2-4cd4-88f9-dc7475024e2d",
  "plannedStartDate": "2026-05-01",
  "plannedFinishDate": "2026-05-03",
  "note": "Планирование смены"
}
```

### Response 200

Returns full updated task detail with incremented `version` and assignment/planning history.

### Error 409

```json
{
  "code": "stale_production_task_version",
  "message": "Production task has changed. Reload before saving."
}
```

## POST /api/production-tasks/{id}/status

Move a task through the allowed lifecycle. Requires `ADMIN` or `PRODUCTION_SUPERVISOR` for any task; `PRODUCTION_EXECUTOR` may update only assigned tasks.

### Request: Start Or Complete

```json
{
  "expectedVersion": 1,
  "toStatus": "IN_PROGRESS",
  "note": "Начал работу"
}
```

### Request: Block

```json
{
  "expectedVersion": 2,
  "toStatus": "BLOCKED",
  "reason": "Нет материала",
  "note": "Ожидаем поставку"
}
```

### Request: Unblock

```json
{
  "expectedVersion": 3,
  "toStatus": "IN_PROGRESS",
  "note": "Материал получен"
}
```

### Response 200

Returns full updated task detail with status history.

### Error 422

```json
{
  "code": "invalid_task_status_transition",
  "message": "Task status transition is not allowed."
}
```

## GET /api/production-tasks/assignees

Search active executor users for assignment. Requires `ADMIN`, `ORDER_MANAGER`, or `PRODUCTION_SUPERVISOR`.

### Query Parameters

- `search?: string`
- `limit?: number` - defaults to `20`, max `50`

### Response 200

```json
{
  "items": [
    {
      "id": "3d857609-f6e2-4cd4-88f9-dc7475024e2d",
      "displayName": "Иван Исполнитель",
      "login": "worker1"
    }
  ]
}
```

## Audit Requirements

Successful create, assign, planning update, status change, block, unblock, and complete operations must persist:

- A business audit event with actor, target task, timestamp, and summary.
- A task history entry suitable for the task detail timeline.
- Structured status fields where status changes occur.

Failed validation, forbidden writes, invalid transitions, and stale writes must not mutate task data.
