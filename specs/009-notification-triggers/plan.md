# Implementation Plan: Триггеры генерации уведомлений

**Branch**: `009-notification-triggers` | **Date**: 2026-04-29 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/009-notification-triggers/spec.md`

## Summary

Добавить автоматическую генерацию уведомлений при бизнес-событиях модуля production:
назначение задачи (TASK_ASSIGNED), смена статуса (STATUS_CHANGED), просрочка дедлайна (TASK_OVERDUE).
Интеграция — через существующий `NotificationCreatePort` (кросс-модульный порт из спеки 008).
Синхронные триггеры встраиваются в `AssignProductionTaskUseCase` и `ChangeProductionTaskStatusUseCase`;
проверка просрочек — через `@Scheduled` job.

## Technical Context

**Language/Version**: Kotlin, Java 21, Spring Boot 3  
**Primary Dependencies**: Spring Framework (DI, @Scheduled, @Transactional)  
**Storage**: PostgreSQL (notification table from spec 008, production_task table)  
**Testing**: JUnit 5 + kotlin.test, unit tests on use cases (no web layer)  
**Target Platform**: Linux server (Docker)  
**Project Type**: Modular monolith, web-service  
**Performance Goals**: Синхронные триггеры < 50ms overhead; scheduled job обрабатывает до 1000 задач  
**Constraints**: Fire-and-forget (ошибка уведомления не ломает основную операцию), single instance  
**Scale/Scope**: 3 триггера, 2 модифицированных use-case, 1 новый scheduled job, ~10 файлов

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **ERP domain fit**: ✅ Укрепляет рабочий процесс production tasks — ключевую операционную сущность.
  Уведомления превращают пассивное наблюдение за доской в активное информирование участников.

- **Constraint-aware operations**: ✅ TASK_OVERDUE фиксирует due-date pressure (plannedFinishDate).
  Факт уведомления о просрочке — сигнал для TOC-анализа буферов. STATUS_CHANGED трассирует flow
  history. Не блокирует будущие buffer management или bottleneck boards.

- **Architecture boundaries**: ✅ Триггеры живут в application layer (use cases). Notification
  создаётся через `NotificationCreatePort` (interface в notifications/application). Production module
  не импортирует persistence/web/domain классы notifications — только порт и команду.
  Scheduled job — в application layer production module.

- **Traceability/audit**: ✅ Каждое уведомление сохраняется в таблицу notification с createdAt,
  type, targetId. Audit log production module уже фиксирует операции; уведомления дополняют
  аудит информацией о том, кто был проинформирован.

- **API-only/security**: ✅ Нет новых API endpoints. Триггеры работают только внутри backend.
  Доступ к уведомлениям — через существующие endpoints спеки 008 с JWT auth.

- **Docker/verifiability**: ✅ Нет новых сервисов или портов. Docker startup не затрагивается.
  Верификация: `make backend-test-docker`, `make docker-up-detached && make health`,
  curl-тесты через quickstart.md.

- **Exception handling**: Нет нарушений конституции.

## Project Structure

### Documentation (this feature)

```text
specs/009-notification-triggers/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit-tasks)
```

### Source Code (repository root)

```text
src/main/kotlin/com/ctfind/productioncontrol/
├── notifications/
│   └── application/
│       ├── NotificationPorts.kt          # ADD: existsByTypeAndTargetIdAndRecipient() to persistence port
│       └── CreateNotificationUseCase.kt  # NO CHANGE (implements NotificationCreatePort)
└── production/
    └── application/
        ├── AssignProductionTaskUseCase.kt          # MODIFY: inject NotificationCreatePort, add trigger
        ├── ChangeProductionTaskStatusUseCase.kt    # MODIFY: inject NotificationCreatePort, add trigger
        └── OverdueTaskNotificationJob.kt           # NEW: @Scheduled job for TASK_OVERDUE

src/main/kotlin/com/ctfind/productioncontrol/
└── notifications/
    └── adapter/
        └── persistence/
            └── NotificationPersistenceAdapter.kt   # MODIFY: implement existsBy... method

src/test/kotlin/com/ctfind/productioncontrol/
└── production/
    └── application/
        ├── AssignProductionTaskNotificationTests.kt        # NEW: unit tests
        ├── ChangeProductionTaskStatusNotificationTests.kt  # NEW: unit tests
        └── OverdueTaskNotificationJobTests.kt              # NEW: unit tests
