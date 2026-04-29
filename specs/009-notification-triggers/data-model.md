# Data Model: Триггеры генерации уведомлений

## Existing Entities (no modifications to schema)

### Notification (из спеки 008, таблица `notification`)

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | PK |
| recipient_user_id | UUID | FK → app_user |
| type | VARCHAR(50) | TASK_ASSIGNED, STATUS_CHANGED, TASK_OVERDUE |
| title | VARCHAR(200) | NOT NULL |
| body | VARCHAR(1000) | nullable |
| target_type | VARCHAR(30) | PRODUCTION_TASK, ORDER |
| target_id | VARCHAR(100) | taskNumber (человекочитаемый) |
| read | BOOLEAN | default false |
| read_at | TIMESTAMPTZ | nullable |
| created_at | TIMESTAMPTZ | NOT NULL |

**Новые значения type**: TASK_ASSIGNED, STATUS_CHANGED, TASK_OVERDUE — VARCHAR storage,
расширение без миграции.

### ProductionTask (существующая, таблица `production_task`)

Релевантные поля для триггеров:

| Field | Type | Usage in triggers |
|-------|------|-------------------|
| id | UUID | PK |
| task_number | VARCHAR | targetId в уведомлении |
| executor_user_id | UUID? | Получатель TASK_ASSIGNED и TASK_OVERDUE |
| created_by_user_id | UUID | Получатель STATUS_CHANGED и TASK_OVERDUE |
| status | VARCHAR | Определяет триггер STATUS_CHANGED; COMPLETED исключает из overdue |
| planned_finish_date | DATE? | Дедлайн для определения просрочки |

## New Port Methods (no schema changes)

### NotificationPersistencePort (add method)

```
existsByTypeAndTargetIdAndRecipient(type, targetId, recipientUserId) → Boolean
```

Используется для дедупликации TASK_OVERDUE.

### ProductionTaskPort (add method)

```
findOverdue(today: LocalDate) → List<ProductionTask>
```

Возвращает задачи с `planned_finish_date < today` и `status != COMPLETED` и `planned_finish_date IS NOT NULL`.

## Relationships

```
ProductionTask.executorUserId ──→ app_user.id (получатель TASK_ASSIGNED, TASK_OVERDUE)
ProductionTask.createdByUserId ──→ app_user.id (получатель STATUS_CHANGED, TASK_OVERDUE)
Notification.recipientUserId ──→ app_user.id (целевой пользователь)
Notification.targetId = ProductionTask.taskNumber (связь через человекочитаемый ID)
```

## No Migrations Required

Все изменения — на уровне application code. Новые значения enum хранятся как VARCHAR.
Новый метод `existsBy...` использует существующие столбцы таблицы `notification`.
Новый метод `findOverdue` использует существующие столбцы таблицы `production_task`.
