package dev.viktorstrohush.orderflow.inventory.application.port.out;

import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationResult;

/** Debe llamarse dentro de la transacción de la reserva: el evento se confirma con ella. */
public interface StockEventPublisher {

    void publish(ReservationResult result);
}
