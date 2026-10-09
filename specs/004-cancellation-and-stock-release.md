# 004 · Cancellation and stock release

**English** · [Español](es/004-cancelacion-y-liberacion-de-stock.md)

## Context
Until now, canceling a `PENDING` order only changed its status. If `OrderPlaced` was already on its
way, inventory-service reserved the stock anyway and the result was ignored: the stock
stayed deducted for a canceled order. This spec adds the **compensation** of the saga.

## Rules
- R1. When an order is canceled, `OrderCancelled` is published to `orders.order-cancelled.v1` (key = `orderId`).
- R2. inventory-service stores the lines of each reservation so that it can return them.
- R3. On receiving `OrderCancelled`:
  - if the reservation exists and is `RESERVED`, the stock is returned and the reservation moves to `RELEASED`;
  - if it is `REJECTED` or `RELEASED`, nothing is done (idempotency);
  - if it **does not exist**, it is recorded as `REJECTED` with the reason `CANCELLED_BEFORE_RESERVING` (a code since [spec 007](007-internationalization.md)). That way an `OrderPlaced`
    that arrives later reserves nothing.
- R4. There is no ordering guarantee between different topics: `OrderCancelled` can be processed before
  `OrderPlaced`. R3 covers both orderings.
- R5. The release locks the product rows in SKU order, the same as the reservation.
- R6. If canceling and confirming coincide, the first one to commit wins (optimistic locking). The
  cancellation that loses responds `409`, never `500`.

## Acceptance criteria
- [x] Reserving and then canceling returns the stock to its initial value.
- [x] A duplicate `OrderCancelled` does not return the stock twice.
- [x] If `OrderCancelled` arrives before `OrderPlaced`, the stock does not change.
- [x] Canceling an order publishes `OrderCancelled`.
- [x] A concurrency conflict when canceling responds `409`.

## Out of scope
Canceling orders that are already `CONFIRMED` (requires returns) and refunds.
