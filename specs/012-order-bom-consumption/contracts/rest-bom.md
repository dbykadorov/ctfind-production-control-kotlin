# REST Contract: BOM (Order Material Requirements)

Контракт REST API для управления спецификацией материалов заказа. Все эндпоинты — JSON, JWT Bearer-аутентификация.

Базовый префикс: `/api/orders/{orderId}/bom`. Контроллер живёт в `inventory/adapter/web/BomController.kt` (см. R-008).

## Common error envelope

Соответствует существующему формату модуля inventory (`InventoryApiError`):

```json
{
  "error": "VALIDATION_FAILED",
  "message": "Quantity must be greater than zero",
  "field": "quantity"
}
```

| HTTP | error                    | Когда                                                  |
|------|--------------------------|--------------------------------------------------------|
| 400  | `VALIDATION_FAILED`      | quantity ≤ 0, comment > 500 символов и т.п.            |
| 401  | (нет тела)               | JWT отсутствует или невалидный                         |
| 403  | `FORBIDDEN`              | RBAC отказ                                             |
| 404  | `ORDER_NOT_FOUND`        | заказ не существует                                    |
| 404  | `MATERIAL_NOT_FOUND`     | материал не существует                                 |
| 404  | `BOM_LINE_NOT_FOUND`     | BOM-строка не существует                               |
| 409  | `ORDER_LOCKED`           | заказ в статусе `SHIPPED` — изменения BOM запрещены    |
| 409  | `BOM_LINE_DUPLICATE`     | материал уже есть в BOM этого заказа                   |
| 409  | `BOM_LINE_HAS_CONSUMPTION` | попытка удалить строку, по которой были списания     |

## Endpoints

### `GET /api/orders/{orderId}/bom`

Список BOM-строк заказа.

**Roles**: `ADMIN ∪ ORDER_MANAGER ∪ WAREHOUSE ∪ PRODUCTION_SUPERVISOR`.

**Path params**:
- `orderId` — UUID

**Response 200**:

```json
{
  "items": [
    {
      "id": "11111111-1111-1111-1111-111111111111",
      "orderId": "22222222-2222-2222-2222-222222222222",
      "materialId": "33333333-3333-3333-3333-333333333333",
      "materialName": "Фанера 10мм",
      "materialUnit": "SQUARE_METER",
      "quantity": "10.0000",
      "comment": "На основу",
      "createdAt": "2026-04-29T18:00:00Z",
      "updatedAt": "2026-04-29T18:00:00Z"
    }
  ]
}
```

Сортировка: `createdAt DESC` (новые сверху, FR-006).

**Response 404**: `ORDER_NOT_FOUND` если заказа нет.

---

### `POST /api/orders/{orderId}/bom`

Добавить строку BOM.

**Roles**: `ADMIN ∪ ORDER_MANAGER`.

**Request body**:

```json
{
  "materialId": "33333333-3333-3333-3333-333333333333",
  "quantity": "10.0000",
  "comment": "На основу"
}
```

`comment` опционален; `quantity` — строковое представление BigDecimal с до 4 знаков после запятой.

**Response 201**: то же тело, что в `GET items[]` (одна строка).

**Errors**:
- 400 `VALIDATION_FAILED` (`field: "quantity"` если ≤ 0; `field: "comment"` если > 500)
- 403 `FORBIDDEN`
- 404 `ORDER_NOT_FOUND`, `MATERIAL_NOT_FOUND`
- 409 `ORDER_LOCKED` (заказ `SHIPPED`)
- 409 `BOM_LINE_DUPLICATE`

---

### `PUT /api/orders/{orderId}/bom/{lineId}`

Изменить количество и/или комментарий существующей строки. Материал менять нельзя.

**Roles**: `ADMIN ∪ ORDER_MANAGER`.

**Request body**:

```json
{
  "quantity": "12.0000",
  "comment": "На основу + запас"
}
```

`comment` может быть `null` для очистки.

**Response 200**: обновлённая строка.

**Errors**: 400 / 403 / 404 (`BOM_LINE_NOT_FOUND` или `ORDER_NOT_FOUND`) / 409 (`ORDER_LOCKED`).

---

### `DELETE /api/orders/{orderId}/bom/{lineId}`

Удалить строку BOM.

**Roles**: `ADMIN ∪ ORDER_MANAGER`.

**Response 204** (no body).

**Errors**:
- 403 `FORBIDDEN`
- 404 `BOM_LINE_NOT_FOUND` / `ORDER_NOT_FOUND`
- 409 `ORDER_LOCKED`
- 409 `BOM_LINE_HAS_CONSUMPTION` — есть хотя бы одно списание по `(orderId, materialId)`. Тело:

  ```json
  { "error": "BOM_LINE_HAS_CONSUMPTION", "message": "Невозможно удалить материал, по которому уже были списания" }
  ```

---

## Concurrency

BOM-операции — обычные REST CRUD без особых конкурентных гарантий. Дубль-вставка в одну и ту же `(order_id, material_id)` ловится `UNIQUE` constraint и возвращается как 409.

## Audit

Каждая успешная мутация порождает запись в `inventory_audit_event`:
- POST → `BOM_LINE_ADDED`
- PUT  → `BOM_LINE_UPDATED` (с diff в metadata)
- DELETE → `BOM_LINE_REMOVED`

См. таблицу шаблонов в [data-model.md §4](../data-model.md).
