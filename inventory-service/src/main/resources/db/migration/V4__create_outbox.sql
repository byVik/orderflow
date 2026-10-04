-- Outbox transaccional (specs/005): el resultado de la reserva se guarda en la misma
-- transacción que el descuento de stock y un relay lo publica después en Kafka.
CREATE TABLE outbox (
    id           BIGSERIAL PRIMARY KEY,
    topic        VARCHAR(120) NOT NULL,
    message_key  VARCHAR(64)  NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);

-- Índice parcial: el relay solo consulta las filas pendientes, que son pocas.
CREATE INDEX idx_outbox_pending ON outbox (id) WHERE published_at IS NULL;
