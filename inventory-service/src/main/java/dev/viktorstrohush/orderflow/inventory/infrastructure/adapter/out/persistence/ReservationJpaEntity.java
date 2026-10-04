package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.out.persistence;

import dev.viktorstrohush.orderflow.inventory.domain.model.Reservation;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stock_reservations")
class ReservationJpaEntity {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Reservation.Status status;

    private String reason;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    /**
     * Long y no long: con la versión a null Spring Data sabe que la fila es nueva y hace un INSERT
     * directo, de modo que la clave primaria rechaza una segunda reserva del mismo pedido. Además
     * protege la liberación: dos liberaciones simultáneas no devuelven el stock dos veces.
     */
    @Version
    private Long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "stock_reservation_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<ReservationItemEmbeddable> items = new ArrayList<>();

    protected ReservationJpaEntity() {
    }

    ReservationJpaEntity(UUID orderId, Reservation.Status status, String reason, Instant processedAt,
                         List<ReservationItemEmbeddable> items) {
        this.orderId = orderId;
        this.status = status;
        this.reason = reason;
        this.processedAt = processedAt;
        this.items.addAll(items);
    }

    void updateState(Reservation.Status status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    UUID getOrderId() { return orderId; }
    Reservation.Status getStatus() { return status; }
    String getReason() { return reason; }
    List<ReservationItemEmbeddable> getItems() { return items; }
}
