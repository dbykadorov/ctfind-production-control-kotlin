# Cross-module Contract: OrderLookupPort

Внутренний Kotlin-контракт между модулем `inventory` и модулем `orders`. Исключает прямой импорт `orders.adapter.persistence.*` из `inventory`. Соответствует существующему паттерну `ProductionOrderSourcePort` (см. `production/application/ProductionTaskPorts.kt:30-33`).

## Location

- **Декларация**: `inventory/application/InventoryPorts.kt`
- **Реализация**: `inventory/adapter/persistence/OrderLookupAdapter.kt` (`@Component`), читает `CustomerOrderJpaRepository` и `CustomerJpaRepository` (импорт допустим — это adapter, не application).

## Domain DTO

```kotlin
package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.orders.domain.OrderStatus
import java.util.UUID

data class InventoryOrderSummary(
    val id: UUID,
    val orderNumber: String,
    val customerName: String,
    val status: OrderStatus,
) {
    val shipped: Boolean get() = status == OrderStatus.SHIPPED
}

data class ActiveOrderSearchQuery(
    val search: String? = null,    // подстрока по orderNumber или customerName, case-insensitive
    val limit: Int = 20,
)
```

`OrderStatus` импортируется из `orders.domain` — это допустимо: domain enum не несёт инфраструктурных зависимостей.

## Port

```kotlin
package com.ctfind.productioncontrol.inventory.application

import java.util.UUID

interface OrderLookupPort {
    /** Краткое представление заказа для валидации и UI. Null, если заказ не существует. */
    fun findOrderSummary(orderId: UUID): InventoryOrderSummary?

    /**
     * Поиск **активных** заказов (status != SHIPPED) для UX-выбора при списании из склада (US2 alt-entry).
     * Возвращает только заказы, имеющие хотя бы одну BOM-строку (см. FR-024).
     * Сортировка — по `created_at DESC`.
     */
    fun searchActiveOrdersForConsumption(query: ActiveOrderSearchQuery): List<InventoryOrderSummary>
}
```

## Implementation contract

`OrderLookupAdapter`:

```kotlin
@Component
class OrderLookupAdapter(
    private val customerOrderRepo: CustomerOrderJpaRepository,
    private val customerRepo: CustomerJpaRepository,
    private val omrRepo: OrderMaterialRequirementJpaRepository,
) : OrderLookupPort {

    override fun findOrderSummary(orderId: UUID): InventoryOrderSummary? = ...

    override fun searchActiveOrdersForConsumption(query: ActiveOrderSearchQuery): List<InventoryOrderSummary> {
        // SELECT co.*, c.name FROM customer_order co
        //   JOIN customer c ON c.id = co.customer_id
        //   WHERE co.status != 'SHIPPED'
        //     AND EXISTS (SELECT 1 FROM order_material_requirement omr WHERE omr.order_id = co.id)
        //     AND (:search IS NULL OR co.order_number ILIKE %s OR c.name ILIKE %s)
        //   ORDER BY co.created_at DESC LIMIT :limit
        ...
    }
}
```

Запрос реализуется как `@Query` на новом репозитории-адаптере или через `EntityManager.createQuery`. Имеет смысл вынести в `OrderMaterialRequirementJpaRepository.findActiveOrdersWithBom(...)` — тогда LEFT JOIN не нужен.

## Consumers

| Use case / Controller             | Использует метод                          | Зачем                                                |
|-----------------------------------|-------------------------------------------|------------------------------------------------------|
| `AddBomLineUseCase`               | `findOrderSummary`                        | Проверить `!order.shipped`                           |
| `UpdateBomLineUseCase`            | `findOrderSummary`                        | Проверить `!order.shipped`                           |
| `RemoveBomLineUseCase`            | `findOrderSummary`                        | Проверить `!order.shipped`                           |
| `ConsumeStockUseCase`             | `findOrderSummary`                        | Проверить существование и `!order.shipped`           |
| `BomController.list`              | `findOrderSummary`                        | 404 если заказа нет; обогатить response orderNumber  |
| `MaterialUsageController.get`     | `findOrderSummary`                        | 404 если заказа нет                                  |
| `BomController` / DTO             | `findOrderSummary`                        | Денормализация orderNumber в audit-summary           |
| `StockConsumeDialog` (frontend)   | `GET /api/orders/active-for-consumption?` | Picker (см. ниже)                                    |

## REST endpoint для frontend

Для US2 alt-entry (выбор заказа из склада) добавляется тонкий REST-фасад над `searchActiveOrdersForConsumption`:

### `GET /api/orders/active-for-consumption`

**Roles**: `ADMIN ∪ WAREHOUSE` (только те, кто может списывать).

**Query params**:
- `search` — опциональная подстрока (≥ 2 символа на стороне UI)
- `limit` — default 20, max 100

**Response 200**:

```json
{
  "items": [
    {
      "id": "...",
      "orderNumber": "ORD-000123",
      "customerName": "ООО «Заказчик»",
      "status": "IN_WORK"
    }
  ]
}
```

Контроллер этого эндпоинта живёт в `inventory/adapter/web/` (логически — операция склада, фильтр «куда можно списывать»). Не путать с `/api/orders` из orders-модуля, который возвращает полные данные заказов и предназначен для других страниц кабинета.

## Testing

- `OrderLookupAdapterTest` (Spring + Postgres-Testcontainers): создать заказ NEW + BOM-строку + заказ SHIPPED + заказ без BOM → `searchActiveOrdersForConsumption()` возвращает только NEW с BOM.
- Use case-тесты подменяют `OrderLookupPort` через MockK (см. dual-constructor pattern в `ReceiveStockUseCase`).
