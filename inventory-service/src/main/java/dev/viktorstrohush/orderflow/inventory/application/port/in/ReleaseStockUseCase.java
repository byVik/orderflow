package dev.viktorstrohush.orderflow.inventory.application.port.in;

import java.util.UUID;

public interface ReleaseStockUseCase {

    /** Devuelve el stock reservado para un pedido cancelado. Idempotente por orderId. */
    void release(UUID orderId);
}
