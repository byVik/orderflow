CREATE TABLE products (
    sku                VARCHAR(64) PRIMARY KEY,
    name               VARCHAR(120)   NOT NULL,
    price              NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    available_quantity INT            NOT NULL CHECK (available_quantity >= 0),
    version            BIGINT         NOT NULL DEFAULT 0
);

CREATE TABLE stock_reservations (
    order_id     UUID PRIMARY KEY,
    reserved     BOOLEAN NOT NULL,
    reason       VARCHAR(255),
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
