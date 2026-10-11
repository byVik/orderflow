# OrderFlow

[![CI](https://github.com/byVik/orderflow/actions/workflows/ci.yml/badge.svg)](https://github.com/byVik/orderflow/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-orange)
![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-event--driven-231F20)
![Vue 3](https://img.shields.io/badge/Vue-3%20%2B%20TypeScript-42b883)

[English](README.md) · **Español**

Gestión de pedidos con **microservicios event-driven**. Un usuario crea un pedido, el servicio de
inventario reserva el stock de forma asíncrona a través de **Kafka** y el pedido pasa a confirmado
o rechazado. Si el usuario cancela, el stock reservado se devuelve. Incluye un frontend en
**Vue 3 + TypeScript**.

Es un proyecto personal para practicar **Spring Boot**, **arquitectura hexagonal / DDD** y
**mensajería con Kafka**, desarrollado con un flujo de **Spec Driven Development asistido por IA**
(ver [Desarrollo con IA](#desarrollo-con-ia-y-sdd)).

| Catálogo | Seguimiento del pedido |
|----------|------------------------|
| ![Catálogo](docs/screenshots/02-catalog.png) | ![Pedido pendiente](docs/screenshots/05-order-pending.png) |

Más capturas en [docs/screenshots](docs/screenshots/). La interfaz está disponible en inglés y en
español, con un selector en la cabecera; las capturas muestran la versión en inglés.

---

## Arquitectura

```mermaid
flowchart LR
    U([Usuario]) --> FE[Frontend<br/>Vue 3 + TS]
    FE -- REST + JWT --> OS[order-service<br/>Spring Boot]
    FE -- REST --> IS[inventory-service<br/>Spring Boot]
    OS -- precios del catálogo --> IS
    OS -- OrderPlaced / OrderCancelled --> K[(Kafka)]
    K -- OrderPlaced / OrderCancelled --> IS
    IS -- StockResult --> K
    K -- StockResult --> OS
    OS --- DB1[(PostgreSQL<br/>orders + outbox)]
    IS --- DB2[(PostgreSQL<br/>inventory + outbox)]
```

**Flujo de un pedido (saga coreografiada):**

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant OS as order-service
    participant K as Kafka
    participant IS as inventory-service
    FE->>OS: POST /api/orders
    OS->>IS: GET /api/products?skus=… (precios, fuera de la transacción)
    OS->>OS: guarda pedido PENDING + evento en outbox (misma transacción)
    OS-->>FE: 201 Created
    OS->>K: relay del outbox publica OrderPlaced
    K->>IS: OrderPlaced
    IS->>IS: reserva todo o nada (SELECT … FOR UPDATE) + resultado en outbox
    IS->>K: StockResult RESERVED / REJECTED
    K->>OS: StockResult
    OS->>OS: pedido → CONFIRMED / REJECTED
    FE->>OS: polling GET /api/orders/{id}
    opt El usuario cancela un pedido pendiente
        FE->>OS: POST /api/orders/{id}/cancel
        OS->>K: OrderCancelled
        K->>IS: OrderCancelled
        IS->>IS: devuelve el stock reservado (compensación)
    end
```

### Arquitectura hexagonal en cada servicio

```
order-service/src/main/java/.../order
├── domain/                  # Modelo y reglas de negocio (sin Spring ni JPA)
│   ├── model/               # Order (agregado), OrderLine, OrderId, OrderStatus
│   └── exception/
├── application/
│   ├── port/in/             # Casos de uso: PlaceOrder, GetOrders, CancelOrder, ApplyStockResult
│   ├── port/out/            # Lo que necesita el núcleo: OrderRepository, OrderEventPublisher, ProductCatalog
│   └── service/             # Implementación de los casos de uso
└── infrastructure/
    ├── adapter/in/web/      # REST (controladores, DTOs, Problem Details)
    ├── adapter/in/messaging/# Listener de Kafka
    ├── adapter/out/persistence/ # JPA + Flyway
    ├── adapter/out/messaging/   # Outbox y relay hacia Kafka
    ├── adapter/out/catalog/     # Cliente REST de inventory-service
    └── config/              # Seguridad, Kafka, OpenAPI, ensamblado de beans
```

El dominio no conoce Spring: los adaptadores dependen del núcleo y nunca al revés. Un test de
**ArchUnit** lo comprueba en cada build.

### Frontend organizado por dominio

```
frontend/src
├── features/                # Una carpeta por dominio: vistas, componentes, store, cliente de la API, tipos y tests
│   ├── auth/                # Login y sesión
│   ├── catalog/             # Catálogo y tarjeta de producto
│   ├── cart/                # Carrito
│   └── orders/              # Lista y detalle de pedidos, etiqueta de estado, línea de estado
├── shared/                  # Lo que usan todos los dominios: cliente HTTP, i18n, cabecera, sondeo, formato
└── router/                  # Rutas y guard de sesión
```

Dentro de un dominio los imports son relativos; entre dominios y hacia `shared/` van por el alias `@/`.

## Stack

| Capa | Tecnologías |
|------|-------------|
| Backend | Java 21, Spring Boot 3.5, Spring Web, Spring Data JPA / Hibernate, Bean Validation, Resilience4j (reintentos + *circuit breaker*), MapStruct, Lombok |
| Mensajería | Apache Kafka (KRaft), Spring Kafka, outbox transaccional, reintentos + dead-letter topic |
| Persistencia | PostgreSQL 16, Flyway, base de datos por servicio |
| Seguridad | Spring Security, OAuth2 Resource Server, JWT |
| API | OpenAPI 3 / Swagger UI (springdoc), errores RFC 7807 (Problem Details) con códigos de error estables |
| Frontend | Vue 3 (Composition API, `<script setup>`), TypeScript, Pinia, Vue Router, vue-i18n (inglés / español), Tailwind CSS, Vite |
| Testing | JUnit 5, Mockito, AssertJ, MockMvc, **Testcontainers** (PostgreSQL + Kafka), Awaitility, ArchUnit, Vitest, Vue Test Utils |
| DevOps | Docker multi-stage, Docker Compose, GitHub Actions (CI), JaCoCo |

## Decisiones técnicas

- **Hexagonal + DDD:** el agregado `Order` protege sus invariantes (sin líneas vacías, sin SKUs duplicados, entre 1 y 100 unidades por línea, transiciones de estado válidas). Los casos de uso son interfaces (puertos de entrada) y la infraestructura se enchufa por puertos de salida.
- **El precio lo decide el backend:** el cliente solo envía `sku` y `quantity`. order-service consulta el catálogo, con *timeouts* y **fuera de la transacción** para no retener una conexión a la base de datos durante una llamada HTTP. Una llamada fallida se reintenta cuando otro intento puede servir (sin respuesta o un `5xx`), y un ***circuit breaker*** deja de llamar a un catálogo que falla una y otra vez, para que las peticiones de pedido fallen rápido en vez de acumularse esperando su *timeout* (Resilience4j, [spec 009](specs/009-resilient-catalog-client.md)). Si el catálogo no responde: `503`.
- **Outbox transaccional:** guardar en la base de datos y publicar en Kafka son dos escrituras sin transacción común. El evento se inserta en una tabla `outbox` en la misma transacción que el pedido y un *relay* lo publica después (`FOR UPDATE SKIP LOCKED`, espera la confirmación del broker). Ni eventos fantasma ni eventos perdidos. Los puertos no cambiaron: fue un cambio de adaptador.
- **Compensación de la saga:** cancelar publica `OrderCancelled` e inventory devuelve el stock. Como entre topics distintos no hay orden garantizado, si la cancelación llega antes que el pedido queda registrada y ese pedido ya no reserva nada.
- **Idempotencia:** inventory-service guarda cada reserva por `orderId` y lo comprueba después de tomar los bloqueos; la clave primaria es la red de seguridad. order-service ignora resultados para pedidos que ya no están `PENDING`.
- **Sin sobreventa:** la reserva bloquea las filas de producto (`PESSIMISTIC_WRITE`) en orden de SKU para evitar interbloqueos. Es todo o nada, y hay un test con compradores simultáneos.
- **Concurrencia en pedidos:** bloqueo optimista (`@Version`). Si cancelar y confirmar coinciden, gana el primero y el otro recibe un `409`.
- **Listado de pedidos paginado:** `GET /api/orders?page=0&size=10` devuelve una página y `hasNext`, sin total, así no hay un `count` en cada petición. Una página cuesta dos consultas sea cual sea su tamaño (los pedidos con `LIMIT` y después las líneas de todos en un lote), y un test de integración las cuenta. Los puertos usan sus propios `PageQuery` / `PageResult`: el `Pageable` de Spring Data se queda en el adaptador de persistencia ([spec 008](specs/008-order-list-pagination.md)).
- **Código generado solo en los bordes:** Lombok escribe los getters y constructores de las entidades JPA y de los adaptadores, y MapStruct el mapeo petición → comando, donde los dos lados son records sin lógica. El dominio y la capa de aplicación no usan ninguno: `Order` se mapea a mano porque tiene comportamiento, no getters.
- **Orden de eventos:** la clave del mensaje es el `orderId`, así todos los eventos de un pedido van a la misma partición.
- **Errores en consumidores:** 3 reintentos y después el mensaje va a un *dead-letter topic* declarado, dejando un log de error.
- **El backend identifica los errores y la interfaz los traduce:** cada respuesta de error lleva un `code` estable (y `params` cuando el mensaje tiene datos), y el motivo de rechazo de un pedido también es un código. La interfaz los convierte en texto en inglés o en español, y recurre al `detail` en inglés si no conoce el código. El dominio no guarda textos para el usuario ([spec 007](specs/007-internationalization.md)).
- **Seguro por defecto:** el login de demo y su secreto JWT solo existen con el perfil `dev`. Sin perfil, order-service exige `JWT_SECRET` y no expone `/api/auth/token`. En producción los servicios serían resource servers de Keycloak/Auth0.

## Cómo ejecutarlo

### Todo con Docker

```bash
docker compose up --build
```

| Servicio | URL |
|----------|-----|
| Frontend | http://localhost:8080 |
| Swagger order-service | http://localhost:8081/swagger-ui.html |
| Swagger inventory-service | http://localhost:8082/swagger-ui.html |

Entra con cualquier nombre de usuario, añade productos al carrito y haz un pedido. Para ver un
rechazo, pide más unidades del micrófono (`MC-01`, solo hay 3). `docker compose down -v` borra
los datos y deja el catálogo como al principio.

Las credenciales de la base de datos son por defecto `orderflow` / `orderflow`. Para cambiarlas,
copia [.env.example](.env.example) a `.env` (ignorado por git) antes del primer arranque.

### Modo desarrollo

```bash
docker compose up -d postgres kafka                                   # infraestructura
mvn install -DskipTests                                               # una vez: instala el módulo events
mvn -pl inventory-service spring-boot:run                             # puerto 8082
mvn -pl order-service spring-boot:run -Dspring-boot.run.profiles=dev  # puerto 8081, login de demo
cd frontend && npm install && npm run dev                             # http://localhost:5173 (proxy a los servicios)
```

### Probar la API con curl

```bash
TOKEN=$(curl -s -X POST localhost:8081/api/auth/token \
  -H 'Content-Type: application/json' -d '{"username":"viktor"}' | jq -r .accessToken)

curl -s -X POST localhost:8081/api/orders -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"lines":[{"sku":"KB-01","quantity":1}]}'

curl -s "localhost:8081/api/orders?page=0&size=10" -H "Authorization: Bearer $TOKEN"
```

## Tests

```bash
mvn test                   # unitarios, web y arquitectura: rápidos, sin Docker
mvn verify                 # además los de integración (necesita Docker para Testcontainers)
cd frontend && npm test    # Vitest
```

- **Dominio:** reglas de los agregados y transiciones de estado.
- **Aplicación:** casos de uso con Mockito (precios del catálogo, propiedad del pedido, idempotencia, compensación).
- **Web:** `@WebMvcTest` con la configuración de seguridad real: tokens válidos, caducados y firmados con otra clave; validación del cuerpo y de los parámetros de página, 401, 404, 409 y 503.
- **Cliente HTTP:** el adaptador que llama a inventory-service contra un servidor simulado (`MockRestServiceServer`), y sus reintentos y su *circuit breaker* en un contexto de Spring mínimo: cuántas veces se intenta un `5xx`, un `4xx` y una conexión caída, y que un circuito abierto corta las llamadas.
- **Arquitectura:** ArchUnit comprueba que el dominio no depende de frameworks ni de otras capas, y que la capa de aplicación no usa Spring Data.
- **Integración:** `@SpringBootTest` con PostgreSQL y Kafka reales en Testcontainers. Prueban el outbox de extremo a extremo, la idempotencia ante duplicados, la liberación de stock (incluida la cancelación que llega antes que el pedido) que varias reservas simultáneas no venden de más, y que una página de pedidos cuesta dos consultas SQL (la guarda contra el N+1).
- **Frontend:** stores, cliente HTTP, guard del router, sondeo con temporizadores simulados, componentes, y la traducción de errores y textos en los dos idiomas.

La CI de GitHub Actions ejecuta todo en cada push y construye las imágenes Docker.

## Desarrollo con IA y SDD

El proyecto se ha desarrollado con **Spec Driven Development** y **Claude Code** como asistente:

1. Cada funcionalidad empieza como especificación en [`/specs`](specs/) (en inglés), con reglas y criterios de aceptación.
2. Los criterios se convierten en tests.
3. La implementación se hace con el agente, usando la spec como contexto, y **todo el código se revisa** antes de integrarlo.

Dos ejemplos de cómo se usó el agente más allá de escribir código:

- **Revisión del propio diseño.** Una auditoría con el agente encontró fallos que los tests no cubrían: una cancelación dejaba el stock descontado, y un evento podía perderse si el servicio caía justo tras el commit. La fuga de stock se reprodujo primero contra el sistema en marcha, y los dos se resolvieron con su spec ([004](specs/004-cancellation-and-stock-release.md), [005](specs/005-transactional-outbox.md)) y sus tests.
- **Diseño de la interfaz.** Las pantallas se prototiparon en **Claude Design** (un lienzo con las cinco vistas, generado desde Claude Code) y después se implementaron en Vue 3 + Tailwind ([spec 006](specs/006-interface-redesign.md)). El resultado se comprobó con capturas automáticas en escritorio y móvil, que son las de este README.

La IA acelera la implementación. Las decisiones de diseño, la revisión y la responsabilidad del
código son mías.

## Próximos pasos

- [ ] Observabilidad: trazas distribuidas a través de Kafka (un pedido seguido de extremo a extremo), Micrometer + Prometheus + Grafana.
- [ ] Endurecer el relay del outbox: acotar la espera del productor cuando Kafka no responde (`max.block.ms`) y purgar las filas ya publicadas.
- [ ] Test de extremo a extremo con Playwright en la CI, sobre `docker compose up`.
- [ ] Paginación por clave en el listado de pedidos: con `OFFSET`, las páginas lejanas son más lentas cuanto más crece la tabla.
- [ ] Keycloak como Identity Provider real (RS256, `issuer-uri`).
- [ ] Una copia local de precios alimentada por eventos, para que crear un pedido no dependa de que inventory-service esté en pie.
- [ ] Contratos de eventos con Schema Registry (Avro o Protobuf).
- [ ] Herramienta para reprocesar el dead-letter topic.
- [ ] Análisis estático con SonarCloud en la CI.

## Autor

**Viktor Strohush Loyish** · Full Stack Developer (Java · Vue 3)
[LinkedIn](https://www.linkedin.com/in/viktor-strohush-loyish)
