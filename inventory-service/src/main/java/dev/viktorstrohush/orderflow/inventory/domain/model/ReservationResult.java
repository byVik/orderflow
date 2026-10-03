package dev.viktorstrohush.orderflow.inventory.domain.model;

import java.util.UUID;

public record ReservationResult(UUID orderId, boolean reserved, String reason) {

    public static ReservationResult reserved(UUID orderId) {
        return new ReservationResult(orderId, true, null);
    }

    public static ReservationResult rejected(UUID orderId, String reason) {
        return new ReservationResult(orderId, false, reason);
    }
}
