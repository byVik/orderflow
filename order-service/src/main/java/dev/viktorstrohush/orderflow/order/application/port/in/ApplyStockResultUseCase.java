package dev.viktorstrohush.orderflow.order.application.port.in;

import dev.viktorstrohush.orderflow.order.domain.model.OrderId;

public interface ApplyStockResultUseCase {

    /**
     * Aplica el resultado de la reserva de stock. Es idempotente: si el pedido ya no está
     * en PENDING (mensaje duplicado o pedido cancelado) se ignora.
     */
    void apply(OrderId id, boolean reserved, String reason);
}
