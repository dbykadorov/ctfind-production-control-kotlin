# Tasks: Списание материалов под заказ + BOM

**Input**: Design documents from `/specs/012-order-bom-consumption/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Включены — проект использует JUnit 5 + MockK на бэке и Vitest source-text TDD на фронте; такая же политика была в спеках 008–011.

**Organization**: Tasks сгруппированы по user stories из spec.md (US1–US4). Каждая история — независимо тестируемый инкремент.

**Constitution**: Сохраняем доменные границы (use case-логика отдельно от контроллеров), TOC-факты (timestamps + actor + orderId на каждом movement), API-only бэкенд, audit на каждой мутации, Docker-first verification.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Можно выполнять параллельно (разные файлы, нет зависимостей внутри фазы)
- **[Story]**: К какой user story относится задача (US1 / US2 / US3 / US4)
- В описаниях указаны точные пути файлов

## Path Conventions

- **Backend (Kotlin)**: `src/main/kotlin/com/ctfind/productioncontrol/`, тесты — `src/test/kotlin/com/ctfind/productioncontrol/`
- **Frontend (Vue + TS)**: `frontend/cabinet/src/`, тесты — `frontend/cabinet/tests/unit/`
- **Migrations**: `src/main/resources/db/migration/`

---

## Phase 1: Setup

**Purpose**: Минимальная подготовка — фича расширяет существующие модули, новой инфраструктуры не вводит.

- [X] T001 Verify migration head в `src/main/resources/db/migration/` — последняя миграция `V8__create_inventory_tables.sql`. Подтвердить, что V9 будет следующим номером, и что docker-postgres-волюм может быть сброшен на dev (`make docker-reset`) перед прогоном новой миграции.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: База, на которую опираются все user stories — миграция, новые domain-сущности, расширение портов и cross-module-адаптер.

**⚠️ CRITICAL**: Ни одна user story не может стартовать, пока эта фаза не завершена.

- [X] T002 Создать миграцию `src/main/resources/db/migration/V9__bom_and_consumption.sql` согласно [data-model.md §7](./data-model.md): `CREATE TABLE order_material_requirement` с UNIQUE(order_id, material_id) и индексами; `ALTER TABLE stock_movement ADD COLUMN order_id UUID REFERENCES customer_order(id)` + `chk_movement_order_consistency` CHECK + `idx_stock_movement_order_id`.
- [X] T003 [P] Расширить enum `MovementType` в `src/main/kotlin/com/ctfind/productioncontrol/inventory/domain/StockMovement.kt`: добавить `CONSUMPTION`. Добавить поле `orderId: UUID?` в data class `StockMovement` и инвариант `init {}` (RECEIPT⇔orderId==null, CONSUMPTION⇔orderId!=null).
- [X] T004 [P] Создать domain entity `OrderMaterialRequirement` в `src/main/kotlin/com/ctfind/productioncontrol/inventory/domain/OrderMaterialRequirement.kt` согласно [data-model.md §1](./data-model.md): поля id/orderId/materialId/quantity/comment/createdAt/updatedAt, инварианты quantity>0 и comment.length≤500.
- [X] T005 Test для domain-инвариантов в `src/test/kotlin/com/ctfind/productioncontrol/inventory/domain/StockMovementTest.kt` (RECEIPT с orderId → require fails; CONSUMPTION без orderId → require fails; quantity≤0 → require fails) и `src/test/kotlin/com/ctfind/productioncontrol/inventory/domain/OrderMaterialRequirementTest.kt` (quantity≤0 fails; comment > 500 fails).
- [X] T006 [P] Расширить JPA entity `StockMovementEntity` в `src/main/kotlin/com/ctfind/productioncontrol/inventory/adapter/persistence/InventoryJpaEntities.kt`: добавить `@Column(name = "order_id") var orderId: UUID? = null`. Добавить новую `@Entity OrderMaterialRequirementEntity` (поля по data-model.md §1).
- [X] T007 [P] Расширить `src/main/kotlin/com/ctfind/productioncontrol/inventory/adapter/persistence/InventoryJpaRepositories.kt`: добавить `OrderMaterialRequirementJpaRepository` с методами `findByOrderIdOrderByCreatedAtDesc`, `findByOrderIdAndMaterialId`, `existsByOrderIdAndMaterialId`, `existsByMaterialId`, `findActiveOrderIdsByMaterialId(@Query JPQL, исключающий SHIPPED)`. Добавить методы `StockMovementJpaRepository`: `existsByOrderIdAndMaterialIdAndMovementType`, `sumQuantityByOrderIdAndMaterialIdAndMovementType`, `findActiveOrdersWithBomFor` (см. order-lookup-port.md).
- [X] T008 [P] Расширить `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/InventoryPorts.kt`: добавить `OrderMaterialRequirementPort` (CRUD по BOM-строкам), методы `StockMovementPort.hasConsumption(orderId, materialId)` и `StockMovementPort.sumConsumedByOrder(orderId): Map<UUID, BigDecimal>`. Добавить новый `OrderLookupPort` с DTO `InventoryOrderSummary` и `ActiveOrderSearchQuery` согласно [contracts/order-lookup-port.md](./contracts/order-lookup-port.md).
- [X] T009 [P] Расширить `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/InventoryPermissions.kt`: добавить константы `ORDER_MANAGER_ROLE_CODE` (re-export из orders.application) и `PRODUCTION_SUPERVISOR_ROLE_CODE`; функции `canEditBom`, `canConsumeStock`, `canViewBomAndUsage` согласно [research.md R-006](./research.md).
- [X] T010 [P] Создать cross-module адаптер `src/main/kotlin/com/ctfind/productioncontrol/inventory/adapter/persistence/OrderLookupAdapter.kt` (`@Component`), реализующий `OrderLookupPort` через `CustomerOrderJpaRepository` + `CustomerJpaRepository` + `OrderMaterialRequirementJpaRepository`. См. [contracts/order-lookup-port.md](./contracts/order-lookup-port.md).
- [X] T011 Расширить `InventoryPersistenceAdapter` в `src/main/kotlin/com/ctfind/productioncontrol/inventory/adapter/persistence/InventoryPersistenceAdapter.kt`: реализовать новые методы `StockMovementPort`, добавить реализацию `OrderMaterialRequirementPort` (mapping toDomain/toEntity для нового entity, обновить существующие toDomain/toEntity для StockMovement с orderId).
- [X] T012 Test `src/test/kotlin/com/ctfind/productioncontrol/inventory/adapter/persistence/OrderLookupAdapterTest.kt` (Spring + Postgres-Testcontainers): создать заказ NEW + BOM-строку + заказ SHIPPED + заказ NEW без BOM → `searchActiveOrdersForConsumption` возвращает ровно один — NEW с BOM; `findOrderSummary(unknownId)` → null.
- [X] T013 [P] Test `src/test/kotlin/com/ctfind/productioncontrol/inventory/adapter/persistence/OrderMaterialRequirementJpaRepositoryTest.kt`: проверить UNIQUE(order_id, material_id) и базовый CRUD.

**Checkpoint**: Foundation готова — миграция применяется чисто, новые типы и порты собираются, persistence-тесты зелёные. Можно начинать user stories параллельно.

---

## Phase 3: User Story 1 — Спецификация материалов в заказе (BOM CRUD) (Priority: P1) 🎯 MVP

**Goal**: Менеджер заказа добавляет, редактирует и удаляет строки BOM в карточке заказа. Уникальность (order, material), блокировка для SHIPPED, блокировка удаления при наличии списаний.

**Independent Test**: Войти как Order Manager → открыть карточку заказа → секция «Материалы (BOM)» → добавить «Фанера 10м²» → отредактировать на 12 → удалить (без списаний — успех). Попробовать дубль → отказ. Перевести заказ в SHIPPED → кнопки disabled.

### Backend — Use cases (US1)

- [X] T014 [P] [US1] Расширить `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/InventoryModels.kt`: добавить commands `AddBomLineCommand`, `UpdateBomLineCommand`, `RemoveBomLineCommand`, view `BomLineView`, мэппинг `OrderMaterialRequirement.toView(materialName, materialUnit)`.
- [X] T015 [P] [US1] Создать `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/AddBomLineUseCase.kt` (dual-constructor `@Service` pattern из `ReceiveStockUseCase`): RBAC `canEditBom`, проверка `OrderLookupPort.findOrderSummary` (404+!shipped+409), проверка `MaterialPort.findById` (404), проверка дубля через `OrderMaterialRequirementPort` (409 BOM_LINE_DUPLICATE), сохранение, audit `BOM_LINE_ADDED`. Возвращает `InventoryMutationResult<BomLineView>`.
- [X] T016 [P] [US1] Создать `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/UpdateBomLineUseCase.kt`: RBAC, lookup строки + заказа, валидация quantity/comment, обновление updatedAt, audit `BOM_LINE_UPDATED` с diff before/after.
- [X] T017 [P] [US1] Создать `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/RemoveBomLineUseCase.kt`: RBAC, lookup, проверка `StockMovementPort.hasConsumption(orderId, materialId)` → 409 BOM_LINE_HAS_CONSUMPTION, удаление, audit `BOM_LINE_REMOVED`.
- [X] T018 [US1] Расширить `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/InventoryQueryUseCase.kt`: метод `listBom(orderId): List<BomLineView>?` (null если заказа нет, иначе строки отсортированы createdAt DESC, обогащены materialName/materialUnit одним доп. lookup'ом).
- [X] T019 [US1] Расширить правило удаления материала в `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/DeleteMaterialUseCase.kt`: дополнительная проверка `OrderMaterialRequirementPort.existsInActiveOrder(materialId)` перед удалением (см. [data-model.md §3](./data-model.md), [research.md R-001/R-006](./research.md)).

### Backend — Use case tests (US1)

- [X] T020 [P] [US1] Test `src/test/kotlin/com/ctfind/productioncontrol/inventory/application/AddBomLineUseCaseTest.kt` (MockK): forbidden без ORDER_MANAGER, NotFound на отсутствующем заказе, ORDER_LOCKED на SHIPPED, MATERIAL_NOT_FOUND, BOM_LINE_DUPLICATE, ValidationFailed на quantity≤0, Success + audit emitted.
- [X] T021 [P] [US1] Test `src/test/kotlin/com/ctfind/productioncontrol/inventory/application/UpdateBomLineUseCaseTest.kt`: forbidden, BOM_LINE_NOT_FOUND, ORDER_LOCKED, ValidationFailed, Success + audit с diff.
- [X] T022 [P] [US1] Test `src/test/kotlin/com/ctfind/productioncontrol/inventory/application/RemoveBomLineUseCaseTest.kt`: forbidden, NOT_FOUND, ORDER_LOCKED, BOM_LINE_HAS_CONSUMPTION, Success + audit.
- [X] T023 [P] [US1] Test `src/test/kotlin/com/ctfind/productioncontrol/inventory/application/DeleteMaterialActiveBomLineTest.kt`: материал в BOM активного заказа → блокировка; в BOM только SHIPPED заказов и без movements → разрешено.

### Backend — Web layer (US1)

- [X] T024 [US1] Расширить `src/main/kotlin/com/ctfind/productioncontrol/inventory/adapter/web/InventoryDtos.kt`: `BomLineResponse`, `BomLineListResponse`, `BomLineCreateRequest`, `BomLineUpdateRequest`, маппинг `BomLineView.toResponse()`. Расширить `InventoryApiError` для кодов `ORDER_NOT_FOUND`, `ORDER_LOCKED`, `BOM_LINE_DUPLICATE`, `BOM_LINE_HAS_CONSUMPTION`, `BOM_LINE_NOT_FOUND`, `MATERIAL_NOT_IN_BOM`.
- [X] T025 [US1] Создать `src/main/kotlin/com/ctfind/productioncontrol/inventory/adapter/web/BomController.kt` (`@RestController @RequestMapping("/api/orders/{orderId}/bom")`): GET (list), POST (add), PUT (update — `/{lineId}`), DELETE (`/{lineId}`). JWT-actor → `AuthenticatedInventoryActor` через существующий хелпер. Маппинг `InventoryMutationResult` → HTTP-коды по [contracts/rest-bom.md](./contracts/rest-bom.md).
- [X] T026 [P] [US1] Test `src/test/kotlin/com/ctfind/productioncontrol/inventory/adapter/web/BomControllerTest.kt` (MockMvc + MockK use cases): по одному happy-path на каждый метод + проверка 401/403/404/409 для ключевых сценариев из rest-bom.md.

### Frontend — types + composable (US1)

- [X] T027 [P] [US1] Расширить `frontend/cabinet/src/api/types/warehouse.ts` (или вынести в новый `frontend/cabinet/src/api/types/inventory.ts`): типы `BomLine`, `BomLineCreateRequest`, `BomLineUpdateRequest`, `BomLineListResponse`. Точно соответствуют OpenAPI из `contracts/openapi.yaml`.
- [X] T028 [US1] Создать composable `frontend/cabinet/src/api/composables/use-order-bom.ts`: `useOrderBom(orderId)` → `{ lines, loading, error, refetch, addLine(req), updateLine(id, req), removeLine(id) }`. Использует существующий `apiClient`.
- [X] T029 [P] [US1] Расширить `frontend/cabinet/src/api/composables/use-permissions.ts`: добавить computed `canEditOrderBom`, `canViewOrderBom`. Источник — `useAuthStore().roles` (codes `ADMIN`, `ORDER_MANAGER`, `WAREHOUSE`, `PRODUCTION_SUPERVISOR`).

### Frontend — компоненты (US1)

- [X] T030 [P] [US1] Создать `frontend/cabinet/src/components/domain/orders/BomLineDialog.vue`: shadcn Dialog с формой (Material Select + quantity Input + Textarea для comment). `<template #footer>` за пределами `<form>`, submit через `form="bom-line-form"`. Поддержка режимов add/edit. Emit `saved` на успех. Использует пропсы `orderId`, `editingLine?: BomLine`, `availableMaterials: Material[]`.
- [X] T031 [P] [US1] Создать `frontend/cabinet/src/components/domain/orders/BomSection.vue`: таблица BomLines (material name, unit, quantity, comment, actions), кнопка «Добавить материал», обработчик клика → `BomLineDialog`. Используются `useOrderBom(orderId)` и `useMaterials()` для списка материалов в picker. Действия (edit/delete) скрываются при `!canEditOrderBom` или `order.status === 'SHIPPED'` (входит prop `orderShipped: boolean`).
- [X] T032 [US1] Интегрировать `BomSection` в `frontend/cabinet/src/pages/office/OrderDetailPage.vue`: импорт + рендер ниже основной информации заказа. Прокинуть `:order-id` и `:order-shipped`.
- [X] T033 [P] [US1] Расширить i18n в `frontend/cabinet/src/i18n/keys.ts` и `frontend/cabinet/src/i18n/ru.ts`: ключи `bom.section.title`, `bom.empty`, `bom.add`, `bom.edit`, `bom.delete`, `bom.duplicate`, `bom.cannotDeleteWithConsumption`, `bom.lockedShipped`, валидация `bom.quantityRequired`, `bom.quantityPositive`. Тексты на русском.

