CREATE TABLE material (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    unit        VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_material_name UNIQUE (name)
);

CREATE TABLE stock_movement (
    id                 UUID PRIMARY KEY,
    material_id        UUID         NOT NULL REFERENCES material (id),
    movement_type      VARCHAR(20)  NOT NULL,
    quantity           NUMERIC(15, 4) NOT NULL CHECK (quantity > 0),
    comment            VARCHAR(500),
    actor_user_id      UUID         NOT NULL,
    actor_display_name VARCHAR(255) NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_movement_material_id ON stock_movement (material_id);
CREATE INDEX idx_stock_movement_created_at  ON stock_movement (created_at DESC);

CREATE TABLE inventory_audit_event (
    id            UUID PRIMARY KEY,
    event_type    VARCHAR(50)  NOT NULL,
    actor_user_id UUID         NOT NULL,
    target_id     UUID         NOT NULL,
    event_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    summary       VARCHAR(500) NOT NULL,
    metadata      TEXT
);

CREATE INDEX idx_inventory_audit_event_at ON inventory_audit_event (event_at DESC);

INSERT INTO role (id, code, name, created_at)
VALUES ('a0000000-0000-0000-0000-000000000005', 'WAREHOUSE', 'Warehouse', NOW())
ON CONFLICT (code) DO NOTHING;
