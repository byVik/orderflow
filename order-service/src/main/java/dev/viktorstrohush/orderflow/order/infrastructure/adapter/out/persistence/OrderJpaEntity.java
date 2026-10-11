package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.persistence;

import dev.viktorstrohush.orderflow.order.domain.model.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lombok writes the getters and the empty constructor that JPA requires. No @Data and no @Setter:
 * the state only changes through {@link #addLine} and {@link #updateState}.
 */
@Entity
@Table(name = "orders")
@Getter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class OrderJpaEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    /** With @BatchSize the lines of all the orders of a page come in one query, not one per order. */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    @BatchSize(size = 50)
    private List<OrderLineJpaEntity> lines = new ArrayList<>();

    OrderJpaEntity(UUID id, String customerId, OrderStatus status, String rejectionReason, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
    }

    void addLine(OrderLineJpaEntity line) {
        line.setOrder(this);
        lines.add(line);
    }

    void updateState(OrderStatus status, String rejectionReason) {
        this.status = status;
        this.rejectionReason = rejectionReason;
    }
}