### Frontend — тесты (US1)

- [X] T034 [P] [US1] Test `frontend/cabinet/tests/unit/composables/use-order-bom.test.ts`: mock fetch — list/add/update/remove updates `lines` правильно; error path сохраняет error в state.
- [X] T035 [P] [US1] Source-text test `frontend/cabinet/tests/unit/components/BomSection.test.ts` (паттерн readFileSync + строковые ассерты, как `WarehouseListPage.test.ts`): проверить i18n-ключи, наличие классов и пропсов, presence условного рендеринга для disabled кнопок.
- [X] T036 [P] [US1] Source-text test `frontend/cabinet/tests/unit/components/BomLineDialog.test.ts`: i18n-ключи, валидация quantity≥0.0001, structure form/footer.

**Checkpoint**: Менеджер может полноценно вести BOM в карточке заказа. На этом этапе списания ещё нет — секция «Расход материалов» не реализована.

---

## Phase 4: User Story 2 — Списание материала под заказ (Priority: P1)

**Goal**: Кладовщик списывает материал по заказу из карточки заказа (основной вход) или из раздела «Склад» (альтернативный). Блокировка остатка, перерасход — warning.

**Independent Test**: Создать материал «Фанера», приход 50 → создать заказ + BOM 10 → списать 5 (остаток 45) → списать 100 (блок INSUFFICIENT_STOCK). Списать через `WarehouseListPage` (picker заказа). Перевод в SHIPPED → кнопка списания disabled.

