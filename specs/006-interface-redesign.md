# 006 · Interface redesign

**English** · [Español](es/006-rediseno-interfaz.md)

## Context
The first interface met spec 003 but was generic and did not explain what is most distinctive about
the system: that the order is confirmed asynchronously. The redesign was first prototyped in **Claude Design**
(a canvas with the five screens) and then implemented in Vue 3 + Tailwind with Claude Code.

## Design decisions
- **One status line per order.** The detail shows the three steps of the saga in the words of
  the person buying: order received, stock reservation and result. It replaces the "esperando…" text.
- **Figures in monospace.** SKUs, identifiers and prices use JetBrains Mono with fixed-width
  digits; everything else, Hanken Grotesk.
- **A single accent color** (dark teal, contrast ≥ 4.5:1 with white text). Amber for
  what is pending and red for what is rejected.
- **The status does not depend on color alone**: each status also has a shape (dot, square,
  check mark, cross) and a text.
- **Controls of at least 44 px** and visible focus on all interactive elements.

## Rules
- R1. The five screens of spec 003 keep their behavior; the presentation changes.
- R2. The add button respects the same limit as the cart (stock and per-line maximum).
- R3. The cart is saved in the browser and checked against the catalog on return: prices are
  updated and quantities that are no longer available are trimmed.
- R4. The polling of pending orders spaces out the requests (from 1.5 s to 10 s) and gives up after 40,
  notifying the user.
- R5. An expired session leads to the login, remembering the page; logging out manually empties the cart.
- R6. The pages work at mobile width: the header collapses and the tables scroll.

## Acceptance criteria
- [x] The status line shows the correct steps for each of the four statuses.
- [x] The cart survives a reload and adjusts when the catalog is refreshed.
- [x] The polling spaces out the requests, stops on unmount and gives up on reaching the maximum.
- [x] Without a session, or with an expired session, private routes redirect to the login with `redirect`.
