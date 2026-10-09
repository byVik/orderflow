# 006 · Rediseño de la interfaz

[English](../006-interface-redesign.md) · **Español**

## Contexto
La primera interfaz cumplía la spec 003 pero era genérica y no explicaba lo más propio del sistema:
que el pedido se confirma de forma asíncrona. El rediseño se prototipó primero en **Claude Design**
(un lienzo con las cinco pantallas) y después se implementó en Vue 3 + Tailwind con Claude Code.

## Decisiones de diseño
- **Una línea de estado por pedido.** El detalle muestra los tres pasos de la saga con palabras de
  quien compra: pedido recibido, reserva de stock y resultado. Sustituye al texto «esperando…».
- **Cifras en monoespaciada.** SKUs, identificadores y precios usan JetBrains Mono con dígitos de
  ancho fijo; el resto, Hanken Grotesk.
- **Un solo color de acento** (verde azulado oscuro, contraste ≥ 4,5:1 con texto blanco). Ámbar para
  lo pendiente y rojo para lo rechazado.
- **El estado no depende solo del color**: cada estado tiene además una forma (punto, cuadrado,
  marca de verificación, aspa) y un texto.
- **Controles de 44 px como mínimo** y foco visible en todos los elementos interactivos.

## Reglas
- R1. Las cinco pantallas de la spec 003 mantienen su comportamiento; cambia la presentación.
- R2. El botón de añadir respeta el mismo límite que el carrito (stock y máximo por línea).
- R3. El carrito se guarda en el navegador y se revisa contra el catálogo al volver: se actualizan
  precios y se recortan cantidades que ya no hay.
- R4. El sondeo de pedidos pendientes espacia las consultas (de 1,5 s a 10 s) y se rinde tras 40,
  avisando al usuario.
- R5. Una sesión caducada lleva al login recordando la página; salir a mano vacía el carrito.
- R6. Las páginas funcionan a ancho de móvil: la cabecera se pliega y las tablas se desplazan.

## Criterios de aceptación
- [x] La línea de estado muestra los pasos correctos para cada uno de los cuatro estados.
- [x] El carrito sobrevive a una recarga y se ajusta al refrescar el catálogo.
- [x] El sondeo espacia las consultas, se detiene al desmontar y se rinde al llegar al máximo.
- [x] Sin sesión, o con la sesión caducada, las rutas privadas redirigen al login con `redirect`.