**⚠️ Зависимость от US1**: тестовый сценарий требует BOM-строку, но имплементационно US2 от US1 не зависит — оба строятся на foundational. На бэке порядок не важен. На фронте используется одинаковый `useMaterials`, но разные dialog-компоненты.

### Backend — Use cases (US2)

- [X] T037 [P] [US2] Расширить `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/InventoryModels.kt`: `ConsumeStockCommand` (materialId/orderId/quantity/comment/actor), `ConsumeStockResult` view с denormalized orderNumber/materialName/materialUnit. Расширить `InventoryMutationResult` ветками `OrderLocked`, `MaterialNotInBom`, `InsufficientStock(available)`.
- [X] T038 [P] [US2] Создать `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/ConsumeStockUseCase.kt`. Алгоритм по [research.md R-005](./research.md): `@Transactional`, RBAC `canConsumeStock`, **`PESSIMISTIC_WRITE` lock** на `material` row через `MaterialPort.findByIdForUpdate(id)` (нужно добавить в порт), валидация существования заказа и `!shipped`, проверка `OrderMaterialRequirementPort.existsByOrderIdAndMaterialId`, recompute `currentStock = sumReceipts - sumConsumptions`, сравнение с requested, save StockMovement(CONSUMPTION, orderId), audit `STOCK_CONSUMPTION`.
- [X] T039 [US2] Расширить `MaterialPort` в `InventoryPorts.kt` методом `findByIdForUpdate(id): Material?`. Реализовать в `InventoryPersistenceAdapter` через `@Lock(LockModeType.PESSIMISTIC_WRITE)` на новом методе репозитория `MaterialJpaRepository.findByIdForUpdate(id)`.
- [X] T040 [US2] Расширить активные заказы для UI-picker'а: `searchActiveOrdersForConsumption` в `OrderLookupAdapter` уже есть из foundational; убедиться, что фильтр «`SHIPPED` исключён + есть хотя бы одна BOM-строка» работает как в [order-lookup-port.md](./contracts/order-lookup-port.md).

