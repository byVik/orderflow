CREATE TABLE orders (
    id               UUID PRIMARY KEY,
    customer_id      VARCHAR(64)  NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    rejection_reason VARCHAR(255),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_orders_customer_created ON orders (customer_id, created_at DESC);

CREATE TABLE order_lines (
    id         BIGSERIAL PRIMARY KEY,
    order_id   UUID          NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    sku        VARCHAR(64)   NOT NULL,
    quantity   INT           NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0)
);

CREATE INDEX idx_order_lines_order ON order_lines (order_id);
