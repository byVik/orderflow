package dev.viktorstrohush.orderflow.order.application.port.in;

import dev.viktorstrohush.orderflow.order.application.port.PageQuery;
import dev.viktorstrohush.orderflow.order.application.port.PageResult;
import dev.viktorstrohush.orderflow.order.domain.model.Order;
import dev.viktorstrohush.orderflow.order.domain.model.OrderId;

public interface GetOrdersQuery {

    /** Returns the order only if it belongs to the given customer. */
    Order getById(OrderId id, String customerId);

    /** One page of the customer's orders, newest first. */
    PageResult<Order> listByCustomer(String customerId, PageQuery page);
}