### Backend — Use case tests (US2)

- [X] T041 [P] [US2] Test `src/test/kotlin/com/ctfind/productioncontrol/inventory/application/ConsumeStockUseCaseTest.kt` (MockK): forbidden, MATERIAL_NOT_FOUND, ORDER_NOT_FOUND, ORDER_LOCKED (SHIPPED), MATERIAL_NOT_IN_BOM, ValidationFailed (qty≤0), INSUFFICIENT_STOCK с available, Success при перерасходе по BOM (warning не блокирует), Success + audit STOCK_CONSUMPTION.
- [X] T042 [P] [US2] Concurrency-test (Spring + Testcontainers) `src/test/kotlin/com/ctfind/productioncontrol/inventory/application/ConsumeStockConcurrencyTest.kt`: остаток 50, два потока списывают по 30 → один Success, другой INSUFFICIENT_STOCK; final stock = 20 либо 50 (зависит от порядка, оба валидны), но не отрицателен.

### Backend — Web layer (US2)

- [X] T043 [US2] Расширить `src/main/kotlin/com/ctfind/productioncontrol/inventory/adapter/web/InventoryDtos.kt`: `ConsumeRequest`, расширить `StockMovementResponse` опциональными `orderId`, `orderNumber`. Добавить shape ошибки `INSUFFICIENT_STOCK` с полем `available` в `InventoryApiError`.
- [X] T044 [US2] Расширить `src/main/kotlin/com/ctfind/productioncontrol/inventory/adapter/web/InventoryController.kt`: новый endpoint `POST /api/materials/{id}/consume`. Маппинг результатов согласно [contracts/rest-consumption.md](./contracts/rest-consumption.md).
- [X] T045 [US2] Создать `src/main/kotlin/com/ctfind/productioncontrol/inventory/adapter/web/ActiveOrdersController.kt` (`@RestController @RequestMapping("/api/orders/active-for-consumption")`): GET с query-params `search`, `limit`. RBAC `canConsumeStock`. Возвращает `InventoryOrderList` по openapi.yaml.
- [X] T046 [P] [US2] Test `src/test/kotlin/com/ctfind/productioncontrol/inventory/adapter/web/InventoryControllerConsumeTest.kt` (MockMvc): happy-path POST consume, 400 на невалидном body, 403 без WAREHOUSE, 404 материала, 409 INSUFFICIENT_STOCK с полем `available`.
- [X] T047 [P] [US2] Test `src/test/kotlin/com/ctfind/productioncontrol/inventory/adapter/web/ActiveOrdersControllerTest.kt`: 200 для WAREHOUSE с фильтрацией, 403 для ORDER_MANAGER, корректный фильтр `search`.

