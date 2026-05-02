CREATE TABLE order_material_requirement (
    id           UUID PRIMARY KEY,
    order_id     UUID                      NOT NULL REFERENCES customer_order (id),
    material_id  UUID                      NOT NULL REFERENCES material (id),
    quantity     NUMERIC(19, 4)            NOT NULL CHECK (quantity > 0),
    comment      VARCHAR(500),
    created_at   TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_omr_order_material UNIQUE (order_id, material_id)
);

CREATE INDEX ix_omr_order_id ON order_material_requirement (order_id);
CREATE INDEX ix_omr_material_id ON order_material_requirement (material_id);

ALTER TABLE stock_movement
    ADD COLUMN order_id UUID REFERENCES customer_order (id);

ALTER TABLE stock_movement
    ADD CONSTRAINT chk_movement_order_consistency
    CHECK (
        (movement_type = 'RECEIPT' AND order_id IS NULL)
            OR
            (movement_type = 'CONSUMPTION' AND order_id IS NOT NULL)
    );

CREATE INDEX idx_stock_movement_order_id ON stock_movement (order_id);
