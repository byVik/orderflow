# 007 · Internacionalización y códigos de error

[English](../007-internationalization.md) · **Español**

## Contexto
La interfaz, las specs y el README estaban escritos en español. El repositorio es público y lo lee
gente que no habla español, así que el inglés pasa a ser el idioma principal y el español se
conserva como segundo. Los textos que ve el usuario estaban escritos dentro de los componentes, y
algunos (los errores y el motivo de rechazo de un pedido) los escribía el backend, así que el
navegador no podía traducirlos.

## Decisiones de diseño
- **El backend identifica; la interfaz traduce.** El backend no conoce el idioma del usuario. Cada
  error lleva un código estable y la interfaz construye el texto. La alternativa, traducir en el
  backend a partir de `Accept-Language`, metería textos de presentación en el dominio y no serviría
  para el motivo de rechazo, que lo produce un consumidor de Kafka sin petición HTTP.
- **vue-i18n con dos ficheros de mensajes.** El inglés es la referencia: su forma es el tipo de
  TypeScript que debe cumplir el fichero en español, así que una clave que falte es un error de
  compilación.
- **Las vistas guardan el error, no su texto.** El error se traduce en la plantilla, así que su
  texto cambia con el idioma como todo lo demás.
- **El motivo de rechazo es solo un código.** Ya no dice de qué producto faltaba stock; ese detalle
  va al log de inventory-service. Llevarlo exigiría un campo nuevo en el evento y una columna en las
  dos bases de datos.

## Reglas
- R1. La interfaz está disponible en inglés y en español. El inglés es el idioma por defecto.
- R2. El idioma se elige con un selector en la cabecera y en la pantalla de login. Se recuerda en el
  navegador y fija el atributo `lang` de la página.
- R3. Cambiar de idioma actualiza todos los textos visibles sin recargar, incluidos los precios, las
  fechas y un error que ya esté en pantalla.
- R4. Toda respuesta de error de order-service es un Problem Details (RFC 7807) con un `code`
  estable y, cuando el mensaje tiene datos, `params`. `detail` queda como texto de respaldo, en inglés.
- R5. La interfaz traduce los errores por `code`. Con un código desconocido muestra `detail`.
- R6. El motivo de rechazo de un pedido es un código: `INSUFFICIENT_STOCK`, `UNKNOWN_PRODUCT` o
  `CANCELLED_BEFORE_RESERVING`. La interfaz lo traduce; un motivo que no es un código conocido se
  muestra tal cual.
- R7. Los errores que detecta el cliente también tienen código: `NETWORK` y `SESSION_EXPIRED`.
- R8. Las specs y el README se escriben en inglés. Las versiones en español se conservan en
  `specs/es/` y `README.es.md`.

## Códigos de error de order-service

| Código | Estado | `params` |
|--------|--------|----------|
| `ORDER_NOT_FOUND` | 404 | — |
| `ORDER_NOT_PENDING` | 409 | `status` |
| `CONCURRENT_UPDATE` | 409 | — |
| `UNKNOWN_PRODUCT` | 400 | `sku` |
| `CUSTOMER_REQUIRED`, `ORDER_EMPTY`, `DUPLICATE_SKU`, `SKU_REQUIRED` | 400 | — |
| `QUANTITY_NOT_POSITIVE`, `NEGATIVE_PRICE` | 400 | `sku` |
| `QUANTITY_ABOVE_MAX` | 400 | `sku`, `max` |
| `VALIDATION_FAILED` | 400 | — (los campos van en `detail`) |
| `CATALOG_UNAVAILABLE` | 503 | — |

## Fuera de alcance
- Los nombres de los productos del catálogo (datos de prueba) y los comentarios del código siguen en español.
- La interfaz no traduce los mensajes por campo de Bean Validation.

## Criterios de aceptación
- [x] Los componentes se muestran en inglés por defecto y pasan a español sin volver a montarse.
- [x] El idioma elegido se guarda en el navegador y se aplica a `lang`.
- [x] Los precios y los plurales siguen al idioma.
- [x] Un Problem Details con `code` y `params` se convierte en un error que conserva los dos, y su
      texto se traduce en los dos idiomas.
- [x] Un código desconocido recurre a `detail`; un fallo de red y una sesión caducada tienen su propio código.
- [x] Toda respuesta de error de order-service lleva su `code`: 404, 409 (estado y concurrencia),
      400 (dominio y validación) y 503.
- [x] Las excepciones de dominio exponen su código y sus datos.
- [x] inventory-service rechaza con `INSUFFICIENT_STOCK`, y la línea de estado lo traduce.