### Frontend — types + composables (US2)

- [X] T048 [P] [US2] Расширить `frontend/cabinet/src/api/types/warehouse.ts`: `ConsumeRequest`, `StockMovement` (расширить opt `orderId/orderNumber`), `InsufficientStockError`, `InventoryOrderSummary`, `InventoryOrderList`.
- [X] T049 [P] [US2] Создать `frontend/cabinet/src/api/composables/use-stock-consumption.ts`: `useStockConsumption()` → `{ consume(materialId, request), loading, error }`. Парсинг 409 INSUFFICIENT_STOCK с возвратом `available` в error для отображения в форме.
- [X] T050 [P] [US2] Создать `frontend/cabinet/src/api/composables/use-active-orders.ts`: `useActiveOrders()` → `{ items, loading, search(q) }` для picker'а в alt-entry. Дебаунс 300ms, минимум 2 символа.
- [X] T051 [P] [US2] Расширить `use-permissions.ts`: добавить `canConsumeStock` (ADMIN или WAREHOUSE).

### Frontend — компоненты (US2)

- [X] T052 [US2] Создать `frontend/cabinet/src/components/domain/warehouse/StockConsumeDialog.vue`: общий компонент для двух точек входа. Props: `preselectedOrder?: InventoryOrderSummary`. Если передан — picker скрыт; иначе рендерится autocomplete на `useActiveOrders`. Внутри: выбор материала из BOM выбранного заказа (использует `useOrderBom(orderId).lines` для списка), input quantity, textarea comment. Warning «Перерасход +X» если `quantity + already_consumed > required` (вычисляется через `use-order-material-usage` или передаётся пропсом). Emit `consumed` после успеха. Маршрут ошибки INSUFFICIENT_STOCK → показать в форме с available.
- [X] T053 [US2] Расширить `frontend/cabinet/src/pages/warehouse/WarehouseListPage.vue`: новая кнопка «Списать» (видна при `canConsumeStock`), открывает `StockConsumeDialog` без `preselectedOrder`. На событие `consumed` — refetch списка материалов (для обновления остатков).
- [X] T054 [P] [US2] Расширить i18n `frontend/cabinet/src/i18n/keys.ts` + `ru.ts`: `consume.button`, `consume.title`, `consume.pickOrder`, `consume.pickMaterial`, `consume.quantity`, `consume.comment`, `consume.submit`, `consume.insufficientStock`, `consume.available`, `consume.overconsumption`, `consume.materialNotInBom`, `consume.orderLocked`.

### Frontend — тесты (US2)

