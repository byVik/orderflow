# 002 · Reserva de stock (saga coreografiada)

## Contexto
inventory-service escucha `OrderPlaced`, intenta reservar el stock y publica el resultado en
`inventory.stock-result.v1`. order-service consume ese resultado y actualiza el pedido.

## Reglas
- R1. **Todo o nada**: si falta stock de una sola línea, no se reserva nada.
- R2. La reserva bloquea las filas de producto (`SELECT … FOR UPDATE`) en orden de SKU para evitar sobreventa y bloqueos cruzados.
- R3. **Idempotencia**: si llega dos veces el mismo `orderId`, no se vuelve a descontar stock y se reenvía el resultado original.
- R4. order-service ignora resultados para pedidos que ya no están `PENDING` (duplicados o pedidos cancelados).
- R5. Un mensaje que falla tras 3 reintentos se envía a `<topic>-dlt` (dead-letter topic).
- R6. La clave de los mensajes es el `orderId`, así se mantiene el orden de los eventos de un mismo pedido.

## Criterios de aceptación
- [x] Con stock suficiente, el stock baja y el pedido pasa a `CONFIRMED`.
- [x] Sin stock suficiente, el stock no cambia y el pedido pasa a `REJECTED` con el motivo.
- [x] Un `OrderPlaced` duplicado descuenta stock una sola vez.
- [x] Un resultado duplicado no cambia un pedido ya confirmado.

## Pendiente / mejoras
- Patrón *transactional outbox* para garantizar la publicación aunque el servicio caiga justo después del commit.
- Liberar el stock cuando se cancela un pedido ya confirmado.
