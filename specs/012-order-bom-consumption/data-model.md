# Data Model: Списание материалов под заказ + BOM

Phase 1 — модель данных и доменных сущностей. Базируется на решениях из [research.md](./research.md).

## ER overview

```text
                       customer_order ── 1 ────────┐
                            │                       │
                            │ 1                     │ 0..1 (только для CONSUMPTION)
                            │                       │
                            ▼ N                     │
              order_material_requirement            │
                  (BOM line)                        │
                            │                       │
                            │ N                     │
                            ▼                       │
                          material ────── 1 ─── N ──┴── stock_movement
                                                       (RECEIPT | CONSUMPTION)
                                                            │
                                                            │
                          inventory_audit_event  (append-only журнал событий)
```

Таблицы `customer_order`, `material`, `stock_movement`, `inventory_audit_event` уже существуют. Новая таблица — `order_material_requirement`. К `stock_movement` добавляется колонка `order_id`.

---

## 1. OrderMaterialRequirement (NEW)

Спецификация материала для конкретного заказа. Одна строка = «материал X в количестве Y нужен для заказа Z».

### Domain (Kotlin)

```kotlin
package com.ctfind.productioncontrol.inventory.domain

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class OrderMaterialRequirement(
    val id: UUID = UUID.randomUUID(),
    val orderId: UUID,
    val materialId: UUID,
    val quantity: BigDecimal,
    val comment: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(quantity > BigDecimal.ZERO) { "BOM line quantity must be greater than zero" }
        require(comment == null || comment.length <= 500) { "comment must be 500 chars or less" }
    }
}
```

### Persistence (`order_material_requirement` table)

| Колонка       | Тип                       | Constraints                                                          |
|---------------|---------------------------|----------------------------------------------------------------------|
| `id`          | UUID                      | PRIMARY KEY                                                          |
| `order_id`    | UUID                      | NOT NULL, FK → `customer_order(id)` (no cascade)                     |
| `material_id` | UUID                      | NOT NULL, FK → `material(id)` (no cascade)                           |
| `quantity`    | NUMERIC(19, 4)            | NOT NULL, CHECK (quantity > 0)                                       |
| `comment`     | VARCHAR(500)              | NULLABLE                                                             |
| `created_at`  | TIMESTAMP WITH TIME ZONE  | NOT NULL DEFAULT NOW()                                               |
| `updated_at`  | TIMESTAMP WITH TIME ZONE  | NOT NULL DEFAULT NOW()                                               |

Constraints:
- `UNIQUE (order_id, material_id)` — реализует FR-002.
- Индексы: `(order_id)`, `(material_id)`.

### Lifecycle и инварианты

- **Создание** разрешено только для активных заказов (`order.status != SHIPPED`) — проверка в `AddBomLineUseCase`.
- **Редактирование** только полей `quantity` и `comment`; `material_id` менять нельзя — реализуется на уровне use case (старая строка остаётся, чтобы изменить материал — удалить и добавить).
- **Удаление** разрешено только если по этой паре `(order_id, material_id)` нет ни одной записи `stock_movement` с типом `CONSUMPTION` (FR-004).
- При попытке создать дубль (тот же `material_id` для того же `order_id`) — нарушение `UNIQUE`, обрабатывается use case как `Conflict`.
- `updated_at` обновляется на каждое изменение `quantity` или `comment`.

---

## 2. StockMovement (расширение существующей сущности)

### Изменения

Domain — `inventory/domain/StockMovement.kt`:

```kotlin
enum class MovementType {
    RECEIPT,
    CONSUMPTION,   // NEW
}

data class StockMovement(
    val id: UUID = UUID.randomUUID(),
    val materialId: UUID,
    val movementType: MovementType,
    val quantity: BigDecimal,
    val comment: String?,
    val orderId: UUID?,                  // NEW: nullable для RECEIPT, обязательный для CONSUMPTION
    val actorUserId: UUID,
    val actorDisplayName: String,
    val createdAt: Instant,
) {
    init {
        require(quantity > BigDecimal.ZERO) { "quantity must be greater than zero" }
        when (movementType) {
            MovementType.RECEIPT -> require(orderId == null) { "RECEIPT must not reference an order" }
            MovementType.CONSUMPTION -> requireNotNull(orderId) { "CONSUMPTION must reference an order" }
        }
    }
}
```

