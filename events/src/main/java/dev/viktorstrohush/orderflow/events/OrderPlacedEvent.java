package dev.viktorstrohush.orderflow.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Published by order-service when an order is created. The message key is the orderId. */
public record OrderPlacedEvent(UUID eventId, UUID orderId, String customerId, List<Line> lines, Instant occurredAt) {

    public record Line(String sku, int quantity) {
    }
}
