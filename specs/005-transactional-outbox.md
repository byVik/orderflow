# 005 · Transactional outbox

**English** · [Español](es/005-outbox-transaccional.md)

## Context
Events were published to Kafka right after the commit. If the service crashed between the commit and
the send, or Kafka was unavailable, the event was lost and the order stayed `PENDING`
forever. Saving to the database and publishing to Kafka are two writes with no common transaction.

## Rules
- R1. The publishing adapters no longer call Kafka: they insert a row into the `outbox` table
  (topic, key, JSON payload) **in the same transaction** as the business change.
- R2. Publishing outside a transaction is a programming error and fails (`MANDATORY`).
- R3. A relay reads the pending rows in order, sends them to Kafka waiting for the broker's
  acknowledgment and marks them as published.
- R4. The relay locks the rows with `FOR UPDATE SKIP LOCKED`: several instances do not send the same row.
- R5. If the send fails, the relay's transaction rolls back and the rows are retried on the
  next pass.
- R6. The guarantee is **at-least-once**: there can be duplicates, and the consumers are already idempotent
  (specs 002 and 004).
- R7. The outbound ports (`OrderEventPublisher`, `StockEventPublisher`) do not change: it is an adapter
  change.

## Acceptance criteria
- [x] When an order is created, `OrderPlaced` reaches Kafka and the outbox row is marked as published.
- [x] When stock is reserved, `StockResult` reaches Kafka through the outbox.
- [x] If the business transaction rolls back, no row is left in the outbox.

## Pending / improvements
- Purge old published rows.
- Replace polling with CDC (Debezium) if latency or volume call for it.
- With several relay instances, the global order across batches is not guaranteed.
