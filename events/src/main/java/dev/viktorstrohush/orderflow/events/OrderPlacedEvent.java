package dev.viktorstrohush.orderflow.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Publicado por order-service cuando se crea un pedido. La clave del mensaje es el orderId. */
public record OrderPlacedEvent(UUID eventId, UUID orderId, String customerId, List<Line> lines, Instant occurredAt) {

    public record Line(String sku, int quantity) {
    }
}
