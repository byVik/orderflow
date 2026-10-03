package dev.viktorstrohush.orderflow.inventory.application.port.out;

import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationResult;

public interface StockEventPublisher {

    void publish(ReservationResult result);
}
