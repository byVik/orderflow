package dev.viktorstrohush.orderflow.inventory.integration;

import dev.viktorstrohush.orderflow.events.OrderCancelledEvent;
import dev.viktorstrohush.orderflow.events.OrderPlacedEvent;
import dev.viktorstrohush.orderflow.events.Topics;
import dev.viktorstrohush.orderflow.inventory.application.port.in.ListProductsQuery;
import dev.viktorstrohush.orderflow.inventory.application.port.in.ReserveStockUseCase;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationRequest;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Cada test usa un producto distinto del catálogo de demo para no interferir con los demás. */
@SpringBootTest
@Import(ContainersConfig.class)
class InventoryIntegrationTest {

    @Autowired
    KafkaContainer kafka;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    ListProductsQuery products;
    @Autowired
    ReserveStockUseCase reserveStock;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void unPedidoRecibidoPorKafkaDescuentaElStockUnaSolaVez() {
        int before = stockOf("WC-01");
        UUID orderId = UUID.randomUUID();

        sendOrderPlaced(orderId, "WC-01", 2);
        sendOrderPlaced(orderId, "WC-01", 2); // duplicado

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(stockOf("WC-01")).isEqualTo(before - 2));
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(stockOf("WC-01")).isEqualTo(before - 2));
    }

    @Test
    void elResultadoDeLaReservaLlegaAKafkaPorElOutbox() {
        UUID orderId = UUID.randomUUID();

        sendOrderPlaced(orderId, "LS-01", 1);

        assertThat(awaitMessage(Topics.STOCK_RESULT, orderId)).startsWith("{").contains("\"status\":\"RESERVED\"");
    }

    @Test
    void cancelarUnPedidoDevuelveElStockUnaSolaVez() {
        int before = stockOf("HD-01");
        UUID orderId = UUID.randomUUID();
        sendOrderPlaced(orderId, "HD-01", 2);
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(stockOf("HD-01")).isEqualTo(before - 2));

        sendOrderCancelled(orderId);
        sendOrderCancelled(orderId); // duplicado

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(stockOf("HD-01")).isEqualTo(before));
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(stockOf("HD-01")).isEqualTo(before));
        assertThat(statusOf(orderId)).isEqualTo("RELEASED");
    }

    @Test
    void siLaCancelacionLlegaAntesQueElPedidoNoSeReservaNada() {
        int before = stockOf("DK-01");
        UUID orderId = UUID.randomUUID();

        // Topics distintos: Kafka no garantiza que OrderPlaced se procese antes que OrderCancelled.
        sendOrderCancelled(orderId);
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(statusOf(orderId)).isEqualTo("REJECTED"));
        sendOrderPlaced(orderId, "DK-01", 1);

        assertThat(awaitMessage(Topics.STOCK_RESULT, orderId)).contains("\"status\":\"REJECTED\"");
        assertThat(stockOf("DK-01")).isEqualTo(before);
    }

    @Test
    void variasReservasSimultaneasNoVendenDeMas() throws Exception {
        int stock = stockOf("MC-01");
        int buyers = stock + 5;
        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ReservationResult>> results = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return reserveStock.reserve(new ReservationRequest(UUID.randomUUID(),
                        List.of(new ReservationRequest.Item("MC-01", 1))));
            }));
        }

        start.countDown();
        int reserved = 0;
        for (Future<ReservationResult> result : results) {
            if (result.get(30, TimeUnit.SECONDS).reserved()) {
                reserved++;
            }
        }
        pool.shutdown();

        assertThat(reserved).isEqualTo(stock);
        assertThat(stockOf("MC-01")).isZero();
    }

    private void sendOrderPlaced(UUID orderId, String sku, int quantity) {
        kafkaTemplate.send(Topics.ORDER_PLACED, orderId.toString(), new OrderPlacedEvent(UUID.randomUUID(), orderId,
                "viktor", List.of(new OrderPlacedEvent.Line(sku, quantity)), Instant.now()));
    }

    private void sendOrderCancelled(UUID orderId) {
        kafkaTemplate.send(Topics.ORDER_CANCELLED, orderId.toString(),
                new OrderCancelledEvent(UUID.randomUUID(), orderId, Instant.now()));
    }

    private int stockOf(String sku) {
        return products.findBySkus(List.of(sku)).getFirst().availableQuantity();
    }

    private String statusOf(UUID orderId) {
        List<String> status = jdbc.queryForList("select status from stock_reservations where order_id = ?",
                String.class, orderId);
        return status.isEmpty() ? null : status.getFirst();
    }

    /** Lee el topic desde el principio hasta encontrar el mensaje cuya clave es el id del pedido. */
    private String awaitMessage(String topic, UUID orderId) {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config)) {
            consumer.subscribe(List.of(topic));
            AtomicReference<String> found = new AtomicReference<>();
            await().pollInSameThread().atMost(Duration.ofSeconds(20)).until(() -> {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (orderId.toString().equals(record.key())) {
                        found.set(record.value());
                    }
                }
                return found.get() != null;
            });
            return found.get();
        }
    }
}
