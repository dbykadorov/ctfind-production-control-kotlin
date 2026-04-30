# Research: Списание материалов под заказ + BOM

Phase 0 — выбор архитектурных решений и обоснование. Все NEEDS CLARIFICATION резолвятся ниже.

---

## R-001 — Маппинг «терминальных» статусов заказа

**Decision**: «Закрытый» статус, при котором запрещены изменения BOM (FR-005), списания (FR-012, FR-024) и удаление BOM-строк (FR-004), — это `OrderStatus.SHIPPED`. Статуса `CANCELLED` в текущей доменной модели нет.

**Rationale**: Чтение `orders/domain/OrderStatus.kt` показало enum из 4 значений: `NEW`, `IN_WORK`, `READY`, `SHIPPED`. Существующая политика редактирования заказа `OrderEditPolicy.canRegularEdit(order) = !order.shipped` уже использует `SHIPPED` как единственный read-only маркер. Спека (раздел Clarifications + FR-005/012/024) использовала русские лейблы «Завершён» и «Отменён» как обобщённые термины — они мапятся на `SHIPPED`; «Отменён» в системе не существует, и его нужно исключить из всех правил.

**Alternatives considered**:
- Ввести новый статус `CANCELLED` параллельно с этой спекой — отвергнуто: расширяет scope, влияет на orders-модуль и UI заказов, требует отдельной миграции state-machine.
- Считать `READY` тоже «закрытым» — отвергнуто: между «готов» и «отгружен» возможны последние списания/корректировки.

**Affected acceptance criteria**: AS US1#8, AS US2#4, FR-005, FR-012, FR-024, edge case про активный заказ при удалении материала. В реализации `canEditBom(order)` и `canConsume(order)` сводятся к `!order.shipped`.

---

## R-002 — Cross-module доступ inventory → orders

**Decision**: Новый порт `OrderLookupPort` в `inventory/application/InventoryPorts.kt` с минимально достаточным API:

```kotlin
interface OrderLookupPort {
    fun findOrderSummary(orderId: UUID): InventoryOrderSummary?
    fun searchActiveOrdersForConsumption(query: ActiveOrderSearchQuery): List<InventoryOrderSummary>
}

data class InventoryOrderSummary(
    val id: UUID,
    val orderNumber: String,
    val customerName: String,
    val status: OrderStatus,
    val shipped: Boolean,
)
```

Реализация — `OrderLookupAdapter` в `inventory/adapter/persistence/`, использует существующий `CustomerOrderJpaRepository` и `CustomerJpaRepository`. Вторичный метод нужен для US2 alt-entry (списание из склада с picker заказов).

**Rationale**: Соответствует паттерну `ProductionOrderSourcePort` из модуля `production` (см. `production/application/ProductionTaskPorts.kt:30-33`). Не тащит в inventory полную модель `CustomerOrder`, передаёт только то, что нужно для валидации статуса и UX-отображения. Реализация в `inventory/adapter/persistence/` сохраняет правило «inventory не импортирует orders.adapter.*».

**Alternatives considered**:
- Прямой импорт `CustomerOrderPort` из orders/application — отвергнуто: нарушает изоляцию модулей; inventory оказывается зависим от мутационных операций orders.
- REST self-call (`HttpClient` к `/api/orders/{id}`) — отвергнуто: оверхед, кольцевая зависимость в Spring, лишний http-trip внутри одного процесса.
- Domain event-bus (publish `OrderShipped` → inventory subscribes) — отвергнуто: преждевременная сложность, текущий монолит обходится синхронным портом.

---

## R-003 — Хранение `orderId` в `stock_movement`

**Decision**: В таблицу `stock_movement` добавляется nullable-колонка `order_id UUID REFERENCES customer_order(id)` без `ON DELETE` каскада. На уровне домена `StockMovement.orderId: UUID?`. Инвариант: для `MovementType.RECEIPT` всегда `orderId == null`; для `MovementType.CONSUMPTION` всегда `orderId != null`. Инвариант проверяется в `StockMovement.init {}`.

