package dev.viktorstrohush.orderflow.order.integration;

import dev.viktorstrohush.orderflow.events.StockResultEvent;
import dev.viktorstrohush.orderflow.events.Topics;
import dev.viktorstrohush.orderflow.order.application.port.in.GetOrdersQuery;
import dev.viktorstrohush.orderflow.order.application.port.in.PlaceOrderUseCase;
import dev.viktorstrohush.orderflow.order.application.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import dev.viktorstrohush.orderflow.order.application.port.out.ProductCatalog;
import dev.viktorstrohush.orderflow.order.domain.model.Order;
import dev.viktorstrohush.orderflow.order.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

/**
 * Test de integración con PostgreSQL y Kafka reales (Testcontainers): crea un pedido y
 * comprueba que pasa a CONFIRMED / REJECTED al recibir el evento de inventory-service.
 */
@SpringBootTest
@Testcontainers
class OrderFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0");

    @MockitoBean
    ProductCatalog catalog;

    @Autowired
    PlaceOrderUseCase placeOrder;
    @Autowired
    GetOrdersQuery getOrders;
    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void elPedidoSeConfirmaCuandoInventoryReservaElStock() {
        Order order = placeSampleOrder();

        kafkaTemplate.send(Topics.STOCK_RESULT, order.id().toString(), StockResultEvent.reserved(order.id().value()));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(getOrders.getById(order.id(), "viktor").status()).isEqualTo(OrderStatus.CONFIRMED));
    }

    @Test
    void elPedidoSeRechazaCuandoNoHayStock() {
        Order order = placeSampleOrder();

        kafkaTemplate.send(Topics.STOCK_RESULT, order.id().toString(),
                StockResultEvent.rejected(order.id().value(), "Sin stock de KB-01"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Order updated = getOrders.getById(order.id(), "viktor");
            assertThat(updated.status()).isEqualTo(OrderStatus.REJECTED);
            assertThat(updated.rejectionReason()).isEqualTo("Sin stock de KB-01");
        });
    }

    private Order placeSampleOrder() {
        when(catalog.pricesFor(anySet())).thenReturn(Map.of("KB-01", new BigDecimal("49.90")));
        return placeOrder.place(new PlaceOrderCommand("viktor", List.of(new PlaceOrderCommand.Line("KB-01", 1))));
    }
}
