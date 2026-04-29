# Data Model: Фронтенд уведомлений

## 1. Backend changes (minimal)

### 1.1 Domain entity extension

Add `targetEntityId: UUID?` to `Notification` domain entity. Paired with `targetType` and `targetId` — stores the UUID of the target entity for frontend routing.

### 1.2 Database migration

New Flyway migration: add `target_entity_id UUID` column to `notification` table (nullable, no FK constraint — entity may be in different module's table).

### 1.3 API response extension

Add `targetEntityId: String?` (UUID as string) to `NotificationResponse` DTO.

### 1.4 Trigger updates

All three trigger call sites must pass entity UUID in `CreateNotificationCommand.targetEntityId`:
- `AssignProductionTaskUseCase` → `saved.id` (task UUID)
- `ChangeProductionTaskStatusUseCase` → `saved.id` (task UUID)
- `OverdueTaskNotificationJob` → `task.id` (task UUID)

## 2. Frontend types

### 2.1 NotificationResponse (from backend API)

```typescript
interface NotificationResponse {
  id: string                          // UUID
  type: NotificationType              // enum string
  title: string                       // max 200 chars
  body: string | null                 // max 1000 chars
  targetType: NotificationTargetType | null  // enum string
  targetId: string | null             // human-readable number (PT-000001)
  targetEntityId: string | null       // UUID for routing
  read: boolean
  readAt: string | null               // ISO instant
  createdAt: string                   // ISO instant
}

type NotificationType = 'TASK_ASSIGNED' | 'STATUS_CHANGED' | 'TASK_OVERDUE'
type NotificationTargetType = 'ORDER' | 'PRODUCTION_TASK'
```

### 2.2 NotificationsPageResponse (paginated list)

```typescript
interface NotificationsPageResponse {
  items: NotificationResponse[]
  page: number          // 0-indexed
  size: number
  totalItems: number
  totalPages: number
}
```

### 2.3 UnreadCountResponse

```typescript
interface UnreadCountResponse {
  count: number
}
```

### 2.4 MarkReadResponse

```typescript
interface MarkReadResponse {
  id: string
  read: boolean
  readAt: string | null
}
```

### 2.5 MarkAllReadResponse

```typescript
interface MarkAllReadResponse {
  updated: number
}
```

## 3. Frontend state

### 3.1 Notification store (Pinia)

```typescript
// Global state (survives page navigation)
{
  unreadCount: number           // from polling GET /api/notifications/unread-count
  dropdownItems: NotificationResponse[] | null  // last 10, loaded on dropdown open
  dropdownLoading: boolean
}
```

### 3.2 Notifications page (composable-local state)

```typescript
// Local to NotificationsPage component
{
  data: NotificationsPageResponse | null
  loading: boolean
  error: ApiError | null
  // filters
  page: number                  // 0-indexed
  unreadOnly: boolean
}
```

## 4. Navigation mapping

| targetType | Route | Param |
|---|---|---|
| `PRODUCTION_TASK` | `/cabinet/production-tasks/:id` | `targetEntityId` |
| `ORDER` | `/cabinet/orders/:id` | `targetEntityId` |
| `null` | no navigation | mark read only |
