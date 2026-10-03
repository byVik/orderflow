# 001 · Crear pedido

## Contexto
Un usuario autenticado crea un pedido con uno o varios productos del catálogo. El pedido no se
confirma al instante: queda `PENDING` hasta que inventory-service reserve el stock (ver spec 002).

## Reglas de negocio
- R1. Un pedido tiene al menos una línea y no repite SKUs.
- R2. La cantidad de cada línea está entre 1 y 100.
- R3. El precio **no** lo envía el cliente: se obtiene del catálogo en el momento de crear el pedido.
- R4. Si algún SKU no existe en el catálogo, el pedido se rechaza con `400`.
- R5. El cliente del pedido es el `sub` del JWT; un usuario solo puede ver y cancelar sus pedidos.
- R6. Solo se puede cancelar un pedido `PENDING`; en otro caso se responde `409`.
- R7. Al crearse el pedido se publica `OrderPlaced` en `orders.order-placed.v1`, **después** del commit.

## Estados
`PENDING → CONFIRMED | REJECTED | CANCELLED` (las tres son estados finales).

## API
| Método | Ruta | Respuesta |
|--------|------|-----------|
| POST | `/api/orders` | `201` + `Location` |
| GET | `/api/orders` | pedidos del usuario, del más reciente al más antiguo |
| GET | `/api/orders/{id}` | `404` si no existe o es de otro usuario |
| POST | `/api/orders/{id}/cancel` | `409` si no está `PENDING` |

## Criterios de aceptación
- [x] Dado un usuario autenticado y productos existentes, cuando crea un pedido, recibe `201`, el pedido está `PENDING` y el total se calcula con los precios del catálogo.
- [x] Dado un pedido sin líneas o con cantidad 0, cuando se envía, recibe `400`.
- [x] Dado un pedido de otro usuario, cuando lo consulta, recibe `404`.
- [x] Dado un pedido `CONFIRMED`, cuando intenta cancelarlo, recibe `409`.
- [x] Sin token, cualquier endpoint de pedidos responde `401`.

## Fuera de alcance
Pagos, envíos y edición de un pedido ya creado.
