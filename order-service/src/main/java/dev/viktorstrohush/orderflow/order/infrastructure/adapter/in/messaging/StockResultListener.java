package dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.messaging;

import dev.viktorstrohush.orderflow.events.StockResultEvent;
import dev.viktorstrohush.orderflow.events.Topics;
import dev.viktorstrohush.orderflow.order.application.port.in.ApplyStockResultUseCase;
import dev.viktorstrohush.orderflow.order.domain.model.OrderId;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Inbound adapter: listens for the result of the stock reservation. */
@Component
@RequiredArgsConstructor
class StockResultListener {

    private final ApplyStockResultUseCase applyStockResult;

    @KafkaListener(topics = Topics.STOCK_RESULT, groupId = "order-service")
    void on(StockResultEvent event) {
        applyStockResult.apply(
                new OrderId(event.orderId()),
                event.status() == StockResultEvent.Status.RESERVED,
                event.reason());
    }
}
