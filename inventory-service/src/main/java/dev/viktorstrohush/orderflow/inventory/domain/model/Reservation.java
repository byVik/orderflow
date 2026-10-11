package dev.viktorstrohush.orderflow.inventory.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The stock set aside for an order. Keeps the lines so they can be returned if the order is
 * cancelled, and doubles as the record of orders already processed (idempotency).
 */
public final class Reservation {

    public enum Status {
        /** The stock is deducted and set aside for the order. */
        RESERVED,
        /** Nothing was reserved: stock was short or the order was cancelled first. */
        REJECTED,
        /** It was reserved and later returned because the order was cancelled. */
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

    /** Rebuilds a reservation from persistence. */
    public static Reservation restore(UUID orderId, Status status, String reason, List<ReservationRequest.Item> items) {
        return new Reservation(orderId, status, reason, items);
    }

    public boolean holdsStock() {
        return status == Status.RESERVED;
    }

    public void release() {
        if (!holdsStock()) {
            throw new IllegalStateException("The reservation for order " + orderId + " has no stock to release (" + status + ")");
        }
        this.status = Status.RELEASED;
        this.reason = RejectionReason.RELEASED_BY_CANCELLATION;
    }

    /** What is reported to order-service: reserved or not, and why. */
    public ReservationResult result() {
        return holdsStock() ? ReservationResult.reserved(orderId) : ReservationResult.rejected(orderId, reason);
    }

    public UUID orderId() { return orderId; }
    public List<ReservationRequest.Item> items() { return items; }
    public Status status() { return status; }
    public String reason() { return reason; }
}
