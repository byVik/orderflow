package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_reservations")
class ReservationJpaEntity {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Column(nullable = false)
    private boolean reserved;

    private String reason;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ReservationJpaEntity() {
    }

    ReservationJpaEntity(UUID orderId, boolean reserved, String reason, Instant processedAt) {
        this.orderId = orderId;
        this.reserved = reserved;
        this.reason = reason;
        this.processedAt = processedAt;
    }

    UUID getOrderId() { return orderId; }
    boolean isReserved() { return reserved; }
    String getReason() { return reason; }
}
