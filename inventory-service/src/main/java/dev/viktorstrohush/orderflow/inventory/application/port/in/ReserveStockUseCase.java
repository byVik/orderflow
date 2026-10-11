package dev.viktorstrohush.orderflow.inventory.application.port.in;

import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationRequest;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationResult;

public interface ReserveStockUseCase {

    /** Reserves all or nothing. Idempotent by orderId. */
    ReservationResult reserve(ReservationRequest request);
}
