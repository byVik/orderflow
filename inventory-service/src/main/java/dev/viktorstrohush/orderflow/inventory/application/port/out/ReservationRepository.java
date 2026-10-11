package dev.viktorstrohush.orderflow.inventory.application.port.out;

import dev.viktorstrohush.orderflow.inventory.domain.model.Reservation;

import java.util.Optional;
import java.util.UUID;

/** Reservations by order. Also the record of orders already processed, for idempotency. */
public interface ReservationRepository {

    Optional<Reservation> find(UUID orderId);

    void save(Reservation reservation);
}
