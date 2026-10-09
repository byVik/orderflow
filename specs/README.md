# Specifications (Spec Driven Development)

**English** · [Español](es/README.md)

Every feature starts here, as a specification, before any code is written. The spec defines
the expected behavior and the acceptance criteria. Those criteria are the basis for the tests
and the context given to the AI agents (Claude Code) to implement it.

| Spec | Status |
|------|--------|
| [001 · Place order](001-place-order.md) | ✅ Implemented |
| [002 · Stock reservation](002-stock-reservation.md) | ✅ Implemented |
| [003 · Frontend purchase flow](003-frontend-purchase-flow.md) | ✅ Implemented |
| [004 · Cancellation and stock release](004-cancellation-and-stock-release.md) | ✅ Implemented |
| [005 · Transactional outbox](005-transactional-outbox.md) | ✅ Implemented |
| [006 · Interface redesign](006-interface-redesign.md) | ✅ Implemented |
| [007 · Internationalization and error codes](007-internationalization.md) | ✅ Implemented |

Workflow:

1. Write or update the spec (context, rules, acceptance criteria).
2. Translate the criteria into tests.
3. Implement with the agent's help, using the spec as context.
4. Review the generated code, pass the tests and mark the spec as implemented.
