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
 * Outbound adapter using the outbox pattern: stores the reservation result in the outbox table
 * within the same transaction that deducts the stock. {@link OutboxRelay} does the send to Kafka.
 */
@Component
class OutboxStockEventPublisher implements StockEventPublisher {

    private final SpringDataOutboxRepository outbox;
    private final ObjectMapper json;

    OutboxStockEventPublisher(SpringDataOutboxRepository outbox, ObjectMapper json) {
        this.outbox = outbox;
        this.json = json;
    }

    /** MANDATORY: publishing outside a transaction is a programming error and must fail. */
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
            throw new IllegalStateException("Could not serialize the result of order " + result.orderId(), e);
        }
    }
}
