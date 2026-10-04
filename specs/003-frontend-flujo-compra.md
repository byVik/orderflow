# 003 · Flujo de compra en el frontend

## Pantallas
1. **Login (demo)**: usuario de 3 a 30 caracteres; guarda el JWT y su caducidad.
2. **Catálogo**: tarjetas con precio y stock; aviso de stock bajo (≤ 5); no se puede añadir más que el stock disponible.
3. **Carrito**: editar cantidades (máximo 10 por línea), quitar líneas, ver el total y confirmar el pedido.
4. **Mis pedidos**: listado con estado; se refresca solo mientras haya pedidos `PENDING`.
5. **Detalle de pedido**: líneas, total, motivo de rechazo y opción de cancelar si está `PENDING`.

## Reglas
- Las rutas privadas redirigen a `/login?redirect=…`.
- Un `401` del backend cierra la sesión y vuelve al login.
- Los errores del backend (Problem Details, RFC 7807) se muestran al usuario con su `detail`.

## Criterios de aceptación
- [x] El carrito no supera el stock ni el máximo por línea (tests del store).
- [x] La petición al backend solo incluye `sku` y `quantity` (el precio lo pone el backend).
- [x] El token se envía como `Authorization: Bearer` y un `401` dispara el logout (tests del cliente HTTP).

## Evolución
La presentación de estas pantallas se rehízo en la [spec 006](006-rediseno-interfaz.md).
