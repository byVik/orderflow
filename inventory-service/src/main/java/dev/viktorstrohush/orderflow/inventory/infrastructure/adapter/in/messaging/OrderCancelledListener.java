package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.in.messaging;

import dev.viktorstrohush.orderflow.events.OrderCancelledEvent;
import dev.viktorstrohush.orderflow.events.Topics;
import dev.viktorstrohush.orderflow.inventory.application.port.in.ReleaseStockUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Inbound adapter: saga compensation. Returns the stock of a cancelled order. */
@Component
@RequiredArgsConstructor
class OrderCancelledListener {

    private final ReleaseStockUseCase releaseStock;

    @KafkaListener(topics = Topics.ORDER_CANCELLED, groupId = "inventory-service",
            properties = "spring.json.value.default.type=dev.viktorstrohush.orderflow.events.OrderCancelledEvent")
    void on(OrderCancelledEvent event) {
        releaseStock.release(event.orderId());
    }
}