### Persistence — изменения в `stock_movement`

Добавляется колонка:

| Колонка     | Тип    | Constraints                                                 |
|-------------|--------|-------------------------------------------------------------|
| `order_id`  | UUID   | NULLABLE, FK → `customer_order(id)` (no cascade, no SET NULL) |

CHECK на уровне БД (опционально, но желательно):

```sql
ALTER TABLE stock_movement
  ADD CONSTRAINT chk_movement_order_consistency
  CHECK (
    (movement_type = 'RECEIPT' AND order_id IS NULL) OR
    (movement_type = 'CONSUMPTION' AND order_id IS NOT NULL)
  );
```

Индекс: `idx_stock_movement_order_id` для агрегата FR-015.

### Лежащие в основе вычислимые величины

- **Текущий остаток материала** = `Σ quantity WHERE material_id = ? AND movement_type = 'RECEIPT'` − `Σ quantity WHERE material_id = ? AND movement_type = 'CONSUMPTION'`. Вычисляется через метод `StockMovementPort.computeCurrentStock(materialId)` (заменяет/обобщает существующий `sumQuantityByMaterialId`).
- **Списано по строке BOM** = `Σ quantity WHERE material_id = ? AND order_id = ? AND movement_type = 'CONSUMPTION'`.

---

## 3. Material (без изменений по схеме, расширение правила удаления)

Сущность `Material` и таблица `material` не меняются. Меняется правило удаления:

```kotlin
fun canDeleteMaterial(
    materialId: UUID,
    hasMovements: (UUID) -> Boolean,
    hasBomLineInActiveOrder: (UUID) -> Boolean,
): Boolean = !hasMovements(materialId) && !hasBomLineInActiveOrder(materialId)
```

Где **активный заказ** = `order.status != SHIPPED` (clarification Q3, R-001). Конкретно: материал нельзя удалить, если в `order_material_requirement` есть запись, ссылающаяся на материал, для заказа со статусом, отличным от `SHIPPED`. Если все BOM-строки этого материала принадлежат заказам в `SHIPPED` и нет ни одного движения — удаление возможно.

Правило реализуется в `DeleteMaterialUseCase`. На уровне БД триггера/CHECK не нужно — use case-проверка достаточна (single-writer для команд CRUD).

---

## 4. InventoryAuditEvent (без изменений по схеме, новые типы событий)

Существующая таблица `inventory_audit_event` (V8) используется как есть. Новые значения `event_type`:

| event_type            | summary template                                                                    | metadata (JSON)                                                                    |
|-----------------------|-------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| `BOM_LINE_ADDED`      | `Добавлен материал «{name}» {qty} {unit} в заказ #{orderNumber}`                    | `{orderId, orderNumber, materialId, materialName, quantity, comment}`              |
| `BOM_LINE_UPDATED`    | `Изменена строка BOM «{name}» в заказе #{orderNumber}: qty {before} → {after}`     | `{orderId, materialId, before:{quantity,comment}, after:{quantity,comment}}`       |
| `BOM_LINE_REMOVED`    | `Удалена строка BOM «{name}» в заказе #{orderNumber}`                              | `{orderId, materialId, materialName, removedQuantity}`                             |
| `STOCK_CONSUMPTION`   | `Списание {qty} {unit} материала «{name}» на заказ #{orderNumber}{comment?}`        | `{orderId, orderNumber, materialId, materialName, quantity, comment}`              |

Существующий `STOCK_RECEIPT` и формат для CRUD материалов — без изменений.

`target_id` в `inventory_audit_event` для BOM-событий — `materialId` (consistent с тем, как `STOCK_RECEIPT` использует `materialId`); `orderId` — в metadata. Это требует адаптации `fetchInventoryEvents` в `AuditPersistenceAdapter` (который уже хардкодит `targetType = "MATERIAL"`).

---

## 5. Read models (вычисляемые, не сущности)

### BomLineView

