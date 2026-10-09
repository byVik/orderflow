# 001 · Place order

**English** · [Español](es/001-crear-pedido.md)

## Context
An authenticated user creates an order with one or more products from the catalog. The order is not
confirmed instantly: it stays `PENDING` until inventory-service reserves the stock (see spec 002).

## Business rules
- R1. An order has at least one line and does not repeat SKUs.
- R2. The quantity of each line is between 1 and 100. The rule lives in the domain (`OrderLine`); the web DTO repeats it to respond sooner.
- R3. The price is **not** sent by the client: it is taken from the catalog at the moment the order is created.
- R4. If any SKU does not exist in the catalog, the order is rejected with `400`.
- R4b. Prices are requested **before** opening the transaction and with timeouts. If the catalog does not respond, the response is `503` and nothing is saved.
- R5. The order's customer is the `sub` of the JWT; a user can only view and cancel their own orders.
- R6. Only a `PENDING` order can be canceled; otherwise the response is `409`.
- R7. When the order is created, `OrderPlaced` is published to `orders.order-placed.v1` through the outbox (spec 005).
- R8. When it is canceled, `OrderCancelled` is published (spec 004).

## States
`PENDING → CONFIRMED | REJECTED | CANCELLED` (all three are final states).

## API
| Method | Path | Response |
|--------|------|----------|
| POST | `/api/orders` | `201` + `Location` |
| GET | `/api/orders` | the user's orders, from most recent to oldest |
| GET | `/api/orders/{id}` | `404` if it does not exist or belongs to another user |
| POST | `/api/orders/{id}/cancel` | `409` if it is not `PENDING` |

## Acceptance criteria
- [x] Given an authenticated user and existing products, when the user creates an order, they receive `201`, the order is `PENDING` and the total is calculated with the catalog prices.
- [x] Given an order with no lines or with quantity 0, when it is submitted, the user receives `400`.
- [x] Given another user's order, when the user requests it, they receive `404`.
- [x] Given a `CONFIRMED` order, when the user tries to cancel it, they receive `409`.
- [x] Without a token, with an expired token or with one signed with a different key, the order endpoints respond `401`.
- [x] A quantity greater than 100 is also rejected in the domain.
- [x] If the catalog is unavailable, creating an order responds `503`.

## Out of scope
Payments, shipping and editing an order that has already been created.
