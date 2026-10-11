# 008 · Order list pagination

## Context
`GET /api/orders` returned every order of the customer in one response. That is fine for a demo and
wrong for a customer with thousands of orders: the response, the query and the page all grow without
limit. This spec makes the list paginated, in the API and in the interface.

## Rules
- R1. `GET /api/orders` accepts `page` (zero-based, default `0`) and `size` (default `10`, maximum `50`).
  A value out of range is rejected with `400` and the code `VALIDATION_FAILED`.
- R2. The response is an object, not an array: `{ "content": [...], "page": 0, "size": 10, "hasNext": true }`.
  There is no total: counting every row on each request is expensive and the interface does not show it.
- R3. The order is fixed: newest first (`createdAt` descending, `id` as the tie-breaker so the order is
  stable). The client cannot choose it.
- R4. A customer still sees only their own orders.
- R5. A page costs two queries whatever its size: one for the orders (with `LIMIT`) and one for all
  their lines. The lines are not fetched with a join, because a join on a collection multiplies the
  rows and forces Hibernate to paginate in memory.
- R6. The application layer does not use Spring Data types. The ports receive a `PageQuery` and return
  a `PageResult`; the persistence adapter translates them to `PageRequest` and `Slice`.
- R7. The interface shows the first page and a "Load more" button while `hasNext` is true. The
  automatic refresh of `PENDING` orders reloads the pages that are already on screen.

## Acceptance criteria
- [x] With 12 orders and `size=5`: page 0 has the 5 newest and `hasNext` is true; page 2 has 2 and
  `hasNext` is false.
- [x] Without parameters the first 10 orders are returned.
- [x] `size=51` and `page=-1` respond `400` with the code `VALIDATION_FAILED` and name the parameter.
- [x] Another customer's orders never appear in the page.
- [x] Listing a page of several orders runs two SQL queries.
- [x] The application layer does not depend on `org.springframework.data` (ArchUnit).
- [x] In the interface, "Load more" appends the next page and disappears on the last one.

## Out of scope
- Keyset pagination: with `OFFSET`, an order created between two requests shifts the following pages by
  one. The interface removes the duplicates; a far page is still slow on a very large table.
- Total count, filters and client-selected sorting.
- Paginating the catalog (`GET /api/products`): it is small and the cart needs all of it.
