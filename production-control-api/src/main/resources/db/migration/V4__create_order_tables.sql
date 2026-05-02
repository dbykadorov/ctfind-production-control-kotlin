CREATE SEQUENCE customer_order_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE customer (
    id UUID PRIMARY KEY,
    display_name VARCHAR(240) NOT NULL,
    status VARCHAR(20) NOT NULL,
    contact_person VARCHAR(200),
    phone VARCHAR(80),
    email VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_customer_display_name ON customer (display_name);
CREATE INDEX ix_customer_status ON customer (status);

CREATE TABLE customer_order (
    id UUID PRIMARY KEY,
    order_number VARCHAR(40) NOT NULL,
    customer_id UUID NOT NULL,
    delivery_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes TEXT,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_customer_order_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT fk_customer_order_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE UNIQUE INDEX uk_customer_order_order_number ON customer_order (order_number);
CREATE INDEX ix_customer_order_customer ON customer_order (customer_id);
CREATE INDEX ix_customer_order_status ON customer_order (status);
CREATE INDEX ix_customer_order_delivery_date ON customer_order (delivery_date);
CREATE INDEX ix_customer_order_updated_at ON customer_order (updated_at);

CREATE TABLE customer_order_item (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    line_no INTEGER NOT NULL,
    item_name VARCHAR(240) NOT NULL,
    quantity NUMERIC(14, 3) NOT NULL,
    uom VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_customer_order_item_order FOREIGN KEY (order_id) REFERENCES customer_order (id) ON DELETE CASCADE,
    CONSTRAINT ck_customer_order_item_line_no CHECK (line_no > 0),
    CONSTRAINT ck_customer_order_item_quantity CHECK (quantity > 0)
);

CREATE UNIQUE INDEX uk_customer_order_item_line ON customer_order_item (order_id, line_no);

CREATE TABLE order_status_change (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    actor_user_id UUID NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL,
    note TEXT,
    CONSTRAINT fk_order_status_change_order FOREIGN KEY (order_id) REFERENCES customer_order (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_status_change_actor FOREIGN KEY (actor_user_id) REFERENCES app_user (id)
);

CREATE INDEX ix_order_status_change_order_changed ON order_status_change (order_id, changed_at);

CREATE TABLE order_change_diff (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    actor_user_id UUID NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL,
    change_type VARCHAR(40) NOT NULL,
    field_diffs TEXT NOT NULL,
    before_snapshot TEXT,
    after_snapshot TEXT,
    CONSTRAINT fk_order_change_diff_order FOREIGN KEY (order_id) REFERENCES customer_order (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_change_diff_actor FOREIGN KEY (actor_user_id) REFERENCES app_user (id)
);

CREATE INDEX ix_order_change_diff_order_changed ON order_change_diff (order_id, changed_at);

CREATE TABLE order_audit_event (
    id UUID PRIMARY KEY,
    event_type VARCHAR(60) NOT NULL,
    actor_user_id UUID NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id UUID NOT NULL,
    event_at TIMESTAMPTZ NOT NULL,
    summary TEXT NOT NULL,
    metadata TEXT,
    CONSTRAINT fk_order_audit_event_actor FOREIGN KEY (actor_user_id) REFERENCES app_user (id)
);

CREATE INDEX ix_order_audit_event_target ON order_audit_event (target_type, target_id);
CREATE INDEX ix_order_audit_event_event_at ON order_audit_event (event_at);
