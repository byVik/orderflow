# 004 · Cancelación y liberación de stock

[English](../004-cancellation-and-stock-release.md) · **Español**

## Contexto
Hasta ahora cancelar un pedido `PENDING` solo cambiaba su estado. Si `OrderPlaced` ya estaba en
camino, inventory-service reservaba el stock igualmente y el resultado se ignoraba: el stock
quedaba descontado para un pedido cancelado. Esta spec añade la **compensación** de la saga.

## Reglas
- R1. Al cancelar un pedido se publica `OrderCancelled` en `orders.order-cancelled.v1` (clave = `orderId`).
- R2. inventory-service guarda las líneas de cada reserva para poder devolverlas.
- R3. Al recibir `OrderCancelled`:
  - si la reserva existe y está `RESERVED`, se devuelve el stock y la reserva pasa a `RELEASED`;
  - si está `REJECTED` o `RELEASED`, no se hace nada (idempotencia);
  - si **no existe**, se registra como `REJECTED` con el motivo `CANCELLED_BEFORE_RESERVING` (un código desde la [spec 007](007-internacionalizacion.md)). Así un `OrderPlaced`
    que llegue después no reserva nada.
- R4. Entre topics distintos no hay garantía de orden: `OrderCancelled` puede procesarse antes que
  `OrderPlaced`. R3 cubre los dos órdenes.
- R5. La liberación bloquea las filas de producto en orden de SKU, igual que la reserva.
- R6. Si cancelar y confirmar coinciden, gana el primero en hacer commit (bloqueo optimista). La
  cancelación que pierde responde `409`, nunca `500`.

## Criterios de aceptación
- [x] Reservar y después cancelar devuelve el stock a su valor inicial.
- [x] Un `OrderCancelled` duplicado no devuelve el stock dos veces.
- [x] Si `OrderCancelled` llega antes que `OrderPlaced`, el stock no cambia.
- [x] Cancelar un pedido publica `OrderCancelled`.
- [x] Un conflicto de concurrencia al cancelar responde `409`.

## Fuera de alcance
Cancelar pedidos ya `CONFIRMED` (requiere devoluciones) y reembolsos.
