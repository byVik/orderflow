package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.in.messaging;

import dev.viktorstrohush.orderflow.events.OrderCancelledEvent;
import dev.viktorstrohush.orderflow.events.Topics;
import dev.viktorstrohush.orderflow.inventory.application.port.in.ReleaseStockUseCase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Adaptador de entrada: compensación de la saga. Devuelve el stock de un pedido cancelado. */
@Component
class OrderCancelledListener {

    private final ReleaseStockUseCase releaseStock;

    OrderCancelledListener(ReleaseStockUseCase releaseStock) {
        this.releaseStock = releaseStock;
    }

    @KafkaListener(topics = Topics.ORDER_CANCELLED, groupId = "inventory-service",
            properties = "spring.json.value.default.type=dev.viktorstrohush.orderflow.events.OrderCancelledEvent")
    void on(OrderCancelledEvent event) {
        releaseStock.release(event.orderId());
    }
}
