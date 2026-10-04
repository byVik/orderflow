package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.viktorstrohush.orderflow.events.StockResultEvent;
import dev.viktorstrohush.orderflow.events.Topics;
import dev.viktorstrohush.orderflow.inventory.application.port.out.StockEventPublisher;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Adaptador de salida con el patrón outbox: guarda el resultado de la reserva en la tabla outbox
 * dentro de la misma transacción que descuenta el stock. El envío a Kafka lo hace {@link OutboxRelay}.
 */
@Component
class OutboxStockEventPublisher implements StockEventPublisher {

    private final SpringDataOutboxRepository outbox;
    private final ObjectMapper json;

    OutboxStockEventPublisher(SpringDataOutboxRepository outbox, ObjectMapper json) {
        this.outbox = outbox;
        this.json = json;
    }

    /** MANDATORY: publicar fuera de una transacción es un error de programación y debe fallar. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(ReservationResult result) {
        StockResultEvent event = result.reserved()
                ? StockResultEvent.reserved(result.orderId())
                : StockResultEvent.rejected(result.orderId(), result.reason());
        try {
            outbox.save(new OutboxJpaEntity(Topics.STOCK_RESULT, result.orderId().toString(),
                    json.writeValueAsString(event), Instant.now()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el resultado del pedido " + result.orderId(), e);
        }
    }
}
