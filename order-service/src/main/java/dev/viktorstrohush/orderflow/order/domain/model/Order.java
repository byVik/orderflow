package dev.viktorstrohush.orderflow.order.domain.model;

import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderException;
import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderStateException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Agregado raíz del pedido. Contiene las reglas de negocio y las transiciones de estado;
 * no depende de Spring ni de JPA.
 */
public final class Order {

    private final OrderId id;
    private final String customerId;
    private final List<OrderLine> lines;
    private final Instant createdAt;
    private OrderStatus status;
    private String rejectionReason;

    private Order(OrderId id, String customerId, List<OrderLine> lines, OrderStatus status,
                  String rejectionReason, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.customerId = customerId;
        this.lines = List.copyOf(lines);
        this.status = Objects.requireNonNull(status);
        this.rejectionReason = rejectionReason;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /** Crea un pedido nuevo en estado PENDING. */
    public static Order place(String customerId, List<OrderLine> lines) {
        if (customerId == null || customerId.isBlank()) {
            throw new InvalidOrderException("El cliente es obligatorio");
        }
        if (lines == null || lines.isEmpty()) {
            throw new InvalidOrderException("El pedido debe tener al menos una línea");
        }
        long distinctSkus = lines.stream().map(OrderLine::sku).distinct().count();
        if (distinctSkus != lines.size()) {
            throw new InvalidOrderException("No puede haber SKUs repetidos en el pedido");
        }
        return new Order(OrderId.newId(), customerId, lines, OrderStatus.PENDING, null, Instant.now());
    }

    /** Reconstruye un pedido desde persistencia. */
    public static Order restore(OrderId id, String customerId, List<OrderLine> lines, OrderStatus status,
                                String rejectionReason, Instant createdAt) {
        return new Order(id, customerId, lines, status, rejectionReason, createdAt);
    }

    public void confirm() {
        requireStatus(OrderStatus.PENDING, "confirmar");
        this.status = OrderStatus.CONFIRMED;
    }

    public void reject(String reason) {
        requireStatus(OrderStatus.PENDING, "rechazar");
        this.status = OrderStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public void cancel() {
        requireStatus(OrderStatus.PENDING, "cancelar");
        this.status = OrderStatus.CANCELLED;
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public BigDecimal total() {
        return lines.stream().map(OrderLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void requireStatus(OrderStatus expected, String action) {
        if (status != expected) {
            throw new InvalidOrderStateException(
                    "No se puede " + action + " el pedido " + id + " en estado " + status);
        }
    }

    public OrderId id() { return id; }
    public String customerId() { return customerId; }
    public List<OrderLine> lines() { return lines; }
    public OrderStatus status() { return status; }
    public String rejectionReason() { return rejectionReason; }
    public Instant createdAt() { return createdAt; }
}
