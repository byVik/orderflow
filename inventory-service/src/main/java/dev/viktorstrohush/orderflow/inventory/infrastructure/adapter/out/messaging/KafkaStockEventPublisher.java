package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.out.messaging;

import dev.viktorstrohush.orderflow.events.StockResultEvent;
import dev.viktorstrohush.orderflow.events.Topics;
import dev.viktorstrohush.orderflow.inventory.application.port.out.StockEventPublisher;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationResult;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Publica el resultado de la reserva una vez confirmada la transacción. */
@Component
class KafkaStockEventPublisher implements StockEventPublisher {

    private final KafkaTemplate<String, Object> kafka;

    KafkaStockEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void publish(ReservationResult result) {
        StockResultEvent event = result.reserved()
                ? StockResultEvent.reserved(result.orderId())
                : StockResultEvent.rejected(result.orderId(), result.reason());
        Runnable send = () -> kafka.send(Topics.STOCK_RESULT, result.orderId().toString(), event);

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
