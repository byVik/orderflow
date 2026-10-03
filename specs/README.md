# Especificaciones (Spec Driven Development)

Cada funcionalidad empieza aquí, como especificación, antes de escribir código. La spec define
el comportamiento esperado y los criterios de aceptación. Esos criterios son la base de los tests
y el contexto que se da a los agentes de IA (Claude Code) para implementar.

| Spec | Estado |
|------|--------|
| [001 · Crear pedido](001-crear-pedido.md) | ✅ Implementada |
| [002 · Reserva de stock](002-reserva-stock.md) | ✅ Implementada |
| [003 · Flujo de compra en el frontend](003-frontend-flujo-compra.md) | ✅ Implementada |

Flujo de trabajo:

1. Escribir o actualizar la spec (contexto, reglas, criterios de aceptación).
2. Traducir los criterios a tests.
3. Implementar con ayuda del agente, usando la spec como contexto.
4. Revisar el código generado, pasar los tests y marcar la spec como implementada.
