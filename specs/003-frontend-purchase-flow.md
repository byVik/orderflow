# 003 · Frontend purchase flow

**English** · [Español](es/003-frontend-flujo-compra.md)

## Screens
1. **Login (demo)**: username of 3 to 30 characters; stores the JWT and its expiry.
2. **Catalog**: cards with price and stock; low-stock warning (≤ 5); no more than the available stock can be added.
3. **Cart**: edit quantities (maximum 10 per line), remove lines, see the total and confirm the order.
4. **My orders**: list with status; it refreshes by itself while there are `PENDING` orders.
5. **Order detail**: lines, total, rejection reason and the option to cancel if it is `PENDING`.

## Rules
- Private routes redirect to `/login?redirect=…`.
- A `401` from the backend closes the session and goes back to the login.
- Backend errors (Problem Details, RFC 7807) are shown to the user with their `detail`.

## Acceptance criteria
- [x] The cart exceeds neither the stock nor the per-line maximum (store tests).
- [x] The request to the backend only includes `sku` and `quantity` (the price is set by the backend).
- [x] The token is sent as `Authorization: Bearer` and a `401` triggers the logout (HTTP client tests).

## Evolution
The presentation of these screens was redone in [spec 006](006-interface-redesign.md).
