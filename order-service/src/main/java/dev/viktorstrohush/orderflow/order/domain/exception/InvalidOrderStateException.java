package dev.viktorstrohush.orderflow.order.domain.exception;

import dev.viktorstrohush.orderflow.order.domain.model.OrderStatus;

import java.util.Map;

public class InvalidOrderStateException extends OrderException {
    public InvalidOrderStateException(String message, OrderStatus status) {
        super(OrderError.ORDER_NOT_PENDING, message, Map.of("status", status.name()));
    }
}
