package dev.viktorstrohush.orderflow.order.integration;

import dev.viktorstrohush.orderflow.events.StockResultEvent;
import dev.viktorstrohush.orderflow.events.Topics;
import dev.viktorstrohush.orderflow.order.application.port.PageQuery;
import dev.viktorstrohush.orderflow.order.application.port.PageResult;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

/**
 * Integration test with real PostgreSQL and Kafka (Testcontainers): it creates an order, checks
 * that its events go out through the outbox and that it moves to CONFIRMED / REJECTED on
 * receiving the result from inventory-service.
 */
@SpringBootTest(properties = {
        "orderflow.auth.jwt-secret=integration-test-secret-0123456789abcdef",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector="
                + "dev.viktorstrohush.orderflow.order.integration.SqlRecorder"})
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
    void theOrderIsConfirmedWhenInventoryReservesTheStock() {
        Order order = placeSampleOrder();

        kafkaTemplate.send(Topics.STOCK_RESULT, order.id().toString(), StockResultEvent.reserved(order.id().value()));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(getOrders.getById(order.id(), "viktor").status()).isEqualTo(OrderStatus.CONFIRMED));
    }

    @Test
    void theOrderIsRejectedWhenThereIsNoStock() {
        Order order = placeSampleOrder();

        kafkaTemplate.send(Topics.STOCK_RESULT, order.id().toString(),
                StockResultEvent.rejected(order.id().value(), "No stock of KB-01"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Order updated = getOrders.getById(order.id(), "viktor");
            assertThat(updated.status()).isEqualTo(OrderStatus.REJECTED);
            assertThat(updated.rejectionReason()).isEqualTo("No stock of KB-01");
        });
    }

    @Test
    void whenAnOrderIsCreatedTheEventReachesKafkaThroughTheOutbox() {
        Order order = placeSampleOrder();

        String payload = awaitMessage(Topics.ORDER_PLACED, order);

        // JSON as is, not an escaped JSON string inside another.
        assertThat(payload).startsWith("{")
                .contains("\"orderId\":\"" + order.id() + "\"")
                .contains("\"sku\":\"KB-01\"");
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(pendingOutboxRows(order)).isZero());
    }

    @Test
    void whenAnOrderIsCancelledTheCancellationIsPublished() {
        Order order = placeSampleOrder();

        cancelOrder.cancel(order.id(), "viktor");

        assertThat(awaitMessage(Topics.ORDER_CANCELLED, order)).contains("\"orderId\":\"" + order.id() + "\"");
    }

    @Test
    void ifTheTransactionRollsBackNoEventIsLeftInTheOutbox() {
        Order order = Order.place("viktor", List.of(new OrderLine("KB-01", 1, BigDecimal.TEN)));

        tx.executeWithoutResult(status -> {
            eventPublisher.publishOrderPlaced(order);
            status.setRollbackOnly();
        });

        assertThat(jdbc.queryForObject("select count(*) from outbox where message_key = ?", Integer.class,
                order.id().toString())).isZero();
    }

    @Test
    void publishingOutsideATransactionFails() {
        Order order = Order.place("viktor", List.of(new OrderLine("KB-01", 1, BigDecimal.TEN)));

        assertThatThrownBy(() -> eventPublisher.publishOrderPlaced(order))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    /** Spec 008: twelve orders, pages of five. */
    @Test
    void theOrdersAreListedInPagesNewestFirstAndOnlyThoseOfTheCustomer() {
        String customer = "pages-" + UUID.randomUUID();
        for (int i = 0; i < 12; i++) {
            placeSampleOrder(customer);
        }
        placeSampleOrder("someone-else");

        PageResult<Order> first = getOrders.listByCustomer(customer, new PageQuery(0, 5));
        PageResult<Order> second = getOrders.listByCustomer(customer, new PageQuery(1, 5));
        PageResult<Order> last = getOrders.listByCustomer(customer, new PageQuery(2, 5));

        assertThat(first.content()).hasSize(5);
        assertThat(first.hasNext()).isTrue();
        assertThat(second.content()).hasSize(5);
        assertThat(second.hasNext()).isTrue();
        assertThat(last.content()).hasSize(2);
        assertThat(last.hasNext()).isFalse();

        List<Order> all = Stream.of(first, second, last).flatMap(page -> page.content().stream()).toList();
        assertThat(all).extracting(Order::id).doesNotHaveDuplicates();
        assertThat(all).extracting(Order::customerId).containsOnly(customer);
        assertThat(all).extracting(Order::createdAt).isSortedAccordingTo(Comparator.reverseOrder());
    }

    /**
     * The guard against N+1: one query for the orders and one for the lines of all of them. If the
     * lines were loaded order by order, a page of ten would cost eleven queries.
     */
    @Test
    void aPageCostsTwoQueriesWhateverItsSize() {
        String customer = "queries-" + UUID.randomUUID();
        for (int i = 0; i < 10; i++) {
            placeSampleOrder(customer);
        }

        List<String> sql = SqlRecorder.record(() ->
                assertThat(getOrders.listByCustomer(customer, new PageQuery(0, 10)).content())
                        .hasSize(10)
                        .allSatisfy(order -> assertThat(order.lines()).hasSize(1)));

        assertThat(sql).hasSize(2);
        assertThat(sql.get(0)).contains("from orders");
        assertThat(sql.get(1)).contains("from order_lines");
    }

    private Order placeSampleOrder() {
        return placeSampleOrder("viktor");
    }

    private Order placeSampleOrder(String customerId) {
        when(catalog.pricesFor(anySet())).thenReturn(Map.of("KB-01", new BigDecimal("49.90")));
        return placeOrder.place(new PlaceOrderCommand(customerId, List.of(new PlaceOrderCommand.Line("KB-01", 1))));
    }

    private Integer pendingOutboxRows(Order order) {
        return jdbc.queryForObject("select count(*) from outbox where message_key = ? and published_at is null",
                Integer.class, order.id().toString());
    }

    /** Reads the topic from the beginning until it finds the message whose key is the order id. */
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
