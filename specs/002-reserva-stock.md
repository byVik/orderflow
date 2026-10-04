# 002 · Reserva de stock (saga coreografiada)

## Contexto
inventory-service escucha `OrderPlaced`, intenta reservar el stock y publica el resultado en
`inventory.stock-result.v1`. order-service consume ese resultado y actualiza el pedido.

## Reglas
- R1. **Todo o nada**: si falta stock de una sola línea, no se reserva nada.
- R2. La reserva bloquea las filas de producto (`SELECT … FOR UPDATE`) en orden de SKU para evitar sobreventa y bloqueos cruzados.
- R3. **Idempotencia**: si llega dos veces el mismo `orderId`, no se vuelve a descontar stock y se reenvía el resultado original. La comprobación se hace **después** de tomar los bloqueos, y la clave primaria de la reserva actúa de red de seguridad.
- R4. order-service ignora resultados para pedidos que ya no están `PENDING` (duplicados o pedidos cancelados).
- R5. Un mensaje que falla tras 3 reintentos se envía a `<topic>-dlt` (dead-letter topic). Los topics `-dlt` se declaran de forma explícita y cada envío deja un log de error.
- R6. La clave de los mensajes es el `orderId`, así se mantiene el orden de los eventos de un mismo pedido.

## Criterios de aceptación
- [x] Con stock suficiente, el stock baja y el pedido pasa a `CONFIRMED`.
- [x] Sin stock suficiente, el stock no cambia y el pedido pasa a `REJECTED` con el motivo.
- [x] Un `OrderPlaced` duplicado descuenta stock una sola vez.
- [x] Un resultado duplicado no cambia un pedido ya confirmado.
- [x] Varias reservas simultáneas sobre las últimas unidades no venden de más.

## Resuelto en specs posteriores
- Liberar el stock al cancelar: [spec 004](004-cancelacion-y-liberacion-de-stock.md).
- Garantizar la publicación de eventos: [spec 005](005-outbox-transaccional.md).
