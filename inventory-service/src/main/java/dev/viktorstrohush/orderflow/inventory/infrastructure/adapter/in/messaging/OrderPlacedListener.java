package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.in.messaging;

import dev.viktorstrohush.orderflow.events.OrderPlacedEvent;
import dev.viktorstrohush.orderflow.events.Topics;
import dev.viktorstrohush.orderflow.inventory.application.port.in.ReserveStockUseCase;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class OrderPlacedListener {

    private final ReserveStockUseCase reserveStock;

    OrderPlacedListener(ReserveStockUseCase reserveStock) {
        this.reserveStock = reserveStock;
    }

    @KafkaListener(topics = Topics.ORDER_PLACED, groupId = "inventory-service")
    void on(OrderPlacedEvent event) {
        reserveStock.reserve(new ReservationRequest(
                event.orderId(),
                event.lines().stream().map(l -> new ReservationRequest.Item(l.sku(), l.quantity())).toList()));
    }
}
