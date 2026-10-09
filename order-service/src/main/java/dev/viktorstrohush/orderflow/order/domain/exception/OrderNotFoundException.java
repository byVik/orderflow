package dev.viktorstrohush.orderflow.order.domain.exception;

import dev.viktorstrohush.orderflow.order.domain.model.OrderId;

import java.util.Map;

public class OrderNotFoundException extends OrderException {
    public OrderNotFoundException(OrderId id) {
        super(OrderError.ORDER_NOT_FOUND, "Order not found: " + id, Map.of());
    }
}
