package dev.viktorstrohush.orderflow.inventory.application.port.out;

import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationResult;

/** Must be called inside the reservation's transaction: the event commits with it. */
public interface StockEventPublisher {

    void publish(ReservationResult result);
}
