package dev.viktorstrohush.orderflow.inventory.application.port.out;

import dev.viktorstrohush.orderflow.inventory.domain.model.Reservation;

import java.util.Optional;
import java.util.UUID;

/** Reservas por pedido. Es también el registro de pedidos ya procesados, para la idempotencia. */
public interface ReservationRepository {

    Optional<Reservation> find(UUID orderId);

    void save(Reservation reservation);
}
