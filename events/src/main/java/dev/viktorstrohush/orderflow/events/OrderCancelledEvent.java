package dev.viktorstrohush.orderflow.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Publicado por order-service cuando el cliente cancela un pedido. inventory-service lo usa
 * para devolver el stock reservado (compensación de la saga). La clave del mensaje es el orderId.
 */
public record OrderCancelledEvent(UUID eventId, UUID orderId, Instant occurredAt) {
}