```

**Structure Decision**: Модификация существующих use cases в production/application + один новый
scheduled job. Тесты — в production/application (рядом с тестируемым кодом). Нет новых модулей,
нет новых контроллеров, нет миграций.

## Complexity Tracking

Нет нарушений конституции.

## Integration Design

### Trigger 1: TASK_ASSIGNED (в AssignProductionTaskUseCase)

**Точка вставки**: после `executorChanged` ветки (после `tasks.save()` и `traces.saveHistoryEvent()`).

```kotlin
// After audit.record() in executorChanged branch:
try {
    notifications.create(CreateNotificationCommand(
        recipientUserId = cmd.executorUserId,
        type = NotificationType.TASK_ASSIGNED,
        title = "Вам назначена задача ${updated.taskNumber}",
        targetType = NotificationTargetType.PRODUCTION_TASK,
        targetId = updated.taskNumber,
    ))
} catch (e: Exception) {
    log.warn("Failed to create TASK_ASSIGNED notification for task {}", updated.taskNumber, e)
}
```

**Условия**: `executorChanged == true` (проверяется существующей логикой use case).

### Trigger 2: STATUS_CHANGED (в ChangeProductionTaskStatusUseCase)

**Точка вставки**: после `tasks.save()`, `traces.saveHistoryEvent()`, `audit.record()`.

```kotlin
// After audit.record():
if (cmd.actorUserId != saved.createdByUserId) {
    try {
        notifications.create(CreateNotificationCommand(
            recipientUserId = saved.createdByUserId,
            type = NotificationType.STATUS_CHANGED,
            title = "Задача ${saved.taskNumber}: статус изменён на ${to.name}",
            targetType = NotificationTargetType.PRODUCTION_TASK,
            targetId = saved.taskNumber,
        ))
    } catch (e: Exception) {
        log.warn("Failed to create STATUS_CHANGED notification for task {}", saved.taskNumber, e)
    }
}
```

**Self-notification suppression**: `cmd.actorUserId != saved.createdByUserId`.

### Trigger 3: TASK_OVERDUE (OverdueTaskNotificationJob)

**Новый класс**: `production/application/OverdueTaskNotificationJob.kt`

```kotlin
@Component
class OverdueTaskNotificationJob(
    private val tasks: ProductionTaskPort,
    private val notifications: NotificationCreatePort,
    private val notificationQuery: NotificationPersistencePort, // for dedup
) {
    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    @Transactional
    fun checkOverdueTasks() {
        val overdue = tasks.findOverdue(LocalDate.now())
        for (task in overdue) {
            // Send to executor (if assigned)
            task.executorUserId?.let { sendIfNotDuplicate(it, task) }
            // Send to creator
            sendIfNotDuplicate(task.createdByUserId, task)
        }
    }

    private fun sendIfNotDuplicate(recipientUserId: UUID, task: ProductionTask) {
        if (notificationQuery.existsByTypeAndTargetIdAndRecipient(
                NotificationType.TASK_OVERDUE, task.taskNumber, recipientUserId)) {
            return
        }
        try {
            notifications.create(CreateNotificationCommand(
                recipientUserId = recipientUserId,
                type = NotificationType.TASK_OVERDUE,
                title = "Задача ${task.taskNumber} просрочена",
                targetType = NotificationTargetType.PRODUCTION_TASK,
                targetId = task.taskNumber,
            ))
        } catch (e: Exception) {
            log.warn("Failed to send TASK_OVERDUE for task {}", task.taskNumber, e)
        }
    }
}
```

**Dedup**: через `NotificationPersistencePort.existsByTypeAndTargetIdAndRecipient()` — новый метод.

**Overdue query**: `ProductionTaskPort.findOverdue(today: LocalDate)` — новый метод, возвращает
задачи с `plannedFinishDate < today` и `status != COMPLETED`.

### Новые методы в портах

1. **NotificationPersistencePort** (notifications/application):
   ```kotlin
   fun existsByTypeAndTargetIdAndRecipient(
       type: NotificationType, targetId: String, recipientUserId: UUID
   ): Boolean
   ```

2. **ProductionTaskPort** (production/application):
   ```kotlin
   fun findOverdue(today: LocalDate): List<ProductionTask>
   ```

### Паттерн fire-and-forget

Все вызовы `notifications.create()` обёрнуты в try-catch. Ошибка логируется через SLF4J logger,
но НЕ пробрасывается наверх. Основная транзакция use-case не откатывается.

### @EnableScheduling

Нужно убедиться что `@EnableScheduling` включён в Spring конфигурации. Если нет — добавить
на главный Application class или создать отдельный `SchedulingConfig`.
