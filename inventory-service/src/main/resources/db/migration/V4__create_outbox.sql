-- Transactional outbox (specs/005): the reservation result is stored in the same
-- transaction as the stock deduction and a relay publishes it to Kafka afterwards.
CREATE TABLE outbox (
    id           BIGSERIAL PRIMARY KEY,
    topic        VARCHAR(120) NOT NULL,
    message_key  VARCHAR(64)  NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);

-- Partial index: the relay only queries the pending rows, which are few.
CREATE INDEX idx_outbox_pending ON outbox (id) WHERE published_at IS NULL;