- [X] T055 [P] [US2] Test `frontend/cabinet/tests/unit/composables/use-stock-consumption.test.ts`: успех; INSUFFICIENT_STOCK парсится с available; ORDER_LOCKED отдаётся как структурированная ошибка.
- [X] T056 [P] [US2] Test `frontend/cabinet/tests/unit/composables/use-active-orders.test.ts`: дебаунс search; пустой items при отсутствии активных заказов; 403 не валит composable.
- [X] T057 [P] [US2] Source-text test `frontend/cabinet/tests/unit/components/StockConsumeDialog.test.ts`: i18n-ключи, presence пикера материала, conditional rendering picker заказа, presence предупреждения о перерасходе.

**Checkpoint**: Кладовщик может списать материал двумя способами; перерасход — visible warning, недостаток остатка — блок.

---

## Phase 5: User Story 3 — Расход материалов по заказу (Priority: P1)

**Goal**: Карточка заказа показывает агрегированный расход: материал / требуется / списано / остаток к списанию + индикатор перерасхода.

**Independent Test**: BOM «Фанера 10м², Гвозди 100шт» → списать 4м² фанеры и 30шт гвоздей → таблица показывает «10/4/6», «100/30/70». Списать ещё 8 фанеры (перерасход +2) → строка показывает «10/12/0» с индикатором +2.

**⚠️ Зависимость от US2**: реальное наблюдение расхода работает только когда есть consumption-движения. Имплементация US3 не блокируется US2 — read-only-агрегат можно проверить с пустыми списаниями.

### Backend (US3)

- [X] T058 [P] [US3] Расширить `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/InventoryModels.kt`: `MaterialUsageRowView`, `MaterialUsageView` согласно [data-model.md §5](./data-model.md).
- [X] T059 [US3] Расширить `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/InventoryQueryUseCase.kt`: метод `getMaterialUsage(orderId): MaterialUsageView?`. SQL/JPQL: LEFT JOIN BOM ⨝ агрегат `SUM(quantity)` по `stock_movement WHERE order_id = ? AND movement_type = 'CONSUMPTION' GROUP BY material_id`. Возвращает null если заказа нет.
- [X] T060 [US3] Создать `src/main/kotlin/com/ctfind/productioncontrol/inventory/adapter/web/MaterialUsageController.kt`: `GET /api/orders/{orderId}/material-usage`. RBAC `canViewBomAndUsage`. 404 если заказа нет.
- [X] T061 [US3] Расширить `src/main/kotlin/com/ctfind/productioncontrol/inventory/adapter/web/InventoryDtos.kt`: `MaterialUsageRowResponse`, `MaterialUsageResponse`.
- [X] T062 [P] [US3] Test `src/test/kotlin/com/ctfind/productioncontrol/inventory/application/MaterialUsageQueryTest.kt`: пустой BOM → пустой rows; BOM без списаний → consumed=0, remaining=required; частичное списание → корректные значения; перерасход → remaining=0 + overconsumption>0.
- [X] T063 [P] [US3] Test `src/test/kotlin/com/ctfind/productioncontrol/inventory/adapter/web/MaterialUsageControllerTest.kt`: 200, 403, 404.

### Frontend (US3)

- [X] T064 [P] [US3] Расширить `frontend/cabinet/src/api/types/warehouse.ts`: `MaterialUsageRow`, `MaterialUsage`.
- [X] T065 [US3] Создать composable `frontend/cabinet/src/api/composables/use-order-material-usage.ts`: `useOrderMaterialUsage(orderId)` → `{ usage, loading, error, refetch }`.
- [X] T066 [P] [US3] Создать `frontend/cabinet/src/components/domain/orders/MaterialUsageSection.vue`: таблица (material, unit, required, consumed, remaining), бэйдж перерасхода для строк с `overconsumption > 0`, пустое состояние «Добавьте материалы в BOM, чтобы начать учёт расхода». Кнопка «Списать» в шапке (видна при `canConsumeStock && !orderShipped`) — открывает `StockConsumeDialog` с preselectedOrder. На событие `consumed` — refetch usage и BOM.
- [X] T067 [US3] Интегрировать `MaterialUsageSection` в `frontend/cabinet/src/pages/office/OrderDetailPage.vue` ниже `BomSection`. Прокинуть `:order-id` и `:order-shipped`. Связать события так, чтобы `consumed` из StockConsumeDialog внутри MaterialUsageSection триггерил refetch и для BomSection.
- [X] T068 [P] [US3] Расширить i18n: `usage.section.title`, `usage.empty`, `usage.column.required`, `usage.column.consumed`, `usage.column.remaining`, `usage.overconsumption`, `usage.consumeButton`.
- [X] T069 [P] [US3] Source-text test `frontend/cabinet/tests/unit/components/MaterialUsageSection.test.ts`: i18n-ключи, conditional рендеринг бэйджа перерасхода, пустое состояние, presence кнопки «Списать» только для подходящих ролей.
- [X] T070 [US3] Обновить `frontend/cabinet/tests/unit/pages/OrderDetailPage.test.ts` (если такой существует — иначе создать): проверить, что новые секции `BomSection` и `MaterialUsageSection` рендерятся в DOM-разметке.

