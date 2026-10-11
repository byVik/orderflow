package dev.viktorstrohush.orderflow.order.domain.model;

public enum OrderStatus {
    /** Created, waiting for the stock reservation. */
    PENDING,
    /** Stock reserved: order confirmed. */
    CONFIRMED,
    /** Not enough stock. */
    REJECTED,
    /** Cancelled by the customer before it was confirmed. */
    CANCELLED
}
