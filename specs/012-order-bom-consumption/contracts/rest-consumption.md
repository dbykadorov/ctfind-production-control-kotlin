# REST Contract: Stock Consumption + Material Usage

Контракт REST API для операций списания и просмотра расхода материалов. Все эндпоинты — JSON, JWT Bearer-аутентификация.

Контроллеры:
- Списание: `POST /api/materials/{id}/consume` — расширение существующего `InventoryController`.
- Расход: `GET /api/orders/{orderId}/material-usage` — новый `MaterialUsageController` в `inventory/adapter/web/`.

## Common error envelope

См. [rest-bom.md](./rest-bom.md). Дополнительные ошибки для consumption:

| HTTP | error                          | Когда                                                                  |
|------|--------------------------------|------------------------------------------------------------------------|
| 400  | `VALIDATION_FAILED`            | quantity ≤ 0, comment > 500                                            |
| 403  | `FORBIDDEN`                    | RBAC отказ                                                             |
| 404  | `MATERIAL_NOT_FOUND`           | материал не существует                                                 |
| 404  | `ORDER_NOT_FOUND`              | заказ не существует                                                    |
| 409  | `ORDER_LOCKED`                 | заказ в статусе `SHIPPED`                                              |
| 409  | `MATERIAL_NOT_IN_BOM`          | материал отсутствует в BOM указанного заказа                           |
| 409  | `INSUFFICIENT_STOCK`           | запрашиваемое количество > текущий остаток                             |

## Endpoints

### `POST /api/materials/{id}/consume`

Оформить списание материала под заказ.

**Roles**: `ADMIN ∪ WAREHOUSE`.

**Path params**:
- `id` — UUID материала

**Request body**:

```json
{
  "orderId": "22222222-2222-2222-2222-222222222222",
  "quantity": "5.0000",
  "comment": "Выдано в цех"
}
```

`comment` опционален. `quantity` — BigDecimal, до 4 знаков, > 0.

**Response 201**:

```json
{
  "id": "44444444-4444-4444-4444-444444444444",
  "materialId": "33333333-3333-3333-3333-333333333333",
  "materialName": "Фанера 10мм",
  "materialUnit": "SQUARE_METER",
  "movementType": "CONSUMPTION",
  "orderId": "22222222-2222-2222-2222-222222222222",
  "orderNumber": "ORD-000123",
  "quantity": "5.0000",
  "comment": "Выдано в цех",
  "actorDisplayName": "Иван Петров",
  "createdAt": "2026-04-29T18:30:00Z"
}
```

`materialName`, `materialUnit`, `orderNumber` денормализуются на стороне use case для удобства UI; в БД они не хранятся в `stock_movement` (только `material_id`, `order_id`).

**Response 409 `INSUFFICIENT_STOCK`**:

```json
{
  "error": "INSUFFICIENT_STOCK",
  "message": "Недостаточный остаток на складе",
  "available": "3.0000"
}
```

Поле `available` — текущий остаток на момент проверки.

**Response 409 `MATERIAL_NOT_IN_BOM`**:

```json
{
  "error": "MATERIAL_NOT_IN_BOM",
  "message": "Материал не входит в спецификацию заказа"
}
```

**Concurrency**: внутри транзакции сериализуется через `PESSIMISTIC_WRITE` lock на строке `material` (R-005). Параллельные списания одного материала обрабатываются по очереди; второй получает `INSUFFICIENT_STOCK`, если суммарно превышает остаток.

**Audit**: успех → `inventory_audit_event` с `event_type = STOCK_CONSUMPTION` (см. [data-model.md §4](../data-model.md)).

**Note**: ответ НЕ содержит warning о перерасходе по BOM. Перерасход вычисляется на стороне UI / агрегата `material-usage`. Это согласуется с FR-011 (warning, не блокировка) и Q2 clarification (без уведомлений).

---

### `GET /api/orders/{orderId}/material-usage`

Агрегат расхода материалов по заказу.

**Roles**: `ADMIN ∪ ORDER_MANAGER ∪ WAREHOUSE ∪ PRODUCTION_SUPERVISOR`.

**Path params**:
- `orderId` — UUID

**Response 200**:

```json
{
  "orderId": "22222222-2222-2222-2222-222222222222",
  "rows": [
    {
      "materialId": "33333333-3333-3333-3333-333333333333",
      "materialName": "Фанера 10мм",
      "materialUnit": "SQUARE_METER",
      "requiredQuantity": "10.0000",
      "consumedQuantity": "12.0000",
      "remainingToConsume": "0.0000",
      "overconsumption": "2.0000"
    },
    {
      "materialId": "55555555-5555-5555-5555-555555555555",
      "materialName": "Гвозди 50мм",
      "materialUnit": "PIECE",
      "requiredQuantity": "100.0000",
      "consumedQuantity": "30.0000",
      "remainingToConsume": "70.0000",
      "overconsumption": "0.0000"
    }
  ]
}
```

Поведение:
- Если по заказу нет ни одной строки BOM → `rows: []`.
- Список включает **только** материалы из BOM. Если был списан материал, который потом удалили из BOM (теоретически невозможно из-за FR-004, но на всякий случай) — он не попадает в этот ответ; такой кейс — сигнал нарушения инварианта, должен ловиться тестом.
- `remainingToConsume = max(0, requiredQuantity - consumedQuantity)`
- `overconsumption = max(0, consumedQuantity - requiredQuantity)`

Сортировка: по `materialName` ASC.

**Errors**:
- 403 `FORBIDDEN`
- 404 `ORDER_NOT_FOUND`

---

## Existing endpoint: `POST /api/materials/{id}/receipt`

Без изменений по контракту. Внутри `ReceiveStockUseCase` `orderId` остаётся `null`. Уже реализовано в спеке 011.
