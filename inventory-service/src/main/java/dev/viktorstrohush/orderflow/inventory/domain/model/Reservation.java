package dev.viktorstrohush.orderflow.inventory.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * El stock apartado para un pedido. Guarda las líneas para poder devolverlas si el pedido se
 * cancela, y sirve de registro de pedidos ya procesados (idempotencia).
 */
public final class Reservation {

    public enum Status {
        /** El stock está descontado y apartado para el pedido. */
        RESERVED,
        /** No se reservó nada: faltaba stock o el pedido se canceló antes. */
        REJECTED,
        /** Se reservó y después se devolvió por la cancelación del pedido. */
        RELEASED
    }

    private final UUID orderId;
    private final List<ReservationRequest.Item> items;
    private Status status;
    private String reason;

    private Reservation(UUID orderId, Status status, String reason, List<ReservationRequest.Item> items) {
        this.orderId = Objects.requireNonNull(orderId);
        this.status = Objects.requireNonNull(status);
        this.reason = reason;
        this.items = List.copyOf(items);
    }

    public static Reservation reserved(UUID orderId, List<ReservationRequest.Item> items) {
        return new Reservation(orderId, Status.RESERVED, null, items);
    }

    public static Reservation rejected(UUID orderId, String reason) {
        return new Reservation(orderId, Status.REJECTED, reason, List.of());
    }

    /** Reconstruye una reserva desde persistencia. */
    public static Reservation restore(UUID orderId, Status status, String reason, List<ReservationRequest.Item> items) {
        return new Reservation(orderId, status, reason, items);
    }

    public boolean holdsStock() {
        return status == Status.RESERVED;
    }

    public void release() {
        if (!holdsStock()) {
            throw new IllegalStateException("La reserva del pedido " + orderId + " no tiene stock que liberar (" + status + ")");
        }
        this.status = Status.RELEASED;
        this.reason = "Reserva liberada por la cancelación del pedido";
    }

    /** Lo que se comunica a order-service: reservado o no, y por qué. */
    public ReservationResult result() {
        return holdsStock() ? ReservationResult.reserved(orderId) : ReservationResult.rejected(orderId, reason);
    }

    public UUID orderId() { return orderId; }
    public List<ReservationRequest.Item> items() { return items; }
    public Status status() { return status; }
    public String reason() { return reason; }
}
