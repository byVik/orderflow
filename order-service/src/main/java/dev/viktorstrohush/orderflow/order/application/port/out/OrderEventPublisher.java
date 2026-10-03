package dev.viktorstrohush.orderflow.order.application.port.out;

import dev.viktorstrohush.orderflow.order.domain.model.Order;

public interface OrderEventPublisher {

    void publishOrderPlaced(Order order);
}
