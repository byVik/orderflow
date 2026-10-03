package dev.viktorstrohush.orderflow.events;

import java.time.Instant;
import java.util.UUID;

/** Publicado por inventory-service con el resultado de la reserva de stock de un pedido. */
public record StockResultEvent(UUID eventId, UUID orderId, Status status, String reason, Instant occurredAt) {

    public enum Status { RESERVED, REJECTED }

    public static StockResultEvent reserved(UUID orderId) {
        return new StockResultEvent(UUID.randomUUID(), orderId, Status.RESERVED, null, Instant.now());
    }

    public static StockResultEvent rejected(UUID orderId, String reason) {
        return new StockResultEvent(UUID.randomUUID(), orderId, Status.REJECTED, reason, Instant.now());
    }
}
