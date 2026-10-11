-- Stock release on cancellation (specs/004).
-- The reservation goes from a boolean to a status and keeps its lines so they can be returned.
ALTER TABLE stock_reservations ADD COLUMN status VARCHAR(20);
UPDATE stock_reservations SET status = CASE WHEN reserved THEN 'RESERVED' ELSE 'REJECTED' END;
ALTER TABLE stock_reservations ALTER COLUMN status SET NOT NULL;
ALTER TABLE stock_reservations DROP COLUMN reserved;
ALTER TABLE stock_reservations ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Reservations made before this migration have no stored lines: they cannot be released.
CREATE TABLE stock_reservation_items (
    order_id UUID        NOT NULL REFERENCES stock_reservations (order_id) ON DELETE CASCADE,
    sku      VARCHAR(64) NOT NULL REFERENCES products (sku),
    quantity INT         NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (order_id, sku)
);
