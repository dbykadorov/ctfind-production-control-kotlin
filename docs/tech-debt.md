# Tech Debt Backlog

Накапливающиеся технические долги, найденные в ходе разработки фич, но не блокирующие их закрытие. Каждая запись: где живёт, почему сейчас ОК, при каком условии становится больно, и какой ориентир по фиксу.

## TD-001 — `JpaProductionTaskAdapter.search` делает in-memory фильтрацию

**Источник:** Feature 005 (production tasks), 2026-04-28.
**Где:** `production-control-api/src/main/kotlin/com/ctfind/productioncontrol/production/adapter/persistence/ProductionTaskPersistenceAdapters.kt` — `JpaProductionTaskAdapter.search(...)`. Делает `taskRepository.findAll()`, фильтрует список через `filterProductionTaskEntitiesForQuery`, потом пагинирует подсписок.

**Почему сейчас ОК:** Phase 1 — до 50 пользователей и сотни задач. На таком масштабе разница между `findAll()` и предикатным запросом не заметна.

**Когда станет больно:** при выходе за ~1–2 тысячи задач каждый запрос `GET /api/production-tasks` тащит весь список из БД и держит сериализацию JPA-сущностей в памяти. Время ответа линейно растёт, плюс page heap.

**Ориентир по фиксу:** заменить `findAll()` на JPA Specification / Criteria-запрос с поддержкой всех фильтров `ProductionTaskListQuery`, серверной пагинацией и сортировкой. Перевести executor visibility в условие SQL (для PRODUCTION_EXECUTOR — `WHERE executor_user_id = :currentUserId`), а не в постфильтрацию. Возможно, потребуется композитный индекс по `(status, updated_at)` для типичной сортировки. Юнит-тесты `ProductionTaskQueryPersistenceTests` уже покрывают набор фильтров — переход должен быть «зелёным» по тем же ассертам.

## TD-002 — N+1 при резолве `displayName` в истории задачи

**Источник:** Feature 005, 2026-04-28.
**Где:** `production-control-api/src/main/kotlin/com/ctfind/productioncontrol/production/application/ProductionTaskHistoryUseCase.kt`. Каждый ивент в таймлайне резолвит `actorUserId` + `previousExecutorUserId` + `newExecutorUserId` через `ProductionActorLookupPort.displayName(userId)`. Реализация `JpaProductionActorLookupAdapter` дёргает `userRepository.findById(...)` per-call.

**Почему сейчас ОК:** типичная задача накапливает <20 событий, лента читается редко (страница detail), пользователей в системе <50. Разница между 20 SQL-запросами и одним JOIN на десятках мс не заметна.

**Когда станет больно:** при росте history (длинный жизненный цикл задачи, частые перепланировки) и/или при появлении дашбордов / отчётов, которые читают истории пачками. 100 задач × 20 событий × 3 лука́па = 6000 select-ов на одном экране.

**Ориентир по фиксу:** в `ProductionTaskHistoryUseCase` собрать уникальные UUID’ы (`actorUserId` + executor before/after), сделать **один** batch-запрос (например, `findAllById(ids)` или новый порт `displayNames(userIds: Set<UUID>): Map<UUID, String>`) и сложить в локальный кеш, потом маппить ивенты. Тесты `ProductionTaskHistoryUseCaseTests` уже фиксируют поведение и должны остаться зелёными.

## TD-003 — Web-тесты production-task контроллеров не проходят через Spring/MockMvc

**Источник:** Feature 005, 2026-04-28.
**Где:** `production-control-api/src/test/kotlin/com/ctfind/productioncontrol/production/adapter/web/ProductionTaskCreateControllerTests.kt`, `ProductionTaskAssignmentControllerTests.kt`, `ProductionTaskStatusControllerTests.kt`. Тесты создают `ProductionTaskController` руками со стабами use-case’ов и зовут методы напрямую.

**Почему сейчас ОК:** проверяют ветки маппинга `MutationResult → ResponseEntity` и команда-из-jwt — это и есть зона ответственности контроллера. Быстрые, без Spring-context overhead.

**Когда станет больно:** проблемы маршрутизации, Spring-валидации (`@Valid`, missing required field) и реальной интеграции с фильтрами безопасности всплывают только в smoke (как `LazyInitializationException` в `ProductionTaskQueryUseCase` — он бы поймался, если бы тесты шли через MockMvc и реальную JPA).

**Ориентир по фиксу:** добавить **один** `@SpringBootTest` (или `@WebMvcTest` + `@DataJpaTest` с testcontainers PostgreSQL) поверх production-tasks API, прогоняющий ключевой happy-path: login → create from order → assign → start → block → unblock → complete → detail с историей. Не нужно дублировать всё — нужен один интеграционный тест на счастливый путь и пара негативов (401, 403, 409, 422), которые сейчас проверяются юнит-уровнем.
