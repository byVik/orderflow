package dev.viktorstrohush.orderflow.order.domain.model;

public enum OrderStatus {
    /** Creado, esperando la reserva de stock. */
    PENDING,
    /** Stock reservado: pedido confirmado. */
    CONFIRMED,
    /** Sin stock suficiente. */
    REJECTED,
    /** Cancelado por el cliente antes de confirmarse. */
    CANCELLED
}
