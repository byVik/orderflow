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
 * Adaptador de salida con el patrón outbox: no llama a Kafka, guarda el evento en la tabla
 * outbox dentro de la transacción del pedido. Así pedido y evento se confirman juntos o no se
 * confirma ninguno. El envío real lo hace {@link OutboxRelay}.
 */
@Component
class OutboxOrderEventPublisher implements OrderEventPublisher {

    private final SpringDataOutboxRepository outbox;
    private final ObjectMapper json;

    OutboxOrderEventPublisher(SpringDataOutboxRepository outbox, ObjectMapper json) {
        this.outbox = outbox;
        this.json = json;
    }

    /** MANDATORY: publicar fuera de una transacción es un error de programación y debe fallar. */
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
            // La clave es el orderId: todos los eventos de un pedido van a la misma partición.
            outbox.save(new OutboxJpaEntity(topic, order.id().toString(), json.writeValueAsString(event), Instant.now()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento del pedido " + order.id(), e);
        }
    }
}
