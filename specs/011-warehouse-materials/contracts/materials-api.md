# Contract: Materials REST API

## Endpoints

### GET /api/materials

List materials with optional search and pagination.

**Auth**: WAREHOUSE or ADMIN role required (403 otherwise)

**Query params**:
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| page | int | 0 | Page number (0-indexed) |
| size | int | 20 | Items per page (max 100) |
| search | string? | — | Case-insensitive substring filter on name |

**Response 200**:
```json
{
  "items": [
    {
      "id": "uuid",
      "name": "Фанера берёзовая",
      "unit": "SQUARE_METER",
      "currentStock": 150.5,
      "createdAt": "2026-04-29T10:00:00Z",
      "updatedAt": "2026-04-29T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 42,
  "totalPages": 3
}
```

**Notes**: `currentStock` is computed as SUM of all stock movements for the material. Materials with no movements have `currentStock: 0`.

---

### POST /api/materials

Create a new material.

**Auth**: WAREHOUSE or ADMIN

**Request body**:
```json
{
  "name": "Фанера берёзовая",
  "unit": "SQUARE_METER"
}
```

**Validation**:
- `name`: required, non-blank, max 255, trimmed, unique (case-insensitive)
- `unit`: required, one of: PIECE, KILOGRAM, METER, LITER, SQUARE_METER, CUBIC_METER

**Response 201**: MaterialResponse (with `currentStock: 0`)

**Response 400**: `{ "error": "validation_failed", "message": "...", "field": "name" }`

**Response 409**: `{ "error": "duplicate_name", "message": "Материал с таким названием уже существует" }`

---

### PUT /api/materials/{id}

Update material name and/or unit.

**Auth**: WAREHOUSE or ADMIN

**Request body**:
```json
{
  "name": "Фанера берёзовая 6мм",
  "unit": "SQUARE_METER"
}
```

**Response 200**: MaterialResponse (with current `currentStock`)

**Response 404**: Material not found

**Response 409**: Duplicate name

---

### DELETE /api/materials/{id}

Delete material. Only allowed if no stock movements exist.

**Auth**: WAREHOUSE or ADMIN

**Response 204**: No content

**Response 404**: Material not found

**Response 409**: `{ "error": "has_movements", "message": "Невозможно удалить материал с историей движений" }`

---

### POST /api/materials/{id}/receipt

Register stock receipt for a material.

**Auth**: WAREHOUSE or ADMIN

**Request body**:
```json
{
  "quantity": 100.5,
  "comment": "Поставка от ООО Лес"
}
```

**Validation**:
- `quantity`: required, > 0, BigDecimal
- `comment`: optional, max 500

**Response 201**: StockMovementResponse

**Response 400**: Invalid quantity

**Response 404**: Material not found

---

### GET /api/materials/{id}/movements

List stock movements for a material, reverse chronological.

**Auth**: WAREHOUSE or ADMIN

**Query params**:
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| page | int | 0 | Page number (0-indexed) |
| size | int | 20 | Items per page (max 100) |

**Response 200**:
```json
{
  "items": [
    {
      "id": "uuid",
      "materialId": "uuid",
      "movementType": "RECEIPT",
      "quantity": 100.5,
      "comment": "Поставка от ООО Лес",
      "actorDisplayName": "Иванов И.И.",
      "createdAt": "2026-04-29T14:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 5,
  "totalPages": 1
}
```

**Response 404**: Material not found

---

### GET /api/materials/{id}

Get single material with current stock.

**Auth**: WAREHOUSE or ADMIN

**Response 200**: MaterialResponse

**Response 404**: Material not found

## Error Response Format

All error responses follow the existing project pattern:

```json
{
  "error": "error_code",
  "message": "Human-readable message"
}
```

Error codes: `validation_failed`, `forbidden`, `not_found`, `duplicate_name`, `has_movements`