```kotlin
data class BomLineView(
    val id: UUID,
    val orderId: UUID,
    val materialId: UUID,
    val materialName: String,
    val materialUnit: MeasurementUnit,
    val quantity: BigDecimal,
    val comment: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

Возвращается списком `GET /api/orders/{orderId}/bom`.

### MaterialUsageRowView

```kotlin
data class MaterialUsageRowView(
    val materialId: UUID,
    val materialName: String,
    val materialUnit: MeasurementUnit,
    val requiredQuantity: BigDecimal,        // из BOM
    val consumedQuantity: BigDecimal,        // Σ списаний по (order, material)
    val remainingToConsume: BigDecimal,      // max(0, required - consumed)
    val overconsumption: BigDecimal,         // max(0, consumed - required), 0 если расхода нет
)

data class MaterialUsageView(
    val orderId: UUID,
    val rows: List<MaterialUsageRowView>,
)
```

Возвращается из `GET /api/orders/{orderId}/material-usage`. Считается одним запросом с LEFT JOIN BOM ⨝ агрегат списаний.

### InventoryOrderSummary (cross-module DTO для OrderLookupPort)

```kotlin
data class InventoryOrderSummary(
    val id: UUID,
    val orderNumber: String,
    val customerName: String,
    val status: OrderStatus,
)

val InventoryOrderSummary.shipped: Boolean get() = status == OrderStatus.SHIPPED
```

Используется и в `BomController` (для вывода имени заказа), и в `StockConsumeDialog` (для autocomplete).

---

## 6. Validation rules summary

| Правило                                                              | Уровень          | Реализуется в                                            |
|----------------------------------------------------------------------|------------------|----------------------------------------------------------|
| BOM quantity > 0                                                     | Domain init + DB | `OrderMaterialRequirement.init`, `CHECK (quantity > 0)`  |
| BOM unique (order, material)                                         | DB               | `UNIQUE (order_id, material_id)`                         |
| BOM material exists                                                  | Use case         | `AddBomLineUseCase`                                      |
| BOM order exists и `!shipped`                                        | Use case         | `AddBomLineUseCase`, `UpdateBomLineUseCase`, `RemoveBomLineUseCase` |
| BOM-строку нельзя удалить, если есть consumption для (order,material) | Use case         | `RemoveBomLineUseCase` через `StockMovementPort.hasConsumption(orderId, materialId)` |
| Consumption: материал должен быть в BOM заказа                        | Use case         | `ConsumeStockUseCase`                                    |
| Consumption quantity > 0                                              | Domain init + DB | `StockMovement.init`, `CHECK (quantity > 0)`             |
| Consumption: order exists и `!shipped`                                | Use case         | `ConsumeStockUseCase`                                    |
| Consumption: quantity ≤ current_stock                                 | Use case (TX)    | `ConsumeStockUseCase` под `PESSIMISTIC_WRITE` (R-005)    |
| Movement consistency (RECEIPT⇒no order, CONSUMPTION⇒order)            | Domain init + DB | `StockMovement.init`, `chk_movement_order_consistency`   |
| Material нельзя удалить при наличии активной BOM-строки                | Use case         | `DeleteMaterialUseCase` (расширение)                     |

---

## 7. Migration `V9__bom_and_consumption.sql` — текстовый план

```sql
-- 1. BOM table
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

CREATE INDEX ix_omr_order_id    ON order_material_requirement (order_id);
CREATE INDEX ix_omr_material_id ON order_material_requirement (material_id);

-- 2. stock_movement.order_id
ALTER TABLE stock_movement
    ADD COLUMN order_id UUID REFERENCES customer_order (id);

ALTER TABLE stock_movement
    ADD CONSTRAINT chk_movement_order_consistency
    CHECK (
        (movement_type = 'RECEIPT' AND order_id IS NULL) OR
        (movement_type = 'CONSUMPTION' AND order_id IS NOT NULL)
    );

CREATE INDEX idx_stock_movement_order_id ON stock_movement (order_id);
```

**Backfill**: не требуется — все существующие записи `stock_movement` имеют `movement_type = 'RECEIPT'` и `order_id` остаётся `NULL`, что удовлетворяет CHECK.
