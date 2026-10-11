package dev.viktorstrohush.orderflow.events;

import java.time.Instant;
import java.util.UUID;

/** Published by inventory-service with the result of an order's stock reservation. */
public record StockResultEvent(UUID eventId, UUID orderId, Status status, String reason, Instant occurredAt) {

    public enum Status { RESERVED, REJECTED }

    public static StockResultEvent reserved(UUID orderId) {
        return new StockResultEvent(UUID.randomUUID(), orderId, Status.RESERVED, null, Instant.now());
    }

    public static StockResultEvent rejected(UUID orderId, String reason) {
        return new StockResultEvent(UUID.randomUUID(), orderId, Status.REJECTED, reason, Instant.now());
    }
}
