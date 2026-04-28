# Data Model: 008-notifications-infrastructure

## Entity: Notification

| Field             | Type                      | Nullable | Default     | Constraints                                |
|-------------------|---------------------------|----------|-------------|--------------------------------------------|
| id                | UUID                      | NO       | generated   | PK                                         |
| recipientUserId   | UUID                      | NO       |             | FK → app_user(id)                          |
| type              | String (NotificationType) | NO       |             | Stored as VARCHAR; Kotlin enum in domain   |
| title             | String                    | NO       |             | 1–200 characters                           |
| body              | String                    | YES      | null        | max 1000 characters                        |
| targetType        | String (NotificationTargetType) | YES | null       | Stored as VARCHAR; enum: ORDER, PRODUCTION_TASK |
| targetId          | String                    | YES      | null        | Human-readable: order name or task number  |
| read              | Boolean                   | NO       | false       |                                            |
| readAt            | Instant                   | YES      | null        | Set on first mark-as-read; never overwritten |
| createdAt         | Instant                   | NO       | now()       |                                            |

### Validation Rules (Domain Layer)

- `title` must not be blank
- `title.length` must be in 1..200
- `body?.length` must be <= 1000 (if provided)
- `targetType` and `targetId` must be either both null or both non-null (consistency constraint)

### Indexes

| Index Name                              | Columns                                          | Purpose                                       |
|-----------------------------------------|--------------------------------------------------|-----------------------------------------------|
| idx_notification_recipient_read_created | (recipient_user_id, read, created_at DESC)       | Primary query: "my notifications, newest first" + "my unread" |
| idx_notification_recipient_unread_count | (recipient_user_id) WHERE read = false           | Partial index for fast unread count polling    |

### Relationships

- `recipientUserId` → `app_user(id)`: FK constraint, ON DELETE CASCADE (if user is deleted, their notifications are irrelevant)
- No relationship to `customer_order` or `production_task` tables — `targetId` is a denormalized human-readable reference, not a FK

### State Transitions

Notification has a minimal lifecycle:

```
UNREAD (read=false, readAt=null)
  → READ (read=true, readAt=timestamp)
```

One-way transition only. No "mark as unread" in Phase 1.

## Enum: NotificationType

```
TASK_ASSIGNED        # Executor received a new task assignment
STATUS_CHANGED       # Task status was changed
TASK_OVERDUE         # Task past its planned finish date
```

Stored as VARCHAR in DB. New values added via Kotlin enum extension in spec 2 — no migration required.

## Enum: NotificationTargetType

```
ORDER                # Links to orders.detail route
PRODUCTION_TASK      # Links to production-tasks.detail route
```

Stored as VARCHAR in DB.

## Flyway Migration: V6__create_notification_table.sql

```sql
CREATE TABLE notification (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    recipient_user_id   UUID         NOT NULL,
    type                VARCHAR(50)  NOT NULL,
    title               VARCHAR(200) NOT NULL,
    body                VARCHAR(1000),
    target_type         VARCHAR(30),
    target_id           VARCHAR(100),
    read                BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_notification PRIMARY KEY (id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_user_id)
        REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_notification_recipient_read_created
    ON notification (recipient_user_id, read, created_at DESC);

CREATE INDEX idx_notification_recipient_unread_count
    ON notification (recipient_user_id) WHERE read = FALSE;
```
