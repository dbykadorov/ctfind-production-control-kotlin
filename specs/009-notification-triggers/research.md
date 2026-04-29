# Research: Триггеры генерации уведомлений

## R1: Способ интеграции триггеров в существующие use-case'ы

**Decision**: Прямая инъекция `NotificationCreatePort` в use-case'ы production module.

**Rationale**: Существующие use-case'ы (`AssignProductionTaskUseCase`, `ChangeProductionTaskStatusUseCase`)
уже следуют паттерну «сохранить → записать history → записать audit». Уведомление — ещё один
побочный эффект в той же цепочке. Event-driven подход (Spring Events) добавил бы сложность
без выгоды: нет async-требования, нет нескольких подписчиков, нет replay.

**Alternatives considered**:
- Spring ApplicationEvents + @EventListener: избыточно, один подписчик, добавляет косвенность
- Domain Events в entity: нарушает pure Kotlin domain (без Spring)
- AOP/interceptor: скрывает бизнес-логику, сложнее тестировать

## R2: Fire-and-forget паттерн

**Decision**: try-catch вокруг `notifications.create()` с логированием ошибки через SLF4J.

**Rationale**: Согласно FR-007, ошибка уведомления не должна прерывать основную операцию.
Простой try-catch достаточен для single-instance deployment. Уведомление записывается в ту же
БД — вероятность ошибки минимальна (та же транзакция, та же connection).

**Alternatives considered**:
- @Async: усложняет транзакционную семантику, добавляет thread pool config
- Outbox pattern: избыточен без межсервисной коммуникации
- Retry: не нужен — если запись в ту же БД не прошла, повтор не поможет

## R3: Дедупликация TASK_OVERDUE

**Decision**: Query `existsByTypeAndTargetIdAndRecipient()` в `NotificationPersistencePort`.

**Rationale**: Проверка наличия существующего уведомления перед созданием — простейший подход.
Альтернатива (флаг `overdueNotified` на задаче) потребовала бы миграцию и модификацию доменной
модели production task ради побочного эффекта.

**Alternatives considered**:
- Флаг `overdueNotified` на ProductionTask: миграция + доменная модель загрязняется
- Отдельная таблица sent_notifications: избыточная абстракция для одного типа
- Set в памяти job'а: не переживёт рестарт

## R4: Scheduled job для просрочек

**Decision**: `@Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES)` на методе в `@Component`.

**Rationale**: Single-instance deployment, нет необходимости в distributed lock.
Spring @Scheduled — стандартный механизм для простых периодических задач.
15 минут — баланс между своевременностью и нагрузкой.

**Alternatives considered**:
- Quartz: избыточен для одного job'а
- ShedLock: нужен только при multiple instances
- Cron expression: fixedRate проще и предсказуемее

## R5: Overdue query в ProductionTaskPort

**Decision**: Новый метод `findOverdue(today: LocalDate): List<ProductionTask>` в `ProductionTaskPort`.

**Rationale**: Запрос «задачи с plannedFinishDate < today и status != COMPLETED» — это domain query,
который логично живёт в persistence port модуля production. JPA implementation — простой
`@Query` на repository.

**Alternatives considered**:
- Запрос через SQL напрямую из job: нарушает hexagonal, job не должен знать о JPA
- Фильтрация в памяти: неэффективно при большом количестве задач

## R6: @EnableScheduling

**Decision**: Проверить наличие `@EnableScheduling` в Spring конфигурации. Если отсутствует —
добавить на Application class или создать `SchedulingConfig`.

**Rationale**: Без `@EnableScheduling` аннотация `@Scheduled` игнорируется. Это стандартная
Spring Boot конфигурация.
