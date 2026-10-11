package dev.viktorstrohush.orderflow.inventory.application.port.in;

import java.util.UUID;

public interface ReleaseStockUseCase {

    /** Returns the stock reserved for a cancelled order. Idempotent by orderId. */
    void release(UUID orderId);
}
