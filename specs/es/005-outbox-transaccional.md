# 005 · Outbox transaccional

[English](../005-transactional-outbox.md) · **Español**

## Contexto
Los eventos se publicaban en Kafka justo después del commit. Si el servicio caía entre el commit y
el envío, o Kafka no estaba disponible, el evento se perdía y el pedido quedaba `PENDING` para
siempre. Guardar en la base de datos y publicar en Kafka son dos escrituras sin transacción común.

## Reglas
- R1. Los adaptadores de publicación ya no llaman a Kafka: insertan una fila en la tabla `outbox`
  (topic, clave, payload JSON) **en la misma transacción** que el cambio de negocio.
- R2. Publicar fuera de una transacción es un error de programación y falla (`MANDATORY`).
- R3. Un *relay* lee las filas pendientes en orden, las envía a Kafka esperando la confirmación del
  broker y las marca como publicadas.
- R4. El relay bloquea las filas con `FOR UPDATE SKIP LOCKED`: varias instancias no envían la misma fila.
- R5. Si el envío falla, la transacción del relay hace rollback y las filas se reintentan en la
  siguiente pasada.
- R6. La garantía es **at-least-once**: puede haber duplicados, y los consumidores ya son idempotentes
  (specs 002 y 004).
- R7. Los puertos de salida (`OrderEventPublisher`, `StockEventPublisher`) no cambian: es un cambio
  de adaptador.

## Criterios de aceptación
- [x] Al crear un pedido, `OrderPlaced` llega a Kafka y la fila del outbox queda marcada como publicada.
- [x] Al reservar stock, `StockResult` llega a Kafka a través del outbox.
- [x] Si la transacción de negocio hace rollback, no queda fila en el outbox.

## Pendiente / mejoras
- Purgar las filas publicadas antiguas.
- Sustituir el sondeo por CDC (Debezium) si la latencia o el volumen lo piden.
- Con varias instancias del relay el orden global entre lotes no está garantizado.
