package dev.viktorstrohush.orderflow.order.application.port.in;

import dev.viktorstrohush.orderflow.order.domain.model.Order;
import dev.viktorstrohush.orderflow.order.domain.model.OrderId;

import java.util.List;

public interface GetOrdersQuery {

    /** Devuelve el pedido solo si pertenece al cliente indicado. */
    Order getById(OrderId id, String customerId);

    List<Order> listByCustomer(String customerId);
}
