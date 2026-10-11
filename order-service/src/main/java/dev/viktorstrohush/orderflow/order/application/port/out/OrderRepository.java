package dev.viktorstrohush.orderflow.order.application.port.out;

import dev.viktorstrohush.orderflow.order.application.port.PageQuery;
import dev.viktorstrohush.orderflow.order.application.port.PageResult;
import dev.viktorstrohush.orderflow.order.domain.model.Order;
import dev.viktorstrohush.orderflow.order.domain.model.OrderId;

import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId id);

    /** One page of the customer's orders, newest first. */
    PageResult<Order> findByCustomerId(String customerId, PageQuery page);
}
