package dev.viktorstrohush.orderflow.order.application.port.out;

import dev.viktorstrohush.orderflow.order.domain.model.Order;

/** Debe llamarse dentro de la transacción que guarda el pedido: el evento se confirma con ella. */
public interface OrderEventPublisher {

    void publishOrderPlaced(Order order);

    void publishOrderCancelled(Order order);
}
