package dev.viktorstrohush.orderflow.order.application.port.out;

import dev.viktorstrohush.orderflow.order.domain.model.Order;

/** Must be called inside the transaction that saves the order: the event commits with it. */
public interface OrderEventPublisher {

    void publishOrderPlaced(Order order);

    void publishOrderCancelled(Order order);
}
