# Data Model: Склад — материалы и остатки

**Date**: 2026-04-29 | **Branch**: `011-warehouse-materials`

## 1. Entities

### Material

Справочная сущность — единица складского учёта.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Уникальный идентификатор |
| name | String | NOT NULL, UNIQUE, max 255 | Название материала |
| unit | MeasurementUnit (enum) | NOT NULL | Единица измерения |
| createdAt | Instant | NOT NULL | Дата создания |
| updatedAt | Instant | NOT NULL | Дата последнего изменения |

**Validation rules**:
- `name` must be non-blank, trimmed (leading/trailing whitespace removed)
- `name` must be unique (case-insensitive — "Фанера" = "фанера")
- `unit` must be a valid enum value

**Computed**:
- `currentStock: BigDecimal` — SUM of all StockMovement quantities for this material (not stored in DB)

### StockMovement

Запись движения материала — immutable event log.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Уникальный идентификатор |
| materialId | UUID | NOT NULL, FK → material | Связь с материалом |
| movementType | MovementType (enum) | NOT NULL | Тип операции (RECEIPT) |
| quantity | BigDecimal | NOT NULL, > 0 | Количество |
| comment | String? | max 500 | Комментарий к операции |
| actorUserId | UUID | NOT NULL | Кто оформил |
| actorDisplayName | String | NOT NULL | Имя оформившего (для отображения) |
| createdAt | Instant | NOT NULL | Дата/время операции |

**Validation rules**:
- `quantity` must be > 0
- `movementType` in this spec is always RECEIPT (WRITE_OFF added in spec 012)
- `actorUserId` must reference an existing user

### InventoryAuditEvent

Событие аудита для складских операций.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Уникальный идентификатор |
| eventType | String | NOT NULL | Тип: MATERIAL_CREATED, MATERIAL_UPDATED, MATERIAL_DELETED, STOCK_RECEIPT |
| actorUserId | UUID | NOT NULL | Кто выполнил действие |
| targetId | UUID | NOT NULL | ID материала |
| eventAt | Instant | NOT NULL | Когда произошло |
| summary | String | NOT NULL | Описание на русском |
| metadata | String? | — | Доп. данные (JSON) |

## 2. Enums

### MeasurementUnit

| Code | DB value | Display (RU) |
|------|----------|-------------|
| PIECE | PIECE | шт |
| KILOGRAM | KILOGRAM | кг |
| METER | METER | м |
| LITER | LITER | л |
| SQUARE_METER | SQUARE_METER | м² |
| CUBIC_METER | CUBIC_METER | м³ |

### MovementType

| Code | DB value | Display (RU) |
|------|----------|-------------|
| RECEIPT | RECEIPT | Приход |

> WRITE_OFF будет добавлен в спеке 012.

### AuditCategory (расширение существующего enum)

Добавить значение `INVENTORY` к существующим `AUTH, ORDER, PRODUCTION_TASK`.

## 3. Relationships

```
Material 1 ──── * StockMovement
    │
    └── currentStock = SUM(StockMovement.quantity WHERE materialId = this.id)
```

- Material → StockMovement: one-to-many (material has many movements)
- StockMovement → User: many-to-one via actorUserId (denormalized displayName for read performance)
- Material deletion blocked if StockMovement count > 0

## 4. Database Schema (Flyway V8)

```sql
CREATE TABLE material (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    unit        VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_material_name UNIQUE (name)
);

CREATE TABLE stock_movement (
    id                 UUID PRIMARY KEY,
    material_id        UUID NOT NULL REFERENCES material(id),
    movement_type      VARCHAR(20) NOT NULL,
    quantity           NUMERIC(15, 4) NOT NULL CHECK (quantity > 0),
    comment            VARCHAR(500),
    actor_user_id      UUID NOT NULL,
    actor_display_name VARCHAR(255) NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_movement_material_id ON stock_movement(material_id);
CREATE INDEX idx_stock_movement_created_at ON stock_movement(created_at DESC);

-- Inventory audit events table
CREATE TABLE inventory_audit_event (
    id            UUID PRIMARY KEY,
    event_type    VARCHAR(50) NOT NULL,
    actor_user_id UUID NOT NULL,
    target_id     UUID NOT NULL,
    event_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    summary       VARCHAR(500) NOT NULL,
    metadata      TEXT
);

CREATE INDEX idx_inventory_audit_event_at ON inventory_audit_event(event_at DESC);

-- Seed WAREHOUSE role
INSERT INTO role (id, code, name, created_at)
VALUES ('a0000000-0000-0000-0000-000000000005', 'WAREHOUSE', 'Warehouse', NOW())
ON CONFLICT (code) DO NOTHING;
```

## 5. API Response Types (Frontend)

### MaterialResponse

```typescript
interface MaterialResponse {
  id: string
  name: string
  unit: MeasurementUnit
  currentStock: number
  createdAt: string
  updatedAt: string
}
```

### MaterialsPageResponse

```typescript
interface MaterialsPageResponse {
  items: MaterialResponse[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}
```

### StockMovementResponse

```typescript
interface StockMovementResponse {
  id: string
  materialId: string
  movementType: 'RECEIPT'
  quantity: number
  comment: string | null
  actorDisplayName: string
  createdAt: string
}
```

### StockMovementsPageResponse

```typescript
interface StockMovementsPageResponse {
  items: StockMovementResponse[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}
```