**Rationale**:
- Существующие записи приходов (V8) — без заказа, поэтому колонка должна быть nullable; backfill не нужен.
- `customer_order` в системе не удаляется (только меняет статус), поэтому `ON DELETE CASCADE` не применим. Если в будущем добавится hard-delete заказов, политика по списаниям должна быть явная (вероятно — `RESTRICT` для сохранения аудита).
- Индекс `idx_stock_movement_order_id` нужен для запроса агрегата расхода по заказу (FR-015).

**Alternatives considered**:
- Отдельная таблица `consumption` (не наследник `stock_movement`) — отвергнуто: фрагментирует журнал движений; `current_stock` потребует UNION двух источников; теряется единый аудит.
- `ON DELETE SET NULL` на `order_id` — отвергнуто: затирает историю «куда ушёл материал»; противоречит принципу IV (Traceability).

---

## R-004 — Структура таблицы `order_material_requirement`

**Decision**: Новая таблица:

```sql
CREATE TABLE order_material_requirement (
    id           UUID PRIMARY KEY,
    order_id     UUID NOT NULL REFERENCES customer_order (id),
    material_id  UUID NOT NULL REFERENCES material (id),
    quantity     NUMERIC(19, 4) NOT NULL CHECK (quantity > 0),
    comment      VARCHAR(500),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_omr_order_material UNIQUE (order_id, material_id)
);

CREATE INDEX ix_omr_order_id ON order_material_requirement (order_id);
CREATE INDEX ix_omr_material_id ON order_material_requirement (material_id);
```

**Rationale**:
- `NUMERIC(19, 4)` совпадает с `StockMovementEntity.quantity` (JPA `precision = 19, scale = 4`). Spec 011 в схеме использует `NUMERIC(15, 4)`, но JPA-аннотация говорит 19,4 — выравниваем на JPA-маппинг, чтобы не плодить расхождений.
- `UNIQUE (order_id, material_id)` обеспечивает FR-002.
- Без `ON DELETE CASCADE` на `customer_order` — заказы не удаляются hard, только статусом. Если в будущем добавится hard-delete, явная политика обсуждается отдельно.
- Без FK-каскада на `material(id)` — материал нельзя удалить, если есть BOM-строка активного заказа (FR из edge case + Q3 clarification).

**Alternatives considered**:
- `precision = 15` (как в schema 011 для `stock_movement`) — отвергнуто: расхождение с JPA-аннотацией текущей реализации. Лучше выровнять на 19,4 во всём модуле; если 011 имел 15,4 — это работает только потому что 15 < 19 и значения помещаются.
- Хранить требуемое кол-во в виде JSON-массива на `customer_order_item` — отвергнуто: денормализация, теряется FK на `material`, невозможно просто посчитать «во всех BOM, где этот материал».
- Soft-delete (`deleted_at`) — отвергнуто: BOM-строки удаляются физически только если по ним нет списаний; иначе блокируются. Состояние «удалена, но списания есть» не нужно.

---

## R-005 — Стратегия конкурентных списаний

**Decision**: В `ConsumeStockUseCase` транзакция оформляется как:

```kotlin
@Transactional
fun consume(command: ConsumeStockCommand): InventoryMutationResult<StockMovementView> {
    // 1. Permission check
    // 2. Lookup material with PESSIMISTIC_WRITE lock (SELECT ... FOR UPDATE)
    // 3. Lookup order via OrderLookupPort, validate !shipped
    // 4. Validate BOM line exists for (order, material)
    // 5. Recompute current stock = SUM(receipts) - SUM(consumptions) WHERE material_id = ?
    // 6. If quantity > stock → return InsufficientStock(available)
    // 7. Save StockMovement (CONSUMPTION, orderId, quantity)
    // 8. Audit STOCK_CONSUMPTION
}
```

`PESSIMISTIC_WRITE` lock на строке `material` сериализует все одновременные списания одного материала. Lock автоматически снимается в конце транзакции. Под `READ COMMITTED` (default Postgres) этого достаточно.