**Checkpoint**: Все три ключевых story (P1) функциональны. На этом этапе можно делать MVP-демо фичи 012.

---

## Phase 6: User Story 4 — Аудит операций (Priority: P2)

**Goal**: Все мутации (BOM_LINE_ADDED, BOM_LINE_UPDATED, BOM_LINE_REMOVED, STOCK_CONSUMPTION) появляются в журнале аудита под категорией «Склад» с метаданными.

**Independent Test**: Под Order Manager — добавить BOM, отредактировать, удалить. Под WAREHOUSE — списать. Под admin открыть `/cabinet/audit`, фильтр INVENTORY → видны 4 разных типа событий с актором, заказом и материалом.

**Note**: Эмиссия audit-событий уже встроена в use cases US1/US2 (T015–T017, T038). US4 покрывает чтение (audit-log-страница) и проверку формата.

### Backend (US4)

- [X] T071 [P] [US4] Проверить и при необходимости расширить `src/main/kotlin/com/ctfind/productioncontrol/audit/adapter/persistence/AuditPersistenceAdapter.kt` (`fetchInventoryEvents`): убедиться, что новые `event_type` (`BOM_LINE_ADDED`, `BOM_LINE_UPDATED`, `BOM_LINE_REMOVED`, `STOCK_CONSUMPTION`) попадают в выборку и фильтр работает. Если хардкоженный `targetType = "MATERIAL"` мешает — пересмотреть для BOM-событий.
- [X] T072 [P] [US4] Test `src/test/kotlin/com/ctfind/productioncontrol/inventory/application/InventoryAuditEventEmissionTest.kt`: для каждой мутации (Add/Update/Remove BOM, Consume) проверить — что в `InventoryAuditPort.record(event)` событие попало с правильным `eventType`, актором, корректным JSON-metadata (для UPDATE — diff before/after).
- [X] T073 [P] [US4] Integration test `src/test/kotlin/com/ctfind/productioncontrol/audit/AuditQueryInventoryEventsTest.kt`: применить миграции, выполнить серию мутаций через use cases, прочитать через AuditQueryUseCase с категорией `INVENTORY` — все 4 типа событий присутствуют в правильном порядке.

### Frontend (US4)

- [X] T074 [P] [US4] Verify `frontend/cabinet/src/pages/audit/AuditLogPage.vue`: фильтр `INVENTORY` (уже введён в спеке 011) показывает новые события. При необходимости — расширить i18n-метки для типов событий `audit.event.bomLineAdded`, `bomLineUpdated`, `bomLineRemoved`, `stockConsumption`.
- [X] T075 [P] [US4] Source-text test `frontend/cabinet/tests/unit/pages/AuditLogPage.test.ts` (расширение существующего теста): убедиться, что i18n-ключи для новых типов событий присутствуют и категория `INVENTORY` отображается с новыми типами.

**Checkpoint**: Полная traceability — admin может в журнале аудита разбирать любые расхождения по BOM и списаниям.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: финальная верификация, документация, проверка Constitution-гейтов.

- [X] T076 [P] Запустить backend-тесты в Docker: `make backend-test-docker`. Все новые JUnit-тесты должны проходить.
- [X] T077 [P] Frontend type check: `cd frontend/cabinet && pnpm typecheck` — нет ошибок.
- [X] T078 [P] Frontend test suite: `cd frontend/cabinet && pnpm vitest run` — все тесты зелёные.
- [X] T079 [P] Frontend build: `cd frontend/cabinet && pnpm build` — production-сборка собирается.
- [X] T080 Запуск стека и health-check: `make docker-reset && make docker-up-detached && make health` → `{"status":"UP"}`. Проверить, что V9 миграция применилась без ошибок.
- [X] T081 Ручной прогон всех 6 сценариев из `specs/012-order-bom-consumption/quickstart.md`. Зафиксировать результаты в pull-request описании.
- [X] T082 Проверка Constitution-гейтов post-implementation: business rules не утекли в контроллеры/DTO; cross-module-доступ только через `OrderLookupPort`; audit на всех мутациях; backend остался API-only; нет нарушений TOC-сохранности фактов (timestamp, actor, orderId на каждом movement).
- [X] T083 [P] Отметить выполненные таски `[X]` в этом файле, обновить `specs/012-order-bom-consumption/checklists/requirements.md` если появились новые открытые пункты.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T001 — мгновенно, no real work.
- **Foundational (Phase 2)**: T002–T013. **Блокирует все user stories.** T002 (миграция) должна примениться до T011 (где используются новые таблицы); T003–T010 могут идти параллельно после T002. T011 зависит от T006–T010. T012–T013 — после T011.
- **User Stories (Phase 3–6)**:
  - US1, US2, US3 могут идти параллельно после Phase 2 (разные файлы, разные стори).
  - US4 — после US1 и US2 (нужны мутации для проверки audit-emission), но имплементационно частично пересекается с US1/US2 (audit emission уже встроена). US4 в основном — про чтение и подтверждение.
