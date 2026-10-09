# 002 · Stock reservation (choreographed saga)

**English** · [Español](es/002-reserva-stock.md)

## Context
inventory-service listens for `OrderPlaced`, tries to reserve the stock and publishes the result to
`inventory.stock-result.v1`. order-service consumes that result and updates the order.

## Rules
- R1. **All or nothing**: if stock is missing for a single line, nothing is reserved.
- R2. The reservation locks the product rows (`SELECT … FOR UPDATE`) in SKU order to avoid overselling and deadlocks.
- R3. **Idempotency**: if the same `orderId` arrives twice, stock is not deducted again and the original result is resent. The check is done **after** acquiring the locks, and the primary key of the reservation acts as a safety net.
- R4. order-service ignores results for orders that are no longer `PENDING` (duplicates or canceled orders).
- R5. A message that fails after 3 retries is sent to `<topic>-dlt` (dead-letter topic). The `-dlt` topics are declared explicitly and each send leaves an error log.
- R6. The message key is the `orderId`, which preserves the order of the events of a given order.

## Acceptance criteria
- [x] With enough stock, the stock goes down and the order moves to `CONFIRMED`.
- [x] Without enough stock, the stock does not change and the order moves to `REJECTED` with the reason.
- [x] A duplicate `OrderPlaced` deducts stock only once.
- [x] A duplicate result does not change an order that is already confirmed.
- [x] Several simultaneous reservations on the last units do not oversell.

## Resolved in later specs
- Releasing the stock on cancellation: [spec 004](004-cancellation-and-stock-release.md).
- Guaranteeing event publication: [spec 005](005-transactional-outbox.md).
