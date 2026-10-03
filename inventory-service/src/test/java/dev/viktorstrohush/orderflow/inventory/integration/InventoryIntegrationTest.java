package dev.viktorstrohush.orderflow.inventory.integration;

import dev.viktorstrohush.orderflow.events.OrderPlacedEvent;
import dev.viktorstrohush.orderflow.events.Topics;
import dev.viktorstrohush.orderflow.inventory.application.port.in.ListProductsQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class InventoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0");

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    ListProductsQuery products;

    @Test
    void unPedidoRecibidoPorKafkaDescuentaElStockUnaSolaVez() {
        int before = stockOf("WC-01");
        UUID orderId = UUID.randomUUID();
        OrderPlacedEvent event = new OrderPlacedEvent(UUID.randomUUID(), orderId, "viktor",
                List.of(new OrderPlacedEvent.Line("WC-01", 2)), Instant.now());

        kafkaTemplate.send(Topics.ORDER_PLACED, orderId.toString(), event);
        kafkaTemplate.send(Topics.ORDER_PLACED, orderId.toString(), event); // duplicado

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(stockOf("WC-01")).isEqualTo(before - 2));
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(stockOf("WC-01")).isEqualTo(before - 2));
    }

    private int stockOf(String sku) {
        return products.findBySkus(List.of(sku)).getFirst().availableQuantity();
    }
}
