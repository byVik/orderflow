package dev.viktorstrohush.orderflow.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by order-service when the customer cancels an order. inventory-service uses it
 * to return the reserved stock (saga compensation). The message key is the orderId.
 */
public record OrderCancelledEvent(UUID eventId, UUID orderId, Instant occurredAt) {
}
