# 009 · Resilient catalog client

## Context
Placing an order needs the prices, and the prices live in inventory-service. It is the only
synchronous call between the two services. Until now a single failed request meant a `503`, even
when a second attempt would have worked, and a fallen inventory-service kept every order request
waiting for its timeout. Besides, no test exercised the HTTP code of the adapter.

## Rules
- R1. A failed call is retried when a new attempt can succeed: no response (connection refused,
  timeout) or a `5xx`. At most 3 attempts, 200 ms apart. The call is a `GET`, so repeating it is safe.
- R2. A `4xx` is not retried: the request is wrong and repeating it changes nothing.
- R3. A circuit breaker watches the last calls. When half of them fail, it opens: for 10 seconds the
  calls fail at once, without reaching inventory-service. Then it lets a few through to check
  whether the service is back.
- R4. Whatever the cause (attempts exhausted, `4xx`, open circuit), the adapter throws
  `CatalogUnavailableException`, the only exception of the port. The API keeps responding `503`
  with the code `CATALOG_UNAVAILABLE`.
- R5. The settings of the client (base URL, timeouts) are one typed object validated at startup: a
  missing or blank value stops the application instead of failing on the first order.
- R6. Retries and circuit breaker are configuration (`resilience4j.*` in `application.yml`), not code.

## Acceptance criteria
- [x] The adapter turns the JSON of the catalog into a map of prices by SKU.
- [x] A `5xx` or a connection failure is attempted 3 times and ends in `CatalogUnavailableException`.
- [x] If the second attempt succeeds, the caller receives the prices and no exception.
- [x] A `4xx` is attempted once.
- [x] After repeated failures the circuit opens and the next call does not reach inventory-service.
- [x] The application does not start without `orderflow.inventory.base-url`.

## Out of scope
Caching the prices (a stale price is worse than a failed order), bulkheads and rate limiting.