- **Polish (Phase 7)**: после всех желаемых US.

### Внутри user stories

- Tests должны быть написаны и **должны падать** до имплементации (TDD).
- Domain → application → adapter (web/persistence) — порядок hexagonal-стиля.
- Backend → Frontend types → composable → component → page integration.
- i18n ключи → компоненты.

### Параллельные возможности

#### Phase 2 (после T002)
T003, T004, T006, T007, T008, T009, T010 — все [P], разные файлы, могут идти параллельно.

#### US1 implementation
T014–T017 — четыре независимых файла, [P]. T020–T023 — четыре независимых тестовых файла, [P]. На фронте T027, T029, T030, T033 — независимые, [P].

#### US2 implementation
T037, T038 — отдельные файлы. T041, T042, T046, T047 — независимые тесты. T048, T049, T050, T051 — независимые composable/types. T054 (i18n) — параллелится с T052.

#### Cross-story
US1, US2, US3 могут стартовать одновременно после T013. Однако T032 (интеграция в OrderDetailPage) и T067 (там же) — **последовательны**, иначе мердж-конфликт. Лучше сделать T032 в US1, потом T067 в US3.

---

## Parallel Example: User Story 1 (Backend)

```bash
# После T013 (foundational checkpoint), запустить параллельно:
Task: "T015 [P] [US1] Create AddBomLineUseCase.kt"
Task: "T016 [P] [US1] Create UpdateBomLineUseCase.kt"
Task: "T017 [P] [US1] Create RemoveBomLineUseCase.kt"
Task: "T014 [P] [US1] Extend InventoryModels.kt with BOM commands and views"

# После их завершения — три use case теста параллельно:
Task: "T020 [P] [US1] Test AddBomLineUseCaseTest.kt"
Task: "T021 [P] [US1] Test UpdateBomLineUseCaseTest.kt"
Task: "T022 [P] [US1] Test RemoveBomLineUseCaseTest.kt"
```

## Parallel Example: User Story 2 (Frontend)

```bash
# После backend US2:
Task: "T048 [P] [US2] Extend warehouse.ts with consume types"
Task: "T049 [P] [US2] Create use-stock-consumption.ts"
Task: "T050 [P] [US2] Create use-active-orders.ts"
Task: "T051 [P] [US2] Extend use-permissions.ts with canConsumeStock"
Task: "T054 [P] [US2] Add consume.* i18n keys"

# Затем компонент (зависит от composables):
Task: "T052 [US2] Create StockConsumeDialog.vue"
```

---

## Implementation Strategy

### MVP First (Phase 1 → Phase 2 → Phase 3)

1. **T001** — Setup verification.
2. **T002–T013** — Foundational. Без них ни US1, ни US2 не имеют под собой схемы.
3. **T014–T036** — US1 BOM CRUD. Это minimum viable feature — менеджер может вести спецификацию без списания (списание = stub-кнопка disabled).
4. **STOP and VALIDATE** — quickstart сценарии 1, 4 (lifecycle), 5 (delete blocking без consumption-блокировки), 6 (audit для BOM-операций).

### Incremental Delivery

После MVP:
- US3 (Расход) — добавляет видимость, но без списаний показывает только «требуется N / списано 0».
- US2 (Списание) — закрывает основной use case склада. После этого quickstart сценарии 2, 3, 5 полностью работают.
- US4 (Аудит) — verification + frontend i18n labels.
- Polish — финальная проверка на докере и quickstart end-to-end.

### Parallel Team Strategy

- **Dev A (Backend)**: T002–T013 → T014–T026 (US1 backend) → T037–T047 (US2 backend) → T058–T063 (US3 backend) → T071–T073 (US4).
- **Dev B (Frontend)**: ждёт Dev A до конца T026 → T027–T036 (US1 frontend), параллельно T048–T057 (US2 frontend), T064–T070 (US3 frontend), T074–T075 (US4 frontend).
- **Senior**: T012, T042 (Testcontainers-тесты), T080–T081 (deploy + quickstart).

---

## Notes

- [P] tasks = разные файлы, нет зависимостей.
- [Story] метка маппит таску на user story для traceability.
- US1/US2/US3 — все P1; в реальной поставке имеет смысл двигаться US1 → US2 → US3 (BOM нужен до списания, расход показывает результат списания).
- Tests **сначала**, ассерты должны падать до имплементации.
- Бизнес-правила — в use cases. Контроллеры не валидируют, только маппят `InventoryMutationResult` в HTTP-коды.
- На каждой мутации — audit-event. Use case не считается завершённым без audit-теста.
- Commit после каждой завершённой логической группы (use case + его тесты, компонент + его тесты).
- Stop at any checkpoint to validate story independently.
- Avoid: vague tasks, same-file conflicts (T032 и T067 — одна и та же `OrderDetailPage.vue`, поэтому последовательны).
