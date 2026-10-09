# 007 · Internationalization and error codes

**English** · [Español](es/007-internacionalizacion.md)

## Context
The interface, the specs and the README were written in Spanish. The repository is public and is
read by people who do not speak Spanish, so English becomes the main language and Spanish is kept
as a second one. The texts the user sees were hard-coded in the components, and some of them (the
errors and the reason an order is rejected) were written by the backend, so the browser could not
translate them.

## Design decisions
- **The backend identifies; the interface translates.** The backend does not know the user's
  language. Every error carries a stable code and the interface builds the text. The alternative,
  translating in the backend from `Accept-Language`, would put presentation texts in the domain and
  would not work for the rejection reason, which is produced by a Kafka consumer with no HTTP request.
- **vue-i18n with two message files.** English is the reference: its shape is the TypeScript type
  the Spanish file must satisfy, so a missing key is a compile error.
- **Views keep the error, not its text.** The error is translated in the template, so its text
  changes with the language like everything else.
- **The rejection reason is only a code.** It no longer says which product was short; that detail
  goes to the inventory-service log. Carrying it would need a new field in the event and a column
  in both databases.

## Rules
- R1. The interface is available in English and Spanish. English is the default.
- R2. The language is chosen with a switch in the header and on the login screen. It is remembered
  in the browser and it sets the `lang` attribute of the page.
- R3. Changing the language updates every visible text without reloading, including prices, dates
  and an error that is already on screen.
- R4. Every error response of order-service is an RFC 7807 Problem Details with a stable `code` and,
  when the message has data, `params`. `detail` stays as a fallback text, in English.
- R5. The interface translates errors by `code`. With an unknown code it shows `detail`.
- R6. The rejection reason of an order is a code: `INSUFFICIENT_STOCK`, `UNKNOWN_PRODUCT` or
  `CANCELLED_BEFORE_RESERVING`. The interface translates it; a reason that is not a known code is
  shown as it is.
- R7. Errors detected by the client also have a code: `NETWORK` and `SESSION_EXPIRED`.
- R8. Specs and README are written in English. The Spanish versions are kept in `specs/es/` and
  `README.es.md`.

## Error codes of order-service

| Code | Status | `params` |
|------|--------|----------|
| `ORDER_NOT_FOUND` | 404 | — |
| `ORDER_NOT_PENDING` | 409 | `status` |
| `CONCURRENT_UPDATE` | 409 | — |
| `UNKNOWN_PRODUCT` | 400 | `sku` |
| `CUSTOMER_REQUIRED`, `ORDER_EMPTY`, `DUPLICATE_SKU`, `SKU_REQUIRED` | 400 | — |
| `QUANTITY_NOT_POSITIVE`, `NEGATIVE_PRICE` | 400 | `sku` |
| `QUANTITY_ABOVE_MAX` | 400 | `sku`, `max` |
| `VALIDATION_FAILED` | 400 | — (the fields are listed in `detail`) |
| `CATALOG_UNAVAILABLE` | 503 | — |

## Out of scope
- Product names in the catalog (seed data) and the comments in the code stay in Spanish.
- The per-field messages of Bean Validation are not translated by the interface.

## Acceptance criteria
- [x] Components show English by default and switch to Spanish without being remounted.
- [x] The chosen language is saved in the browser and applied to `lang`.
- [x] Prices and plurals follow the language.
- [x] A Problem Details with `code` and `params` becomes an error that keeps both, and its text is
      translated in the two languages.
- [x] An unknown code falls back to `detail`; a network failure and an expired session have their own code.
- [x] Every error response of order-service carries its `code`: 404, 409 (status and concurrency),
      400 (domain and validation) and 503.
- [x] Domain exceptions expose their code and params.
- [x] inventory-service rejects with `INSUFFICIENT_STOCK`, and the status line translates it.
