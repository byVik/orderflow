package dev.viktorstrohush.orderflow.order.integration;

import dev.viktorstrohush.orderflow.events.StockResultEvent;
import dev.viktorstrohush.orderflow.events.Topics;
import dev.viktorstrohush.orderflow.order.application.port.in.CancelOrderUseCase;
import dev.viktorstrohush.orderflow.order.application.port.in.GetOrdersQuery;
import dev.viktorstrohush.orderflow.order.application.port.in.PlaceOrderUseCase;
import dev.viktorstrohush.orderflow.order.application.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import dev.viktorstrohush.orderflow.order.application.port.out.OrderEventPublisher;
import dev.viktorstrohush.orderflow.order.application.port.out.ProductCatalog;
import dev.viktorstrohush.orderflow.order.domain.model.Order;
import dev.viktorstrohush.orderflow.order.domain.model.OrderLine;
import dev.viktorstrohush.orderflow.order.domain.model.OrderStatus;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

/**
 * Test de integración con PostgreSQL y Kafka reales (Testcontainers): crea un pedido, comprueba
 * que sus eventos salen por el outbox y que pasa a CONFIRMED / REJECTED al recibir el resultado
 * de inventory-service.
 */
@SpringBootTest(properties = "orderflow.auth.jwt-secret=integration-test-secret-0123456789abcdef")
@Import(ContainersConfig.class)
class OrderFlowIntegrationTest {

    @Autowired
    KafkaContainer kafka;

    @MockitoBean
    ProductCatalog catalog;

    @Autowired
    PlaceOrderUseCase placeOrder;
    @Autowired
    GetOrdersQuery getOrders;
    @Autowired
    CancelOrderUseCase cancelOrder;
    @Autowired
    OrderEventPublisher eventPublisher;
    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    TransactionTemplate tx;

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

    @Test
    void alCrearUnPedidoElEventoLlegaAKafkaPorElOutbox() {
        Order order = placeSampleOrder();

        String payload = awaitMessage(Topics.ORDER_PLACED, order);

        // JSON tal cual, no un texto JSON escapado dentro de otro.
        assertThat(payload).startsWith("{")
                .contains("\"orderId\":\"" + order.id() + "\"")
                .contains("\"sku\":\"KB-01\"");
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(pendingOutboxRows(order)).isZero());
    }

    @Test
    void alCancelarUnPedidoSePublicaLaCancelacion() {
        Order order = placeSampleOrder();

        cancelOrder.cancel(order.id(), "viktor");

        assertThat(awaitMessage(Topics.ORDER_CANCELLED, order)).contains("\"orderId\":\"" + order.id() + "\"");
    }

    @Test
    void siLaTransaccionHaceRollbackNoQuedaEventoEnElOutbox() {
        Order order = Order.place("viktor", List.of(new OrderLine("KB-01", 1, BigDecimal.TEN)));

        tx.executeWithoutResult(status -> {
            eventPublisher.publishOrderPlaced(order);
            status.setRollbackOnly();
        });

        assertThat(jdbc.queryForObject("select count(*) from outbox where message_key = ?", Integer.class,
                order.id().toString())).isZero();
    }

    @Test
    void publicarFueraDeUnaTransaccionFalla() {
        Order order = Order.place("viktor", List.of(new OrderLine("KB-01", 1, BigDecimal.TEN)));

        assertThatThrownBy(() -> eventPublisher.publishOrderPlaced(order))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private Order placeSampleOrder() {
        when(catalog.pricesFor(anySet())).thenReturn(Map.of("KB-01", new BigDecimal("49.90")));
        return placeOrder.place(new PlaceOrderCommand("viktor", List.of(new PlaceOrderCommand.Line("KB-01", 1))));
    }

    private Integer pendingOutboxRows(Order order) {
        return jdbc.queryForObject("select count(*) from outbox where message_key = ? and published_at is null",
                Integer.class, order.id().toString());
    }

    /** Lee el topic desde el principio hasta encontrar el mensaje cuya clave es el id del pedido. */
    private String awaitMessage(String topic, Order order) {
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
                    if (order.id().toString().equals(record.key())) {
                        found.set(record.value());
                    }
                }
                return found.get() != null;
            });
            return found.get();
        }
    }
}
