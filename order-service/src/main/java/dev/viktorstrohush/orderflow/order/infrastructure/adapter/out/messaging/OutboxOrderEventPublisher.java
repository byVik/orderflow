package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.viktorstrohush.orderflow.events.OrderCancelledEvent;
import dev.viktorstrohush.orderflow.events.OrderPlacedEvent;
import dev.viktorstrohush.orderflow.events.Topics;
import dev.viktorstrohush.orderflow.order.application.port.out.OrderEventPublisher;
import dev.viktorstrohush.orderflow.order.domain.model.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbound adapter using the outbox pattern: it does not call Kafka, it stores the event in the
 * outbox table within the order's transaction. That way order and event commit together or
 * neither commits. {@link OutboxRelay} does the actual send.
 */
@Component
class OutboxOrderEventPublisher implements OrderEventPublisher {

    private final SpringDataOutboxRepository outbox;
    private final ObjectMapper json;

    OutboxOrderEventPublisher(SpringDataOutboxRepository outbox, ObjectMapper json) {
        this.outbox = outbox;
        this.json = json;
    }

    /** MANDATORY: publishing outside a transaction is a programming error and must fail. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishOrderPlaced(Order order) {
        store(Topics.ORDER_PLACED, order, new OrderPlacedEvent(
                UUID.randomUUID(),
                order.id().value(),
                order.customerId(),
                order.lines().stream().map(l -> new OrderPlacedEvent.Line(l.sku(), l.quantity())).toList(),
                Instant.now()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishOrderCancelled(Order order) {
        store(Topics.ORDER_CANCELLED, order,
                new OrderCancelledEvent(UUID.randomUUID(), order.id().value(), Instant.now()));
    }

    private void store(String topic, Order order, Object event) {
        try {
            // The key is the orderId: all the events of an order go to the same partition.
            outbox.save(new OutboxJpaEntity(topic, order.id().toString(), json.writeValueAsString(event), Instant.now()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize the event of order " + order.id(), e);
        }
    }
}
