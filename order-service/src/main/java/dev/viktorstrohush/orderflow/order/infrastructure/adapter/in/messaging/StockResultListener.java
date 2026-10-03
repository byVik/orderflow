package dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.messaging;

import dev.viktorstrohush.orderflow.events.StockResultEvent;
import dev.viktorstrohush.orderflow.events.Topics;
import dev.viktorstrohush.orderflow.order.application.port.in.ApplyStockResultUseCase;
import dev.viktorstrohush.orderflow.order.domain.model.OrderId;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Adaptador de entrada: escucha el resultado de la reserva de stock. */
@Component
class StockResultListener {

    private final ApplyStockResultUseCase applyStockResult;

    StockResultListener(ApplyStockResultUseCase applyStockResult) {
        this.applyStockResult = applyStockResult;
    }

    @KafkaListener(topics = Topics.STOCK_RESULT, groupId = "order-service")
    void on(StockResultEvent event) {
        applyStockResult.apply(
                new OrderId(event.orderId()),
                event.status() == StockResultEvent.Status.RESERVED,
                event.reason());
    }
}
