package dev.viktorstrohush.orderflow.order.application.port.in;

import dev.viktorstrohush.orderflow.order.domain.model.Order;
import dev.viktorstrohush.orderflow.order.domain.model.OrderId;

public interface CancelOrderUseCase {

    Order cancel(OrderId id, String customerId);
}