**Rationale**:
- Простота: одна изменяемая сущность (`material` row) → один lock → нет deadlock-сценариев.
- Корректно при default-isolation — не требует переключать на SERIALIZABLE (что снизило бы пропускную способность во всём бэкенде).
- Соответствует AS US2#2 («кто пришёл вторым — получает ошибку «Недостаточный остаток»»).

**Alternatives considered**:
- `SERIALIZABLE` isolation — отвергнуто: излишне для одной операции, требует ретраи на стороне приложения.
- Optimistic locking с `@Version` на `material` — отвергнуто: `material` в этой спеке не мутируется при списании; версия не отражает реальные изменения остатка.
- Atomic UPDATE с CHECK на остаток — отвергнуто: остаток вычисляемый, не хранится; нет столбца, в который можно встроить CHECK.

---

## R-006 — RBAC-имплементация

**Decision**: Расширяем `inventory/application/InventoryPermissions.kt`:

```kotlin
const val WAREHOUSE_ROLE_CODE = "WAREHOUSE"          // existing
const val ORDER_MANAGER_ROLE_CODE = "ORDER_MANAGER"  // переэкспортируется из orders.application
const val PRODUCTION_SUPERVISOR_ROLE_CODE = "PRODUCTION_SUPERVISOR"

fun canManageInventory(roles: Set<String>): Boolean      // existing — для приходов и CRUD материалов
fun canEditBom(roles: Set<String>): Boolean              // ADMIN ∪ ORDER_MANAGER
fun canConsumeStock(roles: Set<String>): Boolean         // ADMIN ∪ WAREHOUSE
fun canViewBomAndUsage(roles: Set<String>): Boolean      // ADMIN ∪ ORDER_MANAGER ∪ WAREHOUSE ∪ PRODUCTION_SUPERVISOR
```

Frontend-router и `usePermissions()` не меняются для существующих gates; добавляются новые computed-флаги:
- `canEditOrderBom = isAdmin || isOrderManager`
- `canConsumeStock = isAdmin || isWarehouse`
- `canViewOrderBom = isAdmin || isOrderManager || isWarehouse || isProductionSupervisor`

**Rationale**:
- Не нарушаем спецификацию (Q1 clarification: PRODUCTION_SUPERVISOR — view yes, EXECUTOR — no).
- Не вводим новых role codes — пере-используем существующие константы из `auth.domain` и `orders.application`.
- Frontend в роутере уже использует mix из display names и role codes (см. `router/index.ts:99`), наследуем тот же стиль для новых вью.

**Alternatives considered**:
- Дать EXECUTOR view-доступ для удобства — отвергнуто пользователем (clarification Q1, Option B).
- Отдельная роль `BOM_EDITOR` — отвергнуто: преждевременная фрагментация ролевой модели.

---

## R-007 — Audit-метаданные для BOM_LINE_UPDATED

**Decision**: Существующая таблица `inventory_audit_event` имеет поле `metadata TEXT`. Для `BOM_LINE_UPDATED` пишем JSON:

```json
{
  "orderId": "<uuid>",
  "materialId": "<uuid>",
  "materialName": "Фанера",
  "before": { "quantity": "10.0000", "comment": null },
  "after":  { "quantity": "12.0000", "comment": "На основу" }
}
```

Для `BOM_LINE_ADDED` и `BOM_LINE_REMOVED` — только текущее состояние. Для `STOCK_CONSUMPTION` — `{orderId, orderNumber, materialId, materialName, quantity}`. Поле `summary` (VARCHAR 500) дублирует ключевую информацию в человекочитаемом виде, как уже сделано в `ReceiveStockUseCase`.

**Rationale**: Единый формат с уже существующим `STOCK_RECEIPT`. Не требует миграции схемы. JSON в TEXT удобен для будущего разбора без принудительной строгой схемы (audit — append-only, разные события могут иметь разные поля).

