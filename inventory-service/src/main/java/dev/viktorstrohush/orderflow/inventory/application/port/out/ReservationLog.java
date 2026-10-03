package dev.viktorstrohush.orderflow.inventory.application.port.out;

import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationResult;

import java.util.Optional;
import java.util.UUID;

/** Registro de pedidos ya procesados, para que la reserva sea idempotente. */
public interface ReservationLog {

    Optional<ReservationResult> find(UUID orderId);

    void record(ReservationResult result);
}
