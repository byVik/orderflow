package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.messaging;

import dev.viktorstrohush.orderflow.events.OrderPlacedEvent;
import dev.viktorstrohush.orderflow.events.Topics;
import dev.viktorstrohush.orderflow.order.application.port.out.OrderEventPublisher;
import dev.viktorstrohush.orderflow.order.domain.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

/**
 * Publica OrderPlaced en Kafka. Si hay una transacción activa, el envío se hace tras el commit
 * para no anunciar pedidos que finalmente no se han guardado.
 */
@Component
class KafkaOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafka;

    KafkaOrderEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void publishOrderPlaced(Order order) {
        OrderPlacedEvent event = new OrderPlacedEvent(
                UUID.randomUUID(),
                order.id().value(),
                order.customerId(),
                order.lines().stream().map(l -> new OrderPlacedEvent.Line(l.sku(), l.quantity())).toList(),
                Instant.now());

        Runnable send = () -> kafka.send(Topics.ORDER_PLACED, event.orderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error publicando OrderPlaced para el pedido {}", event.orderId(), ex);
                    }
                });

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}