**Alternatives considered**:
- Структурированная колонка `metadata JSONB` — отвергнуто: сейчас `metadata` это TEXT по всем модулям (`order_audit_event`, `production_task_audit_event`); делаем как у соседей.
- Отдельная таблица `bom_change_diff` — отвергнуто: дублирует функциональность audit-event; источник правды один.

---

## R-008 — Endpoint layout и владение

**Decision**: BOM endpoints живут под путём `/api/orders/{orderId}/bom`, но контроллер `BomController` принадлежит модулю `inventory`. Аналогично для `/api/orders/{orderId}/material-usage`. Списание остаётся под путём материала: `POST /api/materials/{id}/consume`.

**Rationale**:
- Префикс `/api/orders/{orderId}` отражает, что BOM — собственность заказа (URL-моделирование). Но бизнес-логика BOM — про инвентарь, поэтому контроллер живёт в `inventory/adapter/web/`. Это уже сложившийся паттерн в проекте: `production`-модуль содержит `ProductionTaskController` с путём `/api/orders/{id}/production-tasks`.
- Списание — операция склада, поэтому путь от материала. `orderId` идёт в теле запроса. Это согласуется с тем, что список заказов кладовщик может видеть/выбирать на стороне клиента.

**Alternatives considered**:
- Все эндпоинты под `/api/inventory/...` — отвергнуто: ломает RESTful моделирование, скрывает связь с заказом в URL.
- Списание под `/api/orders/{orderId}/consumptions` — рассматривалось как симметричный вариант, отвергнуто из-за UX: основной идентификатор операции — материал (кладовщик выбирает что списать), заказ — атрибут.

---

## R-009 — UX-вход списания: основной vs альтернативный

**Decision**: Два UX-входа, общий компонент `StockConsumeDialog`:
1. **Из карточки заказа (US2 основной)**: кнопка «Списать» в секции `MaterialUsageSection`. Заказ преселектирован (передаётся как prop), материал выбирается из BOM этого заказа.
2. **Из `WarehouseListPage` (US2 альтернативный, FR-023)**: новая кнопка «Списать» в шапке. Открывает диалог с дополнительным шагом — пикер активного заказа (autocomplete по orderNumber + customerName), затем выбор материала из BOM выбранного заказа.

Компонент принимает опциональный prop `preselectedOrder?: InventoryOrderSummary`. Если передан — пикер скрыт; если нет — рендерится autocomplete на базе `OrderLookupPort.searchActiveOrdersForConsumption`.

**Rationale**: Один компонент, один тест-сценарий, два места вызова. Соответствует уже сложившейся практике (см. `MaterialCreateDialog.vue`, `StockReceiptDialog.vue`).

**Alternatives considered**:
- Два независимых диалога — отвергнуто: дублирование валидации остатка, двойной maintenance.
- Только вход из карточки заказа — отвергнуто: бриф и FR-023 явно требуют альтернативный вход для кладовщика.

---

## R-010 — Frontend testing strategy

**Decision**: Сохраняется source-text TDD стиль: новые компоненты (`BomSection.vue`, `StockConsumeDialog.vue`) тестируются через `readFileSync` + строковые ассерты на ключевые контрактные элементы (наличие i18n-ключей, классов, prop-имён). Логика composables (`use-order-bom`, `use-stock-consumption`) — через стандартный mount/api-mock.

**Rationale**: Соответствует существующей политике (`tests/setup.ts`, `WarehouseListPage.test.ts`, `MaterialCreateDialog.test.ts`). Не требуется E2E.

**Alternatives considered**: Playwright E2E — out of scope для этой спеки; ручная quickstart-проверка покрывает.

---

## Open items (deferred to implementation)

- Точные тексты i18n-ключей (`bom.section.title`, `bom.line.add`, и т.д.) выбираются в задачах фронта.
- Дизайн перерасход-индикатора (бейдж/иконка/цвет) — UI-deтail, реализуется по существующим Tailwind-токенам без отдельного research.
- Конкретные регекспы для autocomplete по orderNumber (минимум символов до запроса) — реализация на стороне `useOrderPicker` composable.
