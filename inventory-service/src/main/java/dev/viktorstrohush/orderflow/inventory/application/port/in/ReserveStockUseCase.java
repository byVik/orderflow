package dev.viktorstrohush.orderflow.inventory.application.port.in;

import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationRequest;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationResult;

public interface ReserveStockUseCase {

    /** Reserva todo o nada. Idempotente por orderId. */
    ReservationResult reserve(ReservationRequest request);
}
