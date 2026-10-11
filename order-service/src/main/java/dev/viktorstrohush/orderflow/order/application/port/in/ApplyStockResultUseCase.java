package dev.viktorstrohush.orderflow.order.application.port.in;

import dev.viktorstrohush.orderflow.order.domain.model.OrderId;

public interface ApplyStockResultUseCase {

    /**
     * Applies the result of the stock reservation. It is idempotent: if the order is no longer
     * PENDING (duplicate message or cancelled order) it is ignored.
     */
    void apply(OrderId id, boolean reserved, String reason);
}
