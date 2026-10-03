package dev.viktorstrohush.orderflow.order.domain.exception;

import dev.viktorstrohush.orderflow.order.domain.model.OrderId;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(OrderId id) {
        super("Pedido no encontrado: " + id);
    }
}
